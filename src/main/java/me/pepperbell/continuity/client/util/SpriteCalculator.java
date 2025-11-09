package me.pepperbell.continuity.client.util;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.jetbrains.annotations.Unmodifiable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockRenderView;

public final class SpriteCalculator {
	private static final BlockModels MODELS = MinecraftClient.getInstance().getBakedModelManager().getBlockModels();

	private static final EnumMap<Direction, SpriteCache> SPRITE_CACHES = new EnumMap<>(Direction.class);

	static {
		for (Direction direction : Direction.values()) {
			SPRITE_CACHES.put(direction, new SpriteCache(direction));
		}

		InvalidateRenderStateCallback.EVENT.register(SpriteCalculator::clearCache);
	}

	@Unmodifiable
	public static Set<Sprite> getSprites(BlockState state, Direction face) {
		return SPRITE_CACHES.get(face).getSprites(state);
	}

	public static void clearCache() {
		for (SpriteCache cache : SPRITE_CACHES.values()) {
			cache.clear();
		}
	}

	private static class SpriteCache {
		private final Direction face;
		private final Reference2ObjectOpenHashMap<BlockState, Set<Sprite>> spritesMap = new Reference2ObjectOpenHashMap<>();
		private final MutableMesh mutableMesh = Renderer.get().mutableMesh();
		private final CollectingQuadTransform quadTransform;
		private final Supplier<Random> randomSupplier = new Supplier<>() {
			private final Random random = Random.create();

			@Override
			public Random get() {
				// Use item rendering seed for consistency
				random.setSeed(42L);
				return random;
			}
		};
		private final StampedLock lock = new StampedLock();

		public SpriteCache(Direction face) {
			this.face = face;
			quadTransform = new CollectingQuadTransform(face);
		}

		@Unmodifiable
		public Set<Sprite> getSprites(BlockState state) {
			Set<Sprite> sprites;

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
		private Set<Sprite> calculateSprites(BlockState state) {
			BakedModel model = MODELS.getModel(state);
			QuadEmitter emitter = mutableMesh.emitter();
			quadTransform.clear();
			emitter.pushTransform(quadTransform);
			try {
				model.emitBlockQuads(emitter, EmptyBlockRenderView.INSTANCE, state, BlockPos.ORIGIN, randomSupplier, cullFace -> false);
			} catch (Exception e) {
				//
			}
			emitter.popTransform();
			Set<Sprite> sprites = quadTransform.result();
			return !sprites.isEmpty() ? sprites : Set.of(model.getParticleSprite());
		}

		public void clear() {
			long writeStamp = lock.writeLock();
			try {
				spritesMap.clear();
				quadTransform.clear();
			} finally {
				lock.unlockWrite(writeStamp);
			}
		}

		private static class CollectingQuadTransform implements QuadTransform {
			private final Direction face;
			private final List<Sprite> sprites = new ObjectArrayList<>();

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
			public Set<Sprite> result() {
				return Set.copyOf(sprites);
			}
		}
	}
}
