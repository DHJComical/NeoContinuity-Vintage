package me.pepperbell.continuity.client.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.jetbrains.annotations.Unmodifiable;

// import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
// import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
// import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.minecraft.client.Minecraft;
// import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
// import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public final class SpriteCalculator {
	private static final ModelManager MODEL_MANAGER = Minecraft.getInstance().getModelManager();

	private static final EnumMap<Direction, SpriteCache> SPRITE_CACHES = new EnumMap<>(Direction.class);

	static {
		for (Direction direction : Direction.values()) {
			SPRITE_CACHES.put(direction, new SpriteCache(direction));
		}

		// InvalidateRenderStateCallback.EVENT.register(SpriteCalculator::clearCache);
	}

	@Unmodifiable
	public static Set<TextureAtlasSprite> getSprites(BlockState state, Direction face) {
		return SPRITE_CACHES.get(face).getSprites(state);
	}

	public static void clearCache() {
		for (SpriteCache cache : SPRITE_CACHES.values()) {
			cache.clear();
		}
	}

	private static class SpriteCache {
		private final Direction face;
		private final Reference2ObjectOpenHashMap<BlockState, Set<TextureAtlasSprite>> spritesMap = new Reference2ObjectOpenHashMap<>();
		// private final MutableMesh mutableMesh = Renderer.get().mutableMesh();
		// private final CollectingQuadTransform quadTransform;
		private final List<BlockStateModelPart> scratchList;
		private final RandomSource random = RandomSource.createThreadLocalInstance();
		private final StampedLock lock = new StampedLock();

		public SpriteCache(Direction face) {
			this.face = face;
			// quadTransform = new CollectingQuadTransform(face);
			scratchList = new ArrayList<>();
		}

		@Unmodifiable
		public Set<TextureAtlasSprite> getSprites(BlockState state) {
			Set<TextureAtlasSprite> sprites;

			long optimisticReadStamp = lock.tryOptimisticRead();
			if (optimisticReadStamp != 0L) {
				try {
					// This map read could happen at the same time as a map write, so catch any exceptions.
					// This is safe due to the map implementation used, which is guaranteed to not mutate the map during
					// a read.
					sprites = spritesMap.get(state);
					if (sprites != null && lock.validate(optimisticReadStamp)) {
						return sprites;
					}
				} catch (Exception e) {
					//
				}
			}

			long readStamp = lock.readLock();
			try {
				sprites = spritesMap.get(state);
			} finally {
				lock.unlockRead(readStamp);
			}

			if (sprites == null) {
				long writeStamp = lock.writeLock();
				try {
					sprites = spritesMap.get(state);
					if (sprites == null) {
						sprites = calculateSprites(state);
						spritesMap.put(state, sprites);
					}
				} finally {
					lock.unlockWrite(writeStamp);
				}
			}

			return sprites;
		}

		@Unmodifiable
		@SuppressWarnings("deprecated")
		private Set<TextureAtlasSprite> calculateSprites(BlockState state) {
			BlockStateModel model = MODEL_MANAGER.getBlockStateModelSet().get(state);
			/* QuadEmitter emitter = mutableMesh.emitter();
			quadTransform.clear();
			emitter.pushTransform(quadTransform);
			random.setSeed(42);
			try {
				model.emitQuads(emitter, BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, random, cullFace -> false);
			} catch (Exception e) {
				//
			}
			emitter.popTransform();
			Set<TextureAtlasSprite> sprites = quadTransform.result(); */

			model.collectParts(random, scratchList);

			ObjectOpenHashSet<TextureAtlasSprite> sprites = new ObjectOpenHashSet<>();

			for (BlockStateModelPart part : scratchList) {
				for (Direction direction : RenderUtil.DIRECTIONS) {
					for (BakedQuad quad : part.getQuads(direction)) {
						sprites.add(quad.materialInfo().sprite());
					}
				}

				for (BakedQuad quad : part.getQuads(null)) {
					sprites.add(quad.materialInfo().sprite());
				}
			}

			scratchList.clear();

			return !sprites.isEmpty() ? sprites : Set.of(model.particleMaterial().sprite());
		}

		public void clear() {
			long writeStamp = lock.writeLock();
			try {
				spritesMap.clear();
				// quadTransform.clear();
			} finally {
				lock.unlockWrite(writeStamp);
			}
		}

		/* private static class CollectingQuadTransform implements QuadTransform {
			private final Direction face;
			private final List<TextureAtlasSprite> sprites = new ObjectArrayList<>();

			private CollectingQuadTransform(Direction face) {
				this.face = face;
			}

			@Override
			public boolean transform(MutableQuadView quad) {
				if (quad.lightFace() == face) {
					sprites.add(RenderUtil.getSpriteFinder().find(quad));
				}
				return false;
			}

			public void clear() {
				sprites.clear();
			}

			@Unmodifiable
			public Set<TextureAtlasSprite> result() {
				return Set.copyOf(sprites);
			}
		} */
	}
}
