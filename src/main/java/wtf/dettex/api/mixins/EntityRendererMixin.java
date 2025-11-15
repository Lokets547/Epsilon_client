package wtf.dettex.api.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dettex.modules.impl.render.EntityESP;
import wtf.dettex.modules.impl.render.ItemPhysic;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (EntityESP.getInstance().isState() && canRemove((int) (state.width * 100), EntityESP.getInstance())) {
            ci.cancel();
        }
    }

    @Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
    private void dettex$noShadowRadius(S state, CallbackInfoReturnable<Float> cir) {
        if (state instanceof ItemEntityRenderState && ItemPhysic.getInstance().isState()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getShadowOpacity", at = @At("HEAD"), cancellable = true)
    private void dettex$noShadowOpacity(S state, CallbackInfoReturnable<Float> cir) {
        if (state instanceof ItemEntityRenderState && ItemPhysic.getInstance().isState()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Unique
    private boolean canRemove(int width, EntityESP esp) {
       return switch (width) {
           case 60 -> esp.entityType.isSelected("Player");
           case 98 -> esp.entityType.isSelected("TNT");
           default -> false;
       };
    }
}