package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

import java.security.SecureRandom;

public class HvHSmoothMode extends AngleSmoothMode {
    public HvHSmoothMode() {
        super("HvH");
    }

    
    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float yawSpeed = 180f;
        float pitchSpeed = 140f;

        float moveYaw = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float movePitch = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);


        float jitterYaw = (float) (Math.ceil(randomLerp(0.2f, 0.6f) * Math.cos(System.currentTimeMillis() / 360D)));
        float jitterPitch = (float) (Math.ceil(randomLerp(0.1f, 0.3f) * Math.sin(System.currentTimeMillis() / 360D)));

        Angle move = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        move.setYaw(MathHelper.lerp(1.0f, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        move.setPitch(MathHelper.lerp(1.0f, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);
        return move;
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.05, 0.05, 0.05);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}
