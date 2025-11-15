package wtf.dettex.api.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dettex.modules.impl.render.ItemPhysic;
import wtf.dettex.api.interfaces.ItemPhysicState;
import wtf.dettex.api.mixins.accessor.ItemStackEntityRenderStateAccessor;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void dettex$updateState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof ItemPhysicState phys) {
            phys.dettex$setItemOnGround(entity.isOnGround());
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void dettex$applyPhysics(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!ItemPhysic.getInstance().isState()) return;

        matrices.push();
        if (state instanceof ItemPhysicState phys && phys.dettex$isItemOnGround()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        }
        ItemStackEntityRenderStateAccessor acc = (ItemStackEntityRenderStateAccessor) (Object) state;
        acc.dettex$getItemRenderState().render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
        ci.cancel();
    }
}
