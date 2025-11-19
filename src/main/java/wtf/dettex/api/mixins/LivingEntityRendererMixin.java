package wtf.dettex.api.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.dettex.event.EventManager;
import wtf.dettex.common.QuickImports;
import wtf.dettex.event.impl.render.EntityColorEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin implements QuickImports {
    @Shadow @Nullable protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Unique private static LivingEntity CURRENT_ENTITY;

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!translucent && state.width == 0.6F) {
            EntityColorEvent event = new EntityColorEvent(-1, CURRENT_ENTITY);
            EventManager.callEvent(event);
            if (event.isCancelled()) translucent = true;
        }
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l,
                                 @Local(argsOnly = true) VertexConsumerProvider vertexConsumers,
                                 @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        EntityColorEvent event = new EntityColorEvent(l, CURRENT_ENTITY);
        EventManager.callEvent(event);
        boolean throughWalls = event.isCancelled() && CURRENT_ENTITY instanceof PlayerEntity;
        if (throughWalls && vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
            immediate.draw();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        } else {
            instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
        }
    }

    @Redirect(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    private float lerpAngleDegreesHook(float f, float g, float h, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta) {
        CURRENT_ENTITY = entity;
        if (entity.equals(mc.player)) {
            RotationController controller = RotationController.INSTANCE;
            // Only override body yaw in first-person; use vanilla interpolation in third-person to avoid jitter
            if (!mc.options.getPerspective().isFirstPerson()) {
                return MathHelper.lerpAngleDegrees(f, g, h);
            }
            return MathHelper.lerpAngleDegrees(delta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
        }
        return MathHelper.lerpAngleDegrees(f, g, h);
    }

    @Redirect(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float getLerpedPitchHook(LivingEntity instance, float f, @Local(ordinal = 0, argsOnly = true) float delta) {
        CURRENT_ENTITY = instance;
        if (instance.equals(mc.player)) {
            RotationController controller = RotationController.INSTANCE;
            // Only override in first-person to prevent visible jitter on the model in third-person
            if (!mc.options.getPerspective().isFirstPerson()) {
                return instance.getLerpedPitch(f);
            }
            return MathHelper.lerp(delta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
        }
        return instance.getLerpedPitch(f);
    }
}
