package me.pepperbell.continuity.client.mixin;

// import java.util.List;

// import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;

// import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
// import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

// import me.pepperbell.continuity.client.resource.CuboidItemModelWrapperInitContext;
// import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
// import net.minecraft.client.renderer.item.ItemModel;
// import net.minecraft.client.renderer.item.ModelRenderProperties;
// import net.minecraft.client.resources.model.geometry.QuadCollection;

@Mixin(CuboidItemModelWrapper.Unbaked.class)
abstract class CuboidItemModelWrapperUnbakedMixin {
	/* @WrapOperation(method = "bake(Lnet/minecraft/client/renderer/item/ItemModel$BakingContext;Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/item/ItemModel;", at = @At(value = "NEW", target = "(Ljava/util/List;Lnet/minecraft/client/resources/model/geometry/QuadCollection;Lnet/minecraft/client/renderer/item/ModelRenderProperties;Lorg/joml/Matrix4fc;)Lnet/minecraft/client/renderer/item/CuboidItemModelWrapper;"))
	private CuboidItemModelWrapper wrapModelCtor(List<ItemTintSource> tints, QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation, Operation<CuboidItemModelWrapper> original, ItemModel.BakingContext context, Matrix4fc transformation1) {
		CuboidItemModelWrapperInitContext.SPRITE_FINDER_GETTER.set(context.blockModelBaker().materials());
		CuboidItemModelWrapper model = original.call(tints, quads, properties, transformation);
		CuboidItemModelWrapperInitContext.SPRITE_FINDER_GETTER.remove();
		return model;
	} */
}
