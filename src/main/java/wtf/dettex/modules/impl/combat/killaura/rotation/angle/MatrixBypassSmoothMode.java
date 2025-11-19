package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;

import java.util.concurrent.ThreadLocalRandom;

public class MatrixBypassSmoothMode extends AngleSmoothMode {
    private Vec2f rotateVector = null;
    private float lastPitch = 0;

    public MatrixBypassSmoothMode() {
        super("Matrix|Bypass");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        // Инициализируем rotateVector текущим углом при первом вызове
        if (rotateVector == null) {
            rotateVector = new Vec2f(currentAngle.getPitch(), currentAngle.getYaw());
        }
        
        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        // Clamp yaw and pitch deltas
        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 0.42f), 89);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 0.42f), 89);

        // Calculate new yaw with randomness (rotateVector.y = yaw)
        float yaw = rotateVector.y + (yawDelta > 0 ? clampedYaw : -clampedYaw) 
                + ThreadLocalRandom.current().nextFloat(-0.5f, 0.5f);
        
        // Calculate new pitch with clamping and randomness (rotateVector.x = pitch)
        float pitch = MathHelper.clamp(
                rotateVector.x + (pitchDelta > 0 ? clampedPitch : -clampedPitch), 
                -90.0F, 
                90.0F
        ) + ThreadLocalRandom.current().nextFloat(-0.5f, 0.5f);

        // Apply GCD correction
        float gcd = (float) MathUtil.computeGcd();
        yaw -= (yaw - rotateVector.y) % gcd;
        pitch -= (pitch - rotateVector.x) % gcd;
        
        // Update rotate vector for next iteration
        rotateVector = new Vec2f(pitch, yaw);
        
        // Update lastPitch
        lastPitch = clampedPitch;

        return new Angle(yaw, pitch);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}
