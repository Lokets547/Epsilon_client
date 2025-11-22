package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

public class SpookyTimeTestSmoothMode extends AngleSmoothMode {
    public SpookyTimeTestSmoothMode() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 0.0F), 84.8F);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 0.0F), 29.3F);

        float targetYaw = currentAngle.getYaw() + (yawDelta > 0.0F ? clampedYaw : -clampedYaw);
        float targetPitch = currentAngle.getPitch() + (pitchDelta > 0.0F ? clampedPitch : -clampedPitch);

        float lerp = 0.687F;
        float yaw = currentAngle.getYaw() + (targetYaw - currentAngle.getYaw()) * lerp;
        float pitch = currentAngle.getPitch() + (targetPitch - currentAngle.getPitch()) * lerp;

        float time = (float) (System.currentTimeMillis() % 10000L) / 1180.0F;
        yaw += (float) Math.sin((double) (time * 2.0F) * Math.PI * 3.0) * 6.4F;

        pitch = MathHelper.clamp(pitch, -89.0F, 90.0F);

        Angle result = new Angle(yaw, pitch);
        return result.adjustSensitivity();
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}
