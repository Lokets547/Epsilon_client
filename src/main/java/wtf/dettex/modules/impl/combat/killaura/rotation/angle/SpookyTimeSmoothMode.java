package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

import java.util.Random;

public class SpookyTimeSmoothMode extends AngleSmoothMode {
    private static final float RETURN_SPEED = 44.0F;
    private static final float MAX_YAW_SPEED = 54.03F;
    private static final float MIN_YAW_SPEED = 39.2F;
    private static final float MAX_PITCH_SPEED = 24.2F;
    private static final float MIN_PITCH_SPEED = 6.2F;
    private static final float RANDOM_SPEED_FACTOR = 0.3F;
    private static final float YAW_RANDOM_JITTER = 3.0F;
    private static final float PITCH_RANDOM_JITTER = 0.0F;
    private static final float YAW_PITCH_COUPLING = 1.0F;
    private static final float COOLDOWN_SLOWDOWN = 1.0F;

    private static final long SMOOTH_DURATION_MS = 1500L;

    private final Random random = new Random();
    private float lastYawJitter;
    private float lastPitchJitter;

    private long smoothingStartTime = -1L;
    private Angle smoothingStartAngle;
    private Entity smoothingTarget;
    private boolean smoothingActive;

    public SpookyTimeSmoothMode() {
        super("SpookyTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {

        long now = System.currentTimeMillis();

        if (entity != null) {
            if (entity != smoothingTarget) {
                smoothingTarget = entity;
                smoothingStartAngle = AngleUtil.cameraAngle();
                smoothingStartTime = now;
                smoothingActive = true;
            }
        } else {
            smoothingTarget = null;
            smoothingActive = false;
            smoothingStartAngle = null;
        }

        Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);

        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        float yawAbs = Math.abs(yawDelta);
        float pitchAbs = Math.abs(pitchDelta);

        float yawFraction = MathHelper.clamp(yawAbs / 180.0F, 0.0F, 1.0F);
        float pitchFraction = MathHelper.clamp(pitchAbs / 90.0F, 0.0F, 1.0F);

        float yawSpeed = MathHelper.lerp(yawFraction, MIN_YAW_SPEED, MAX_YAW_SPEED);
        float pitchSpeed = MathHelper.lerp(pitchFraction, MIN_PITCH_SPEED, MAX_PITCH_SPEED);

        if (mc.player != null) {
            float cooldown = 1.0F - MathHelper.clamp(mc.player.getAttackCooldownProgress(1), 0.0F, 1.0F);
            float slowdown = MathHelper.lerp(cooldown, 1.0F, COOLDOWN_SLOWDOWN);
            yawSpeed *= slowdown;
            pitchSpeed *= slowdown;
        }

        if (entity == null) {
            yawSpeed = Math.max(yawSpeed, RETURN_SPEED);
            pitchSpeed = Math.max(pitchSpeed, RETURN_SPEED * 0.6F);
        }

        float randomScaleYaw = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPEED_FACTOR);
        float randomScalePitch = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPEED_FACTOR * YAW_PITCH_COUPLING);

        yawSpeed = MathHelper.clamp(yawSpeed * randomScaleYaw, MIN_YAW_SPEED, MAX_YAW_SPEED);
        pitchSpeed = MathHelper.clamp(pitchSpeed * randomScalePitch, MIN_PITCH_SPEED, MAX_PITCH_SPEED);

        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        lastYawJitter = randomJitter(YAW_RANDOM_JITTER, lastYawJitter);
        lastPitchJitter = randomJitter(PITCH_RANDOM_JITTER, lastPitchJitter);

        Angle moveAngle = new Angle(currentAngle.getYaw() + yawStep + lastYawJitter,
                MathHelper.clamp(currentAngle.getPitch() + pitchStep + lastPitchJitter, -89.0F, 90.0F));

        Angle resultAngle = moveAngle;
        if (smoothingActive && smoothingStartAngle != null) {
            float elapsed = (now - smoothingStartTime) / (float) SMOOTH_DURATION_MS;
            if (elapsed >= 1.0F) {
                smoothingActive = false;
                smoothingStartAngle = null;
            } else {
                float progress = easeOutCubic(elapsed);
                float yaw = interpolateYaw(smoothingStartAngle.getYaw(), moveAngle.getYaw(), progress);
                float pitch = MathHelper.lerp(progress, smoothingStartAngle.getPitch(), moveAngle.getPitch());
                resultAngle = new Angle(yaw, pitch);
            }
        }

        return resultAngle.adjustSensitivity();
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }

    private float randomJitter(float bound, float previous) {
        if (bound <= 0.0F) return 0.0F;
        float next = (random.nextFloat() * 2.0F - 1.0F) * bound;
        return MathHelper.lerp(0.35F, previous, next);
    }

    private float interpolateYaw(float start, float end, float progress) {
        float delta = MathHelper.wrapDegrees(end - start);
        return start + delta * progress;
    }

    private float easeOutCubic(float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }
}
