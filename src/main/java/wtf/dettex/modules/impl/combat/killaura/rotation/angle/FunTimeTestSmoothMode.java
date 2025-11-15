package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import antidaunleak.api.annotation.Native;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

public class FunTimeTestSmoothMode extends AngleSmoothMode {
    private static final float SHAKE_INTENSITY = 2.0F;
    private static final float SHAKE_STRENGTH = 20.0F;
    private static final float CIRCLE_AMPLITUDE = 5.0F;
    private static final float CIRCLE_SPEED = 0.25F;

    public FunTimeTestSmoothMode() {
        super("FunTimeSmooth");
    }

    
    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

//        if (Math.abs(yawDelta) == 0.0F && Math.abs(pitchDelta) > 0.0F) {
//            yawDelta += MathUtil.getRandom(0.1F, 0.5F) + 0.1F * 1.0313F;
//        }
//        if (Math.abs(pitchDelta) == 0.0F && Math.abs(yawDelta) > 0.0F) {
//            pitchDelta += MathUtil.getRandom(0.1F, 0.5F) + 0.1F * 1.0313F;
//        }

        float maxYaw = 50.0F + MathUtil.getRandom(0.0F, 5.0329834F);
        float maxPitch = MathUtil.getRandom(18.133F, 23.477F);
        float clampedYaw = Math.min(Math.abs(yawDelta), maxYaw);
        float clampedPitch = Math.min(Math.abs(pitchDelta), maxPitch);

        float time = (float) (System.currentTimeMillis() % 10000L) / 570.0F;
        float smoothShakeYaw = 6.0F * (float) Math.sin(time * 7.0F);

        boolean dynamicOffsets = isShakeActive();
        float bodyShakePhase = dynamicOffsets ? (float) Math.sin(time * SHAKE_INTENSITY * Math.PI * 2.0F) : 0.0F;
        float bodyShakeOffset = dynamicOffsets ? bodyShakePhase * SHAKE_STRENGTH : 0.0F;

        float circlePhase = dynamicOffsets ? time * CIRCLE_SPEED * MathHelper.TAU : 0.0F;
        float circleYawOffset = dynamicOffsets ? MathHelper.sin(circlePhase) * CIRCLE_AMPLITUDE : 0.0F;
        float circlePitchOffset = dynamicOffsets ? MathHelper.cos(circlePhase) * (CIRCLE_AMPLITUDE * 0.6F) : 0.0F;

        float yaw = currentAngle.getYaw() + (yawDelta > 0.0F ? clampedYaw : -clampedYaw) + smoothShakeYaw + bodyShakeOffset + circleYawOffset;

        float movePitch = MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch);
        float desiredPitch = currentAngle.getPitch() + movePitch;
        float basePitch = MathHelper.lerp(0.45F, currentAngle.getPitch(), desiredPitch);


        float appliedCirclePitch = (entity != null ? circlePitchOffset * 0.2F : circlePitchOffset);
        float pitch = MathHelper.clamp(basePitch + appliedCirclePitch, -89.0F, 90.0F);

        Angle result = new Angle(yaw, pitch);
        return result.adjustSensitivity();
    }


    private boolean isShakeActive() {
        Aura aura = Aura.getInstance();
        return aura != null
                && aura.isState()
                && aura.getAimMode().isSelected("FuntimeSmooth");
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.08, 0.1, 0.08);
    }
}