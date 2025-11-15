package wtf.dettex.api.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dettex.event.EventManager;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.common.util.render.Backdrop;
import wtf.dettex.event.impl.render.AspectRatioEvent;
import wtf.dettex.event.impl.render.FovEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RaytracingUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.impl.misc.DebugCamera;
import wtf.dettex.modules.impl.render.NoRender;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Final @Shadow private MinecraftClient client;

    @Shadow private float zoom;

    @Shadow private float zoomX;

    @Shadow private float zoomY;

    @Shadow public abstract float getFarPlaneDistance();

    @Inject(method = "updateCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo ci) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        DebugCamera debugCamera = DebugCamera.getInstance();
        if (debugCamera.isState()) {
            Profilers.get().pop();
            client.crosshairTarget = RaytracingUtil.raycast(debugCamera.pos,4.5, AngleUtil.cameraAngle(),false);
            ci.cancel();
        }
    }

    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult hookRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        if (instance != client.player) return instance.raycast(maxDistance, tickDelta, includeFluids);
        return RaytracingUtil.raycast(maxDistance, RotationController.INSTANCE.getRotation(), includeFluids);
    }

    @Redirect(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookRotationVector(Entity instance, float tickDelta) {
        return RotationController.INSTANCE.getRotation().toVector();
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioEvent aspectRatioEvent = new AspectRatioEvent();
        EventManager.callEvent(aspectRatioEvent);
        if (aspectRatioEvent.isCancelled()) {
            Matrix4f matrix4f = new Matrix4f();
            if (zoom != 1.0f) {
                matrix4f.translate(zoomX, -zoomY, 0.0f);
                matrix4f.scale(zoom, zoom, 1.0f);
            }
            matrix4f.perspective(fovDegrees * 0.01745329238474369F, aspectRatioEvent.getRatio(), 0.05f, getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }

    @ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        FovEvent event = new FovEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) return event.getFov();
        return original;
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);
        matrixStack.translate(client.getEntityRenderDispatcher().camera.getPos().negate());

        Render3DUtil.setLastProjMat(RenderSystem.getProjectionMatrix());
        Render3DUtil.setLastWorldSpaceMatrix(matrixStack.peek());

        WorldRenderEvent event = new WorldRenderEvent(matrixStack, tickCounter.getTickDelta(false));
        Backdrop.capture();
        EventManager.callEvent(event);
        Render3DUtil.onWorldRender(event);
    }

    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;"), method = "tiltViewWhenHurt", require = 4)
    public float changeBobIntensity(float value) {
        return (float) (0);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    public void disableHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!NoRender.getInstance().isState() || !NoRender.getInstance().modeSetting.isSelected("Hurt Cam")) ci.cancel();
    }

    @WrapOperation(method = "tiltViewWhenHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getDamageTiltYaw()F"))
    public float changeHurtCamType(LivingEntity instance, Operation<Float> original) {
        return (float) (0);
    }
}
