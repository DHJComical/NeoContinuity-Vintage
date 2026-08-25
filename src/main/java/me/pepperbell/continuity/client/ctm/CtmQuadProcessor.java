package me.pepperbell.continuity.client.ctm;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import javax.annotation.Nullable;

import me.pepperbell.continuity.api.client.QuadProcessor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

/**
 * Processes quads for textures that have a CTM Mod format definition. This is the CTM-format
 * counterpart of {@code SimpleQuadProcessor}: it computes the connection state for a quad's face
 * and emits the appropriately sub-regioned quads.
 *
 * <p>Each CTM type is handled here (delegating per-type decisions to small helper methods), and
 * the output quads are produced by {@link QuadClipper} plus {@code transformUVs} remapping.</p>
 */
public class CtmQuadProcessor implements QuadProcessor {
	protected final CtmDefinition properties;
	protected final TextureAtlasSprite[] sprites;
	protected final CtmConnectionPredicate connectionPredicate;
	protected final CtmConnectionMap connectionMap;
	protected final CtmType type;
	@Nullable
	protected final CtmCustomLogic logic;

	public CtmQuadProcessor(@Nullable CtmDefinition properties, TextureAtlasSprite[] sprites) {
		this.properties = properties;
		this.sprites = sprites;
		if (properties == null) {
			// Test / fallback construction: never connects
			this.type = CtmType.NORMAL;
			this.logic = null;
			this.connectionPredicate = new CtmConnectionPredicate(false, false, true) {
				@Override
				public boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, IBlockState otherAppearanceState, IBlockState otherState, EnumFacing face, TextureAtlasSprite quadSprite) {
					return false;
				}
			};
			this.connectionMap = new CtmConnectionMap(connectionPredicate);
			return;
		}
		boolean disableObscured = properties.getType() == CtmType.SCTM;
		this.connectionPredicate = CtmConnectionPredicate.fromProperties(properties, disableObscured);
		this.connectionMap = new CtmConnectionMap(connectionPredicate);
		this.type = properties.getType();
		this.logic = properties.getLogic();
	}

	@Override
	public ProcessingResult processQuad(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, int pass, ProcessingContext context) {
		List<BakedQuad> out = context.getExtraQuads();
		out.clear();
		transformQuad(quad, sprite, level, pos, appearanceState, state, rand, out);
		if (out.isEmpty()) {
			return ProcessingResult.DISCARD;
		}
		return ProcessingResult.NEXT_PASS;
	}

	protected void transformQuad(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, List<BakedQuad> out) {
		EnumFacing face = quad.getFace();
		switch (type) {
			case NORMAL -> handleNormal(quad, sprite, out);
			case CTM -> handleCtm(quad, sprite, level, pos, appearanceState, state, face, out);
			case SCTM -> handleSctm(quad, sprite, level, pos, appearanceState, state, face, out);
			case HORIZONTAL, VERTICAL -> handlePlane(quad, sprite, level, pos, appearanceState, state, face, out);
			case PILLAR -> handlePillar(quad, sprite, level, pos, appearanceState, state, face, out);
			case RANDOM, PATTERN -> handleMap(quad, sprite, level, pos, appearanceState, state, face, rand, out);
			case EDGES -> handleEdges(quad, sprite, level, pos, appearanceState, state, face, out);
			case EDGES_FULL -> handleEdgesFull(quad, sprite, level, pos, appearanceState, state, face, out);
			case ELDRITCH -> handleEldritch(quad, sprite, level, pos, appearanceState, state, rand, out);
			default -> {
				if (logic != null) {
					handleCustomLogic(quad, sprite, level, pos, appearanceState, state, face, out);
				} else {
					out.add(quad);
				}
			}
		}
	}

	protected void handleNormal(BakedQuad quad, TextureAtlasSprite sprite, List<BakedQuad> out) {
		out.add(quad);
	}

	protected void handleCtm(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		int connections = connectionMap.compute(level, pos, appearanceState, state, face, sprite);
		handleCtmWithConnections(quad, sprite, connections, out);
	}

	protected void handleCtmWithConnections(BakedQuad quad, TextureAtlasSprite sprite, int connections, List<BakedQuad> out) {
		int[] submapIndices = CtmCtmLogic.getSubmapIndices(connections, connectionMap);

		TextureAtlasSprite baseSprite = sprites[0];
		TextureAtlasSprite ctmSheet = sprites.length > 1 ? sprites[1] : baseSprite;

		BakedQuad[] quadrants = QuadClipper.subdivide4(quad, baseSprite);
		for (int i = 0; i < 4; i++) {
			BakedQuad q = quadrants[i];
			if (q == null) {
				continue;
			}
			int quadrant = QuadClipper.getQuadrant(q, baseSprite);
			int ctmid = submapIndices[quadrant];
			// magic indices 16-19 use the base texture's quadrants; 0-15 use the CTM sheet's 4x4 cells
			TextureAtlasSprite target = CtmCtmLogic.isDefaultTexture(ctmid) ? baseSprite : ctmSheet;
			q = QuadClipper.grow(q, baseSprite);
			q = QuadClipper.transformUVs(q, baseSprite, target, CtmCtmLogic.UVS[ctmid]);
			out.add(q);
		}
	}

	protected void handleSctm(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		int connections = connectionMap.compute(level, pos, appearanceState, state, face, sprite);
		CtmSubmap cell = getSctmCell(connections);
		TextureAtlasSprite base = sprites[0];
		out.add(QuadClipper.transformUVs(quad, base, base, cell));
	}

	/** SCTM single-quad selection table over the 2x2 grid. */
	protected CtmSubmap getSctmCell(int connections) {
		CtmSubmap[][] x2 = CtmSubmap.x2Grid();
		boolean top = connectionMap.connected(connections, CtmDir.TOP);
		boolean bottom = connectionMap.connected(connections, CtmDir.BOTTOM);
		boolean left = connectionMap.connected(connections, CtmDir.LEFT);
		boolean right = connectionMap.connected(connections, CtmDir.RIGHT);

		if (top || bottom || left || right) {
			if (!top || !bottom) {
				// A vertical edge exists
				return x2[0][left && right ? 1 : 0];
			}
			if (!left || !right) {
				// A horizontal edge exists (and both vertical)
				return x2[1][0];
			}
			if (connectionMap.connected(connections, CtmDir.TOP_LEFT) && connectionMap.connected(connections, CtmDir.TOP_RIGHT)) {
				if (connectionMap.connected(connections, CtmDir.BOTTOM_LEFT) && connectionMap.connected(connections, CtmDir.BOTTOM_RIGHT)) {
					return x2[1][1];
				}
			}
		}
		return x2[0][0];
	}

	protected void handlePlane(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		int connections = connectionMap.compute(level, pos, appearanceState, state, face, sprite);
		CtmSubmap cell = getPlaneCell(connections, type == CtmType.VERTICAL);
		TextureAtlasSprite base = sprites[0];
		out.add(QuadClipper.transformUVs(quad, base, base, cell));
	}

	/** Plane (horizontal/vertical) 2x2 cell selection. Vertical tests TOP/BOTTOM, horizontal tests LEFT/RIGHT. */
	protected CtmSubmap getPlaneCell(int connections, boolean vertical) {
		CtmSubmap[][] x2 = CtmSubmap.x2Grid();
		if (vertical) {
			boolean top = connectionMap.connected(connections, CtmDir.TOP);
			boolean bottom = connectionMap.connected(connections, CtmDir.BOTTOM);
			int u = (top == bottom) ? 0 : 1;
			int v = top ? 1 : 0;
			return x2[v][u];
		} else {
			boolean left = connectionMap.connected(connections, CtmDir.LEFT);
			boolean right = connectionMap.connected(connections, CtmDir.RIGHT);
			int u = left ? 1 : 0;
			int v = (left == right) ? 0 : 1;
			return x2[v][u];
		}
	}

	protected void handlePillar(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		// Pillar logic: check the 6 world neighbors, apply CTM's priority pruning (vertical beats
		// east/west beats north/south), then pick a 2x2 cell of the pillar sheet and rotate it.
		TextureAtlasSprite base = sprites[0];
		TextureAtlasSprite pillar = sprites.length > 1 ? sprites[1] : base;

		// connections of the current block per facing
		EnumSet<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
		for (EnumFacing f : EnumFacing.VALUES) {
			BlockPos other = pos.offset(f);
			if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, other, face, sprite)) {
				connections.add(f);
			}
		}

		// per-neighbor connection sets (for the blockConnectionY/Z pruning)
		Map<EnumFacing, EnumSet<EnumFacing>> neighborConnections = new EnumMap<>(EnumFacing.class);
		for (EnumFacing f : EnumFacing.VALUES) {
			BlockPos other = pos.offset(f);
			IBlockState otherState = level.getBlockState(other);
			IBlockState otherAppearance = otherState.getActualState(level, other);
			EnumSet<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);
			for (EnumFacing f2 : EnumFacing.VALUES) {
				BlockPos other2 = other.offset(f2);
				if (connectionPredicate.shouldConnect(level, other, otherAppearance, otherState, other2, f2, sprite)) {
					set.add(f2);
				}
			}
			neighborConnections.put(f, set);
		}

		// Prune connections by priority
		EnumSet<EnumFacing> real = EnumSet.copyOf(connections);
		if (connectedOr(real, EnumFacing.UP, EnumFacing.DOWN)) {
			real.removeIf(f -> f.getAxis().isHorizontal());
		} else if (connectedOr(real, EnumFacing.EAST, EnumFacing.WEST)) {
			real.removeIf(f -> f == EnumFacing.NORTH || f == EnumFacing.SOUTH);
			real.removeIf(f -> blockConnectionZ(f, neighborConnections));
		} else {
			real.removeIf(f -> blockConnectionY(f, neighborConnections));
		}

		int rotation = 0;
		CtmSubmap uvs = CtmSubmap.x2Grid()[0][0];
		if (face.getAxis().isHorizontal() && connectedOr(real, EnumFacing.UP, EnumFacing.DOWN)) {
			uvs = pillarUvs(real, EnumFacing.UP, EnumFacing.DOWN);
		} else if (connectedOr(real, EnumFacing.EAST, EnumFacing.WEST)) {
			rotation = 1;
			uvs = pillarUvs(real, EnumFacing.EAST, EnumFacing.WEST);
		} else if (connectedOr(real, EnumFacing.NORTH, EnumFacing.SOUTH)) {
			uvs = pillarUvs(real, EnumFacing.NORTH, EnumFacing.SOUTH);
			if (face == EnumFacing.DOWN) {
				rotation += 2;
			}
		}

		boolean connected = !real.isEmpty();
		if (connected && !connectedOr(real, EnumFacing.UP, EnumFacing.DOWN)) {
			if (face == EnumFacing.EAST) {
				rotation += 1;
			}
			if (face == EnumFacing.NORTH) {
				rotation += 2;
			}
			if (face == EnumFacing.WEST) {
				rotation += 3;
			}
		}
		// End cap: connection opposite this face -> render as unconnected base
		if (connected && real.contains(face.getOpposite())) {
			connected = false;
		}
		// Free-standing horizontal face -> short column texture
		if (real.isEmpty() && face.getAxis().isHorizontal()) {
			connected = true;
		}

		BakedQuad q = QuadClipper.rotate(quad, base, rotation);
		if (connected) {
			out.add(QuadClipper.transformUVs(q, base, pillar, uvs));
		} else {
			out.add(QuadClipper.transformUVs(q, base, base, CtmSubmap.X1));
		}
	}

	private static boolean connectedOr(EnumSet<EnumFacing> set, EnumFacing... facings) {
		for (EnumFacing f : facings) {
			if (set.contains(f)) {
				return true;
			}
		}
		return false;
	}

	private static boolean blockConnectionZ(EnumFacing dir, Map<EnumFacing, EnumSet<EnumFacing>> neighborConnections) {
		return blockConnection(dir, EnumFacing.Axis.Z, neighborConnections);
	}

	private static boolean blockConnectionY(EnumFacing dir, Map<EnumFacing, EnumSet<EnumFacing>> neighborConnections) {
		return blockConnection(dir, EnumFacing.Axis.Y, neighborConnections)
				|| blockConnection(dir, dir.rotateY().getAxis(), neighborConnections);
	}

	private static boolean blockConnection(EnumFacing dir, EnumFacing.Axis axis, Map<EnumFacing, EnumSet<EnumFacing>> neighborConnections) {
		EnumFacing rot = dir.rotateAround(axis);
		EnumSet<EnumFacing> set = neighborConnections.get(dir);
		return set != null && (set.contains(rot) || set.contains(rot.getOpposite()));
	}

	private static CtmSubmap pillarUvs(EnumSet<EnumFacing> set, EnumFacing face1, EnumFacing face2) {
		CtmSubmap[][] x2 = CtmSubmap.x2Grid();
		if (set.contains(face1) && set.contains(face2)) {
			return x2[1][0];
		} else if (set.contains(face1)) {
			return x2[1][1];
		} else {
			return x2[0][1];
		}
	}

	protected void handleCustomLogic(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		// Custom logic: build connection bitmask over the logic's input directions
		int key = 0;
		var directions = logic.getDirections();
		for (int i = 0; i < directions.size(); i++) {
			CtmCustomLogic.LocalDirection dir = directions.get(i);
			BlockPos otherPos = pos.add(dir.getOffset(face));
			if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, otherPos, face, sprite)) {
				key |= 1 << i;
			}
		}
		int[] outputIds = logic.getOutputsForState(key);
		for (int outputId : outputIds) {
			CtmCustomLogic.OutputFace output = logic.getOutput(outputId);
			TextureAtlasSprite target = sprites[Math.min(output.tex(), sprites.length - 1)];
			BakedQuad clipped = QuadClipper.clip(quad, sprite, output.face());
			clipped = QuadClipper.transformUVs(clipped, sprite, target, output.uvs());
			out.add(clipped);
		}
	}

	protected void handleUnimplemented(BakedQuad quad, TextureAtlasSprite sprite, List<BakedQuad> out) {
		out.add(quad);
	}

	protected void handleMap(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, long rand, List<BakedQuad> out) {
		TextureAtlasSprite base = sprites[0];
		int width = properties.getMapWidth();
		int height = properties.getMapHeight();
		int xOffset = properties.getMapXOffset();
		int yOffset = properties.getMapYOffset();

		int x;
		int y;
		if (type == CtmType.RANDOM) {
			Random rng = new Random(MathHelper.getPositionRandom(pos) + face.ordinal());
			rng.nextBoolean(); // consume one value to match the reference seeding
			x = rng.nextInt(width) + 1;
			y = rng.nextInt(height) + 1;
			// 1-based coords -> cell index
			CtmSubmap cell = CtmSubmap.fromUnitScale(1f / width, 1f / height,
					(x - 1) * (1f / width), (y - 1) * (1f / height));
			out.add(QuadClipper.transformUVs(quad, base, base, cell));
			return;
		}

		// Patterned: world-coordinate modulo
		int px = pos.getX();
		int py = pos.getY();
		int pz = pos.getZ();
		int tx;
		int ty;
		EnumFacing.Axis faceAxis = face.getAxis();
		if (faceAxis == EnumFacing.Axis.Y) {
			tx = px % width;
			ty = (face.getYOffset() * pz + 1) % height;
		} else if (faceAxis == EnumFacing.Axis.Z) {
			tx = px % width;
			ty = -py % height;
		} else {
			tx = (pz + 1) % width;
			ty = -py % height;
		}
		if (face == EnumFacing.NORTH || face == EnumFacing.EAST) {
			tx = (width - tx - 1) % width;
		}
		if (tx < 0) {
			tx += width;
		}
		if (ty < 0) {
			ty += height;
		}

		CtmSubmap cell = CtmSubmap.fromUnitScale(1f / width, 1f / height,
				tx * (1f / width), ty * (1f / height));
		out.add(QuadClipper.transformUVs(quad, base, base, cell));
	}

	protected void handleEdges(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		// Edges: classic CTM with an extra "obscured" sprite (sprites[2]) used when the face is
		// directly blocked by a matching block. Delegates to the CTM logic otherwise.
		TextureAtlasSprite base = sprites[0];
		TextureAtlasSprite ctmSheet = sprites.length > 1 ? sprites[1] : base;
		TextureAtlasSprite obscured = sprites.length > 2 ? sprites[2] : base;

		// Check the obscured case: the block directly in front of the face (in the face normal
		// direction) also connects.
		boolean isObscured = false;
		BlockPos inFront = pos.offset(face);
		if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, inFront, face, sprite)) {
			isObscured = true;
		}

		if (isObscured) {
			// Render the whole face from the obscured sprite, subdivided into 4
			for (BakedQuad q : QuadClipper.subdivide4(quad, base)) {
				out.add(QuadClipper.transformUVs(QuadClipper.grow(q, base), base, obscured, CtmSubmap.X1));
			}
			return;
		}

		// Otherwise classic CTM logic
		handleCtm(quad, sprite, level, pos, appearanceState, state, face, out);
	}

	protected void handleEdgesFull(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, List<BakedQuad> out) {
		// EdgesFull: 16-cell selection over the full 4x4 sheet, one quad per face.
		int connections = connectionMap.compute(level, pos, appearanceState, state, face, sprite);
		TextureAtlasSprite base = sprites[0];
		TextureAtlasSprite sheet = sprites.length > 1 ? sprites[1] : base;

		boolean isObscured = false;
		BlockPos inFront = pos.offset(face);
		if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, inFront, face, sprite)) {
			isObscured = true;
		}

		CtmSubmap[][] x4 = CtmSubmap.x4Grid();
		CtmSubmap cell = edgesFullCell(connections, isObscured, x4);
		if (cell == null) {
			// full normal texture
			out.add(QuadClipper.transformUVs(quad, base, base, CtmSubmap.X1));
		} else {
			out.add(QuadClipper.transformUVs(quad, base, sheet, cell));
		}
	}

	/** Returns the 4x4 cell for the given connection map, or null for the full normal texture. */
	private CtmSubmap edgesFullCell(int connections, boolean isObscured, CtmSubmap[][] x4) {
		boolean top = connectionMap.connected(connections, CtmDir.TOP)
				|| connectionMap.connectedAnd(connections, CtmDir.TOP_LEFT, CtmDir.TOP_RIGHT);
		boolean right = connectionMap.connected(connections, CtmDir.RIGHT)
				|| connectionMap.connectedAnd(connections, CtmDir.TOP_RIGHT, CtmDir.BOTTOM_RIGHT);
		boolean bottom = connectionMap.connected(connections, CtmDir.BOTTOM)
				|| connectionMap.connectedAnd(connections, CtmDir.BOTTOM_LEFT, CtmDir.BOTTOM_RIGHT);
		boolean left = connectionMap.connected(connections, CtmDir.LEFT)
				|| connectionMap.connectedAnd(connections, CtmDir.TOP_LEFT, CtmDir.BOTTOM_LEFT);

		boolean any = top || right || bottom || left
				|| connectionMap.connectedOr(connections, CtmDir.TOP_LEFT, CtmDir.TOP_RIGHT, CtmDir.BOTTOM_LEFT, CtmDir.BOTTOM_RIGHT);
		if (!any) {
			return null;
		}
		if (isObscured || (top && bottom) || (right && left)) {
			return x4[2][1];
		}
		if (!top && !right && !bottom && !left) {
			if (connectionMap.connected(connections, CtmDir.TOP_LEFT) && connectionMap.connected(connections, CtmDir.BOTTOM_RIGHT)) {
				return x4[0][1];
			}
			if (connectionMap.connected(connections, CtmDir.TOP_RIGHT) && connectionMap.connected(connections, CtmDir.BOTTOM_LEFT)) {
				return x4[0][2];
			}
		}
		if (!bottom && !right
				&& connectionMap.connectedOr(connections, CtmDir.LEFT, CtmDir.BOTTOM_LEFT)
				&& connectionMap.connectedOr(connections, CtmDir.TOP, CtmDir.TOP_RIGHT)) {
			return x4[0][3];
		}
		if (!bottom && !left
				&& connectionMap.connectedOr(connections, CtmDir.TOP, CtmDir.TOP_LEFT)
				&& connectionMap.connectedOr(connections, CtmDir.RIGHT, CtmDir.BOTTOM_RIGHT)) {
			return x4[1][3];
		}
		if (!top && !left
				&& connectionMap.connectedOr(connections, CtmDir.RIGHT, CtmDir.TOP_RIGHT)
				&& connectionMap.connectedOr(connections, CtmDir.BOTTOM, CtmDir.BOTTOM_LEFT)) {
			return x4[2][3];
		}
		if (!top && !right
				&& connectionMap.connectedOr(connections, CtmDir.BOTTOM, CtmDir.BOTTOM_RIGHT)
				&& connectionMap.connectedOr(connections, CtmDir.LEFT, CtmDir.TOP_LEFT)) {
			return x4[3][3];
		}
		if (bottom) {
			return x4[1][1];
		}
		if (right) {
			return x4[2][0];
		}
		if (left) {
			return x4[2][2];
		}
		if (top) {
			return x4[3][1];
		}
		if (connectionMap.connected(connections, CtmDir.BOTTOM_LEFT)) {
			return x4[1][2];
		}
		if (connectionMap.connected(connections, CtmDir.BOTTOM_RIGHT)) {
			return x4[1][0];
		}
		if (connectionMap.connected(connections, CtmDir.TOP_RIGHT)) {
			return x4[3][0];
		}
		if (connectionMap.connected(connections, CtmDir.TOP_LEFT)) {
			return x4[3][2];
		}
		return null;
	}

	protected void handleEldritch(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, List<BakedQuad> out) {
		// Eldritch: 4 subdivided quads, each with a small UV jitter seeded by position+face.
		TextureAtlasSprite base = sprites[0];
		BakedQuad[] quads = QuadClipper.subdivide4(quad, base);
		Random rng = new Random(MathHelper.getPositionRandom(pos));
		for (BakedQuad q : quads) {
			BakedQuad grown = QuadClipper.grow(q, base);
			float du = (rng.nextFloat() - 0.5f) * 0.16f;
			float dv = (rng.nextFloat() - 0.5f) * 0.16f;
			out.add(QuadClipper.transformUVs(grown, base, base, CtmSubmap.fromUnitScale(1f + du, 1f + dv, -du / 2f, -dv / 2f)));
		}
	}

	public static class Factory implements QuadProcessor.Factory<CtmDefinition> {
		@Override
		public QuadProcessor createProcessor(CtmDefinition properties, Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
			List<ResourceLocation> spriteIds = properties.getSpriteIds();
			int amount = spriteIds.size();
			TextureAtlasSprite[] sprites = new TextureAtlasSprite[amount];
			for (int i = 0; i < amount; i++) {
				sprites[i] = spriteGetter.apply(spriteIds.get(i));
			}
			return new CtmQuadProcessor(properties, sprites);
		}
	}
}
