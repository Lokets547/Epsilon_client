package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.Main;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.impl.combat.killaura.attack.AttackHandler;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

import java.security.SecureRandom;

public class UniversalSmoothMode extends AngleSmoothMode {
    public UniversalSmoothMode() {
        super("Universal");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        AttackHandler attackHandler = Main.getInstance().getAttackPerpetrator().getAttackHandler();
        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        Aura aura = Aura.getInstance();
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float yaw = canAttack ? 0 : (float) (randomLerp(4, 6) * Math.sin(System.currentTimeMillis() / 40D));
        float pitch = canAttack ? 0 : (float) (randomLerp(2, 3) * Math.cos(System.currentTimeMillis() / 40D));

        float speed = entity != null && attackHandler.canAttack(aura.getConfig(), 0) ? 1 : mc.player.age % 2 == 0 ? new SecureRandom().nextBoolean() ? 0.5F : 0.3F : 0;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed + 0.1875F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw);
        moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed + 0.155F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch);
        return moveAngle;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}
