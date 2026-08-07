package me.pepperbell.continuity.client.model;

import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.api.client.QuadProcessor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class QuadProcessors {
	private static ProcessorHolder[] processorHolders = new ProcessorHolder[0];
	private static final BlockStateKeyCache CACHE = new BlockStateKeyCache();

	private QuadProcessors() {
	}

	public static SpriteKeyCache getCache(IBlockState state) {
		return CACHE.apply(state);
	}

	private static Slice computeSlice(IBlockState state, TextureAtlasSprite sprite) {
		List<QuadProcessor> processorList = new ObjectArrayList<>();
		List<QuadProcessor> multipassProcessorList = new ObjectArrayList<>();

		for (ProcessorHolder holder : processorHolders) {
			QuadProcessor processor = holder.processor();
			CachingPredicates predicates = holder.predicates();
			if (!predicates.affectsBlockStates() || predicates.affectsBlockState(state)) {
				if (predicates.affectsSprites()) {
					if (predicates.affectsSprite(sprite)) {
						processorList.add(processor);
						if (predicates.isValidForMultipass()) {
							multipassProcessorList.add(processor);
						}
					}
				} else {
					processorList.add(processor);
				}
			}
		}

		QuadProcessor[] processors = processorList.toArray(new QuadProcessor[0]);
		QuadProcessor[] multipassProcessors = multipassProcessorList.toArray(new QuadProcessor[0]);
		return new Slice(processors, multipassProcessors);
	}

	public static void reload(List<QuadProcessors.ProcessorHolder> processorHolders) {
		QuadProcessors.processorHolders = processorHolders.toArray(new ProcessorHolder[0]);
		CACHE.clear();
	}

	public record ProcessorHolder(QuadProcessor processor, CachingPredicates predicates) {
	}

	public record Slice(QuadProcessor[] processors, QuadProcessor[] multipassProcessors) {
	}

	private static class BlockStateKeyCache {
		private final Reference2ReferenceOpenHashMap<IBlockState, SpriteKeyCache> map = new Reference2ReferenceOpenHashMap<>();
		private final StampedLock lock = new StampedLock();

		public SpriteKeyCache apply(IBlockState state) {
			SpriteKeyCache innerCache;

			long optimisticReadStamp = lock.tryOptimisticRead();
			if (optimisticReadStamp != 0L) {
				try {
					innerCache = map.get(state);
					if (innerCache != null && lock.validate(optimisticReadStamp)) {
						return innerCache;
					}
				} catch (Exception e) {
					// fast path read can race with a write
				}
			}

			long readStamp = lock.readLock();
			try {
				innerCache = map.get(state);
			} finally {
				lock.unlockRead(readStamp);
			}

			if (innerCache == null) {
				long writeStamp = lock.writeLock();
				try {
					innerCache = map.get(state);
					if (innerCache == null) {
						innerCache = new SpriteKeyCache(state);
						map.put(state, innerCache);
					}
				} finally {
					lock.unlockWrite(writeStamp);
				}
			}

			return innerCache;
		}

		public void clear() {
			long writeStamp = lock.writeLock();
			try {
				map.values().forEach(SpriteKeyCache::clear);
			} finally {
				lock.unlockWrite(writeStamp);
			}
		}
	}

	public static class SpriteKeyCache implements Function<TextureAtlasSprite, Slice> {
		public final Reference2ReferenceOpenHashMap<TextureAtlasSprite, Slice> map = new Reference2ReferenceOpenHashMap<>(4, Hash.FAST_LOAD_FACTOR);
		private final StampedLock lock = new StampedLock();
		private final IBlockState state;

		public SpriteKeyCache(IBlockState state) {
			this.state = state;
		}

		@Override
		public Slice apply(TextureAtlasSprite sprite) {
			Slice slice;

			long optimisticReadStamp = lock.tryOptimisticRead();
			if (optimisticReadStamp != 0L) {
				try {
					slice = map.get(sprite);
					if (slice != null && lock.validate(optimisticReadStamp)) {
						return slice;
					}
				} catch (Exception e) {
					// fast path read can race with a write
				}
			}

			long readStamp = lock.readLock();
			try {
				slice = map.get(sprite);
			} finally {
				lock.unlockRead(readStamp);
			}

			if (slice == null) {
				long writeStamp = lock.writeLock();
				try {
					slice = map.get(sprite);
					if (slice == null) {
						slice = computeSlice(state, sprite);
						map.put(sprite, slice);
					}
				} finally {
					lock.unlockWrite(writeStamp);
				}
			}

			return slice;
		}

		public void clear() {
			long writeStamp = lock.writeLock();
			try {
				map.clear();
			} finally {
				lock.unlockWrite(writeStamp);
			}
		}
	}
}
