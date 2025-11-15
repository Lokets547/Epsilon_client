package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

import java.util.concurrent.ThreadLocalRandom;

public class TraxSmoothMode extends AngleSmoothMode {
    private static final float DATASET_YAW_ABS_MEDIAN = 40.88F;
    private static final float DATASET_YAW_ABS_P75 = 34.77F;
    private static final float DATASET_YAW_ABS_P90 = 45.00F;
    private static final float DATASET_PITCH_ABS_MEDIAN = 17.44F;
    private static final float DATASET_PITCH_ABS_P75 = 20.133F;
    private static final float DATASET_PITCH_ABS_P90 = 22.477F;

    private static final float MIN_YAW_LIMIT = DATASET_YAW_ABS_P75;
    private static final float MIN_PITCH_LIMIT = DATASET_PITCH_ABS_P75;
    private static final float MIN_YAW_STEP = 1.3F;
    private static final float MIN_PITCH_STEP = 0.6F;
    private float currentYawSpeed = DATASET_YAW_ABS_P90;
    private float currentPitchSpeed = DATASET_PITCH_ABS_P75;
    private float yawVelocity;
    private float pitchVelocity;
    private float smoothedYawStep;
    private float smoothedPitchStep;
    private float reactionBias;
    private float pitchReactionBias;
    private float attackYawBias;
    private float attackPitchBias;
    private int lastKnownHurtTime;
    private long lastHitTimestamp;
    private Vec3d lastAnchoredPoint = Vec3d.ZERO;
    private boolean hasAnchor;
    private int lastAnchorEntityId = -1;
    private float postHitYawCurrent;
    private float postHitYawTarget;
    private float postHitPitchCurrent;
    private float postHitPitchTarget;
    private long postHitReleaseTime;
    private Vec3d anchorOffsetCurrent = Vec3d.ZERO;
    private double anchorOrbitPhase;
    private Vec3d anchorOffsetTarget = Vec3d.ZERO;
    public TraxSmoothMode() {
        super("SlothAC");
        this.postHitReleaseTime = 0L;
        this.anchorOrbitPhase = 0.0D;
        this.smoothedYawStep = 0.0F;
        this.smoothedPitchStep = 0.0F;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        long now = System.currentTimeMillis();

        updateReactionBias(now, yawDelta, pitchDelta);
        updateAttackBias(entity, now, yawDelta, pitchDelta);
        updatePostHitOffsets(now);
        Vec3d anchoredLookPoint = resolveAnchoredAimPoint(entity, targetAngle, vec3d, now);
        if (anchoredLookPoint != null && mc.player != null) {
            Angle anchoredAngle = AngleUtil.fromVec3d(anchoredLookPoint.subtract(mc.player.getEyePos()));
            yawDelta = MathHelper.wrapDegrees(anchoredAngle.getYaw() - currentAngle.getYaw());
            pitchDelta = MathHelper.wrapDegrees(anchoredAngle.getPitch() - currentAngle.getPitch());
        }

        boolean hasTrackedTarget = anchoredLookPoint != null && entity instanceof LivingEntity;
        Angle cameraAngle = getCameraAngle();
        if (!hasTrackedTarget) {
            yawDelta = MathHelper.wrapDegrees(cameraAngle.getYaw() - currentAngle.getYaw());
            pitchDelta = MathHelper.wrapDegrees(cameraAngle.getPitch() - currentAngle.getPitch());
            anchorOffsetTarget = anchorOffsetTarget.multiply(0.6D);
            anchorOffsetCurrent = anchorOffsetCurrent.multiply(0.6D);
            smoothedYawStep *= 0.6F;
            smoothedPitchStep *= 0.6F;
        }

        float yawTargetSpeed;
        float pitchTargetSpeed;
        if (hasTrackedTarget) {
            yawTargetSpeed = MathUtil.getRandom(DATASET_YAW_ABS_MEDIAN * 0.92F, DATASET_YAW_ABS_P90 * 0.98F);
            pitchTargetSpeed = MathUtil.getRandom(DATASET_PITCH_ABS_MEDIAN * 0.88F, DATASET_PITCH_ABS_P90 * 0.96F);
        } else {
            yawTargetSpeed = MathUtil.getRandom(DATASET_YAW_ABS_P75 * 0.45F, DATASET_YAW_ABS_P75 * 0.78F);
            pitchTargetSpeed = MathUtil.getRandom(DATASET_PITCH_ABS_P75 * 0.4F, DATASET_PITCH_ABS_P75 * 0.72F);
        }

        currentYawSpeed = MathHelper.lerp(hasTrackedTarget ? 0.28F : 0.2F, currentYawSpeed, yawTargetSpeed);
        currentPitchSpeed = MathHelper.lerp(hasTrackedTarget ? 0.22F : 0.18F, currentPitchSpeed, pitchTargetSpeed);

        float yawNormalized = MathHelper.clamp(Math.abs(yawDelta) / DATASET_YAW_ABS_P90, 0.0F, 1.0F);
        float pitchNormalized = MathHelper.clamp(Math.abs(pitchDelta) / DATASET_PITCH_ABS_P90, 0.0F, 1.0F);
        float yawAmplifier = MathHelper.lerp(hasTrackedTarget ? 0.82F : 0.62F, hasTrackedTarget ? 1.58F : 1.12F, yawNormalized);
        float pitchAmplifier = MathHelper.lerp(hasTrackedTarget ? 0.56F : 0.48F, hasTrackedTarget ? 1.18F : 0.96F, pitchNormalized);
        float yawLimit = Math.max(currentYawSpeed * yawAmplifier, MIN_YAW_LIMIT);
        float pitchLimit = Math.max(currentPitchSpeed * pitchAmplifier, MIN_PITCH_LIMIT);

        float yawInertia = MathUtil.getRandom(hasTrackedTarget ? 0.46F : 0.34F, hasTrackedTarget ? 0.64F : 0.5F);
        float pitchInertia = MathUtil.getRandom(hasTrackedTarget ? 0.32F : 0.24F, hasTrackedTarget ? 0.5F : 0.38F);
        yawVelocity = MathHelper.lerp(yawInertia, yawVelocity, yawDelta);
        pitchVelocity = MathHelper.lerp(pitchInertia, pitchVelocity, pitchDelta);

        float yawStepRaw = MathHelper.clamp(yawVelocity, -yawLimit, yawLimit);
        float pitchStepRaw = MathHelper.clamp(pitchVelocity, -pitchLimit, pitchLimit);

        if (Math.abs(yawDelta) > 0.6F && Math.abs(yawStepRaw) < MIN_YAW_STEP) {
            float yawSign = yawDelta == 0.0F ? (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F) : Math.signum(yawDelta);
            float enforced = MathHelper.clamp(MIN_YAW_STEP + MathUtil.getRandom(-0.75F, 0.75F), MIN_YAW_STEP * 0.65F, MIN_YAW_STEP * 1.7F);
            yawStepRaw = yawSign * Math.min(enforced, Math.abs(yawLimit));
        }

        if (Math.abs(pitchDelta) > 0.35F && Math.abs(pitchStepRaw) < MIN_PITCH_STEP) {
            float pitchSign = pitchDelta == 0.0F ? (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F) : Math.signum(pitchDelta);
            float enforced = MathHelper.clamp(MIN_PITCH_STEP + MathUtil.getRandom(-0.4F, 0.4F), MIN_PITCH_STEP * 0.6F, MIN_PITCH_STEP * 1.6F);
            pitchStepRaw = pitchSign * Math.min(enforced, Math.abs(pitchLimit));
        }

        float yawSaturation = yawLimit > 1.0E-3F ? MathHelper.clamp(Math.abs(yawStepRaw) / yawLimit, 0.0F, 1.0F) : 0.0F;
        float pitchSaturation = pitchLimit > 1.0E-3F ? MathHelper.clamp(Math.abs(pitchStepRaw) / pitchLimit, 0.0F, 1.0F) : 0.0F;
        float yawStepBlendBase = hasTrackedTarget ? 0.32F : 0.22F;
        float pitchStepBlendBase = hasTrackedTarget ? 0.34F : 0.24F;
        float yawStepBlend = MathHelper.clamp(yawStepBlendBase + yawSaturation * (hasTrackedTarget ? 0.42F : 0.3F), yawStepBlendBase, hasTrackedTarget ? 0.74F : 0.58F);
        float pitchStepBlend = MathHelper.clamp(pitchStepBlendBase + pitchSaturation * (hasTrackedTarget ? 0.28F : 0.2F), pitchStepBlendBase, hasTrackedTarget ? 0.62F : 0.5F);
        smoothedYawStep = MathHelper.lerp(yawStepBlend, smoothedYawStep, yawStepRaw);
        smoothedPitchStep = MathHelper.lerp(pitchStepBlend, smoothedPitchStep, pitchStepRaw);
        float yawStep = MathHelper.clamp(smoothedYawStep, -yawLimit, yawLimit);
        float pitchStep = MathHelper.clamp(smoothedPitchStep, -pitchLimit, pitchLimit);

        float yawOvershoot = 0.0F;
        if (hasTrackedTarget && Math.abs(yawDelta) < Math.min(DATASET_YAW_ABS_MEDIAN * 1.2F, yawLimit * 0.36F) && ThreadLocalRandom.current().nextFloat() < 0.24F) {
            float yawSign = yawDelta == 0.0F ? (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F) : Math.signum(yawDelta);
            yawOvershoot = yawSign * MathUtil.getRandom(0.08F, 0.28F);
        }

        float yawSmoothingBase = hasTrackedTarget ? 0.56F : 0.68F;
        float yawSmoothing = MathHelper.clamp(yawSmoothingBase + Math.abs(yawDelta) / (hasTrackedTarget ? 200.0F : 230.0F) * 0.18F, yawSmoothingBase, 0.92F);
        float yawBase = currentAngle.getYaw() + yawStep + yawOvershoot + reactionBias + attackYawBias * (hasTrackedTarget ? 0.1F : 0.06F);
        float yaw = MathHelper.lerp(yawSmoothing, currentAngle.getYaw(), yawBase)
                + postHitYawCurrent;

        float pitchStepFactor = hasTrackedTarget ? 0.56F : 0.48F;
        float pitchBiasFactor = hasTrackedTarget ? 0.58F : 0.5F;
        float pitchBase = currentAngle.getPitch() + pitchStep * pitchStepFactor + pitchReactionBias * pitchBiasFactor + attackPitchBias * (hasTrackedTarget ? 0.01F : 0.006F);
        float pitchSmoothingBase = hasTrackedTarget ? 0.52F : 0.64F;
        float pitchSmoothing = MathHelper.clamp(pitchSmoothingBase + Math.abs(pitchDelta) / (hasTrackedTarget ? 150.0F : 170.0F) * 0.16F, pitchSmoothingBase, hasTrackedTarget ? 0.72F : 0.78F);
        float desiredPitch = MathHelper.lerp(pitchSmoothing, currentAngle.getPitch(), pitchBase);
        float pitch = MathHelper.clamp(desiredPitch + postHitPitchCurrent, -89.8F, 90.0F);

        Angle result = new Angle(yaw, pitch);
        return result.adjustSensitivity();
    }

    private Angle getCameraAngle() {
        if (mc.player != null) {
            return new Angle(mc.player.getYaw(), mc.player.getPitch());
        }
        return Angle.DEFAULT;
    }

    private void updateReactionBias(long now, float yawDelta, float pitchDelta) {
        reactionBias = MathHelper.lerp(0.18F, reactionBias, MathHelper.clamp(yawDelta * 0.04F, -1.2F, 1.2F));
        pitchReactionBias = MathHelper.lerp(0.18F, pitchReactionBias, MathHelper.clamp(pitchDelta * 0.05F, -0.9F, 0.9F));
    }

    private void updateAttackBias(Entity entity, long now, float yawDelta, float pitchDelta) {
        attackYawBias *= 0.86F;
        attackPitchBias *= 0.84F;

        if (!(entity instanceof LivingEntity livingEntity)) {
            lastKnownHurtTime = 0;
            return;
        }

        int hurtTime = livingEntity.hurtTime;
        boolean newHit = hurtTime > 0 && (hurtTime > lastKnownHurtTime || now - lastHitTimestamp > 360L);
        if (newHit) {
            float movementBoost = 1.0F;
            if (mc.player != null) {
                Vec3d velocity = mc.player.getVelocity();
                double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                movementBoost += MathHelper.clamp((float) horizontal * 3.5F, 0.0F, 1.9F);
            }

            float yawImpulse = MathHelper.clamp(yawDelta * movementBoost * 0.28F, -3.5F, 3.5F);
            float pitchImpulse = MathHelper.clamp(pitchDelta * (0.28F + (movementBoost - 1.0F) * 0.18F) * 0.18F, -0.28F, 0.28F);

            attackYawBias = MathHelper.clamp(attackYawBias + yawImpulse, -6.75F, 6.75F);
            attackPitchBias = MathHelper.clamp(attackPitchBias + pitchImpulse, -3.6F, 3.6F);

            postHitYawTarget = MathHelper.clamp(yawImpulse * 0.8F, -3.5F, 3.5F);
            postHitPitchTarget = MathHelper.clamp(pitchImpulse * 1.2F, -0.35F, 0.35F);
            postHitReleaseTime = now + 260L;
            attackYawBias = MathHelper.clamp(attackYawBias, -6.75F, 6.75F);
            attackPitchBias = MathHelper.clamp(attackPitchBias, -3.6F, 3.6F);
            lastHitTimestamp = now;
        }

        lastKnownHurtTime = hurtTime;
    }

    private void updatePostHitOffsets(long now) {
        float chaseFactor = 0.1F;
        postHitYawCurrent = MathHelper.clamp(MathHelper.lerp(chaseFactor, postHitYawCurrent, postHitYawTarget), -5.0F, 5.0F);
        postHitPitchCurrent = MathHelper.clamp(MathHelper.lerp(chaseFactor, postHitPitchCurrent, postHitPitchTarget), -0.8F, 0.8F);

        if (now >= postHitReleaseTime) {
            postHitYawTarget = MathHelper.lerp(0.08F, postHitYawTarget, 0.0F);
            postHitPitchTarget = MathHelper.lerp(0.08F, postHitPitchTarget, 0.0F);
        }
    }

    private Vec3d resolveAnchoredAimPoint(Entity entity, Angle targetAngle, Vec3d existingPoint, long now) {
        if (!(entity instanceof LivingEntity living) || mc.player == null) {
            hasAnchor = false;
            return null;
        }

        if (lastAnchorEntityId != living.getId()) {
            hasAnchor = false;
            lastAnchorEntityId = living.getId();
        }

        Box box = living.getBoundingBox();
        double height = box.getLengthY();
        double neckBase = box.minY + height * 0.58D;
        double shoulderCap = box.minY + height * 0.72D;
        double playerEyeY = mc.player.getEyePos().y;
        double verticalDiff = playerEyeY - neckBase;
        double adaptiveLift = MathHelper.clamp(verticalDiff * 0.32D, -0.28D, 0.48D);
        double targetY = MathHelper.clamp(neckBase + adaptiveLift, box.minY + height * 0.47D, shoulderCap);

        Vec3d basePoint;
        if (existingPoint != null) {
            basePoint = new Vec3d(existingPoint.x, targetY, existingPoint.z);
        } else {
            Vec3d center = box.getCenter();
            basePoint = new Vec3d(center.x, targetY, center.z);
        }

        if (targetAngle != null) {
            Vec3d eyePos = living.getEyePos();
            Vec3d lookDir = targetAngle.toVector();
            Vec3d rayPoint = eyePos.add(lookDir.multiply(0.4D));
            basePoint = new Vec3d(rayPoint.x, targetY, rayPoint.z);
        }

        updateAnchorOffset(living, verticalDiff, now);
        basePoint = basePoint.add(anchorOffsetCurrent);

        Vec3d finalPoint = basePoint;
        if (!hasAnchor) {
            lastAnchoredPoint = finalPoint;
            hasAnchor = true;
        } else {
            double targetSpeed = horizontalSpeed(living.getVelocity());
            double playerSpeed = mc.player != null ? horizontalSpeed(mc.player.getVelocity()) : 0.0D;
            double trackingBoost = MathHelper.clamp((targetSpeed + playerSpeed) * 0.35D, 0.0D, 0.35D);
            double smoothing = MathHelper.clamp(0.48F + Math.abs(verticalDiff) * 0.05F + trackingBoost, 0.48F, 0.86F);
            lastAnchoredPoint = lastAnchoredPoint.lerp(finalPoint, smoothing);
        }

        return lastAnchoredPoint;
    }

    private void updateAnchorOffset(LivingEntity living, double verticalDiff, long now) {
        Box box = living.getBoundingBox();
        double width = box.getLengthX();
        double depth = box.getLengthZ();
        double horizontalCap = Math.max(0.24D, width * 0.52D);
        double depthCap = Math.max(0.24D, depth * 0.58D);
        double horizontalRadius = MathHelper.clamp(width * 0.48D, 0.24D, horizontalCap);
        double depthRadius = MathHelper.clamp(depth * 0.45D, 0.24D, depthCap);

        double targetSpeed = horizontalSpeed(living.getVelocity());
        double playerSpeed = mc.player != null ? horizontalSpeed(mc.player.getVelocity()) : 0.0D;
        double combinedSpeed = MathHelper.clamp((targetSpeed + playerSpeed) * 0.72D, 0.0D, 1.5D);



        anchorOrbitPhase += 0.06D + combinedSpeed * 0.34D;
        double travelScale = MathHelper.clamp(0.58D + combinedSpeed * 0.32D, 0.58D, 1.0D);
        double orbitX = Math.cos(anchorOrbitPhase) * horizontalRadius * travelScale;
        double orbitZ = Math.sin(anchorOrbitPhase) * depthRadius * travelScale;

        Vec3d dynamicTarget = anchorOffsetTarget.add(orbitX, 0.0D, orbitZ);
        double clampedX = MathHelper.clamp(dynamicTarget.x, -horizontalCap, horizontalCap);
        double clampedZ = MathHelper.clamp(dynamicTarget.z, -depthCap, depthCap);
        dynamicTarget = new Vec3d(clampedX, 0.0D, clampedZ);

        double blend = MathHelper.clamp(0.28D + combinedSpeed * 0.42D + Math.abs(verticalDiff) * 0.05D, 0.28D, 0.65D);
        anchorOffsetCurrent = anchorOffsetCurrent.lerp(dynamicTarget, blend);
    }

    private static double horizontalSpeed(Vec3d velocity) {
        double x = velocity.x;
        double z = velocity.z;
        return Math.sqrt(x * x + z * z);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.0f,0.0f,0.0f);
    }
}
