package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

public class SpookyDuelSmoothMode extends AngleSmoothMode {
    public SpookyDuelSmoothMode() {
        super("SpookyDuels");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        float speed = entity != null ? 0.55F : 0.45F;

float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180) * 0.5F;
float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180) * 0.5F;

float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
moveAngle.setYaw(MathHelper.lerp(MathUtil.getRandom(speed + 0.03F, speed + 0.06F),
        currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
moveAngle.setPitch(MathHelper.lerp(MathUtil.getRandom(speed, speed + 0.04F),
        currentAngle.getPitch(), currentAngle.getPitch() + movePitch));

        return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
    }
    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.12F, 0.12F, 0.12F);
    }
}
