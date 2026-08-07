package me.pepperbell.continuity.client.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.mixinterface.TextureAtlasSpriteExtension;
import me.pepperbell.continuity.client.resource.SpriteLoaderLoadContext;
import me.pepperbell.continuity.client.resource.SpriteLoaderStitchContext;
import me.pepperbell.continuity.client.resource.SpriteSourceListInitContext;
import me.pepperbell.continuity.client.resource.SpriteSourceListListContext;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.Identifier;

@Mixin(SpriteLoader.class)
abstract class SpriteLoaderMixin {
	@Shadow
	@Final
	private Identifier location;

	@ModifyArg(method = "loadAndStitch(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/Identifier;ILjava/util/concurrent/Executor;Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0), index = 0)
	private Supplier<List<Function<SpriteResourceLoader, SpriteContents>>> continuity$modifySupplier(Supplier<List<Function<SpriteResourceLoader, SpriteContents>>> supplier) {
		SpriteLoaderLoadContext context = SpriteLoaderLoadContext.THREAD_LOCAL.get();
		if (context != null) {
			CompletableFuture<Set<Identifier>> extraIdsFuture = context.getExtraIdsFuture(location);
			SpriteLoaderLoadContext.EmissiveControl emissiveControl = context.getEmissiveControl(location);
			if (extraIdsFuture != null && emissiveControl != null) {
				return () -> {
					SpriteSourceListInitContext.THREAD_LOCAL.set(extraIdsFuture::join);
					SpriteSourceListListContext.THREAD_LOCAL.set(emissiveControl::setEmissiveIdMap);
					List<Function<SpriteResourceLoader, SpriteContents>> list = supplier.get();
					SpriteSourceListInitContext.THREAD_LOCAL.remove();
					SpriteSourceListListContext.THREAD_LOCAL.remove();
					return list;
				};
			} else if (extraIdsFuture != null) {
				return () -> {
					SpriteSourceListInitContext.THREAD_LOCAL.set(extraIdsFuture::join);
					List<Function<SpriteResourceLoader, SpriteContents>> list = supplier.get();
					SpriteSourceListInitContext.THREAD_LOCAL.remove();
					return list;
				};
			} else if (emissiveControl != null) {
				return () -> {
					SpriteSourceListListContext.THREAD_LOCAL.set(emissiveControl::setEmissiveIdMap);
					List<Function<SpriteResourceLoader, SpriteContents>> list = supplier.get();
					SpriteSourceListListContext.THREAD_LOCAL.remove();
					return list;
				};
			}
		}
		return supplier;
	}

	@ModifyArg(method = "loadAndStitch(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/Identifier;ILjava/util/concurrent/Executor;Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenApply(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0), index = 0)
	private Function<List<SpriteContents>, SpriteLoader.Preparations> continuity$modifyFunction(Function<List<SpriteContents>, SpriteLoader.Preparations> function) {
		SpriteLoaderLoadContext context = SpriteLoaderLoadContext.THREAD_LOCAL.get();
		if (context != null) {
			SpriteLoaderLoadContext.EmissiveControl emissiveControl = context.getEmissiveControl(location);
			if (emissiveControl != null) {
				return spriteContentsList -> {
					Map<Identifier, Identifier> emissiveIdMap = emissiveControl.getEmissiveIdMap();
					if (emissiveIdMap != null) {
						SpriteLoaderStitchContext.THREAD_LOCAL.set(new SpriteLoaderStitchContext() {
							@Override
							public Map<Identifier, Identifier> getEmissiveIdMap() {
								return emissiveIdMap;
							}

							@Override
							public void setHasEmissives(boolean hasEmissives) {
								emissiveControl.setHasEmissives(hasEmissives);
							}
						});
						SpriteLoader.Preparations result = function.apply(spriteContentsList);
						SpriteLoaderStitchContext.THREAD_LOCAL.remove();
						return result;
					}
					return function.apply(spriteContentsList);
				};
			}
		}
		return function;
	}

	@Inject(method = "stitch(Ljava/util/List;ILjava/util/concurrent/Executor;)Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;", at = @At("RETURN"))
	private void continuity$onReturnStitch(List<SpriteContents> spriteContentsList, int mipmapLevels, Executor executor, CallbackInfoReturnable<SpriteLoader.Preparations> cir) {
		SpriteLoaderStitchContext context = SpriteLoaderStitchContext.THREAD_LOCAL.get();
		if (context != null) {
			Map<Identifier, Identifier> emissiveIdMap = context.getEmissiveIdMap();
			Map<Identifier, TextureAtlasSprite> sprites = cir.getReturnValue().regions();
			MutableBoolean hasEmissives = new MutableBoolean(false);
			emissiveIdMap.forEach((id, emissiveId) -> {
				TextureAtlasSprite sprite = sprites.get(id);
				if (sprite != null) {
					TextureAtlasSprite emissiveSprite = sprites.get(emissiveId);
					if (emissiveSprite != null) {
						((TextureAtlasSpriteExtension) sprite).continuity$setEmissiveSprite(emissiveSprite);
						hasEmissives.setTrue();
					}
				}
			});
			context.setHasEmissives(hasEmissives.booleanValue());
		}
	}
}
