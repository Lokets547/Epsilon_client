package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.ai.AimProfile;

import java.util.concurrent.ThreadLocalRandom;

public class MandaSmoothMode extends AngleSmoothMode {
    private static final AimProfile PROFILE = AimProfile.getInstance();

    private float currentYawSpeed = PROFILE.sampleYawSpeed(0.5f);
    private float currentPitchSpeed = PROFILE.samplePitchSpeed(0.5f);
    private float yawLimitState = PROFILE.sampleYawLimit(0.5f);
    private float pitchLimitState = PROFILE.samplePitchLimit(0.5f);
    private float yawMinStepState = PROFILE.sampleYawMinStep(0.5f);
    private float pitchMinStepState = PROFILE.samplePitchMinStep(0.5f);
    private float yawBlendState = PROFILE.sampleYawBlend(0.5f);
    private float pitchBlendState = PROFILE.samplePitchBlend(0.5f);
    private float yawInertiaState = PROFILE.sampleYawInertia(0.5f);
    private float pitchInertiaState = PROFILE.samplePitchInertia(0.5f);
    private float yawSmoothingState = 0.58F;
    private float pitchSmoothingState = 0.54F;
    private float yawVelocity;
    private float pitchVelocity;
    private float smoothedYawStep;
    private float smoothedPitchStep;

    private Vec3d anchorOffsetCurrent = Vec3d.ZERO;
    private Vec3d anchorOffsetTarget = Vec3d.ZERO;
    private Vec3d lastAnchoredPoint = Vec3d.ZERO;
    private boolean hasAnchor;
    private int lastAnchorEntityId = -1;
    private double anchorOrbitPhase;

    public MandaSmoothMode() {
        super("ReallyWorld");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        long now = System.currentTimeMillis();
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
            anchorOffsetTarget = anchorOffsetTarget.multiply(0.65D);
            anchorOffsetCurrent = anchorOffsetCurrent.multiply(0.65D);
            smoothedYawStep *= 0.62F;
            smoothedPitchStep *= 0.62F;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        float yawAbs = Math.abs(yawDelta);
        float pitchAbs = Math.abs(pitchDelta);

        float yawFactorBase = PROFILE.normalizeYaw(yawAbs);
        float pitchFactorBase = PROFILE.normalizePitch(pitchAbs);

        float randomWeight = hasTrackedTarget ? 0.32F : 0.56F;
        float yawSampleFactor = clamp01(mix(yawFactorBase, random.nextFloat(), randomWeight));
        float pitchSampleFactor = clamp01(mix(pitchFactorBase, random.nextFloat(), randomWeight));
        float crossFactor = clamp01((yawSampleFactor + pitchSampleFactor) * 0.5F);

        float yawSpeedTarget = PROFILE.sampleYawSpeed(yawSampleFactor);
        float pitchSpeedTarget = PROFILE.samplePitchSpeed(pitchSampleFactor);
        float tensionBoost = hasTrackedTarget ? 2.8F + yawFactorBase * 1.25F : 1.6F + yawFactorBase * 1.05F;
        float pitchBoost = hasTrackedTarget ? 2.4F + pitchFactorBase * 1.15F : 1.5F + pitchFactorBase * 0.95F;
        yawSpeedTarget = Math.max(yawSpeedTarget, 24.0F + yawAbs * 0.35F) * varyScalar(random, tensionBoost, 0.42F);
        pitchSpeedTarget = Math.max(pitchSpeedTarget, 18.0F + pitchAbs * 0.3F) * varyScalar(random, pitchBoost, 0.4F);

        currentYawSpeed = MathHelper.lerp(hasTrackedTarget ? 0.88F : 0.74F, currentYawSpeed, yawSpeedTarget);
        currentPitchSpeed = MathHelper.lerp(hasTrackedTarget ? 0.82F : 0.68F, currentPitchSpeed, pitchSpeedTarget);

        float yawLimitTarget = Math.max(PROFILE.sampleYawLimit(yawSampleFactor), currentYawSpeed * (2.35F + yawFactorBase * 1.05F));
        float pitchLimitTarget = Math.max(PROFILE.samplePitchLimit(pitchSampleFactor), currentPitchSpeed * (2.18F + pitchFactorBase * 0.99F));
        yawLimitTarget *= varyScalar(random, 1.3F + crossFactor * 0.64F, 0.36F);
        pitchLimitTarget *= varyScalar(random, 1.24F + crossFactor * 0.66F, 0.38F);

        yawLimitState = MathHelper.lerp(0.6F, yawLimitState, yawLimitTarget);
        pitchLimitState = MathHelper.lerp(0.6F, pitchLimitState, pitchLimitTarget);
        float yawLimit = MathHelper.clamp(yawLimitState, 8.0F, 240.0F);
        float pitchLimit = MathHelper.clamp(pitchLimitState, 6.0F, 160.0F);

        float yawMinTarget = Math.max(PROFILE.sampleYawMinStep(yawSampleFactor), currentYawSpeed * 0.34F) * varyScalar(random, 1.5F + yawFactorBase * 0.62F, 0.36F);
        float pitchMinTarget = Math.max(PROFILE.samplePitchMinStep(pitchSampleFactor), currentPitchSpeed * 0.3F) * varyScalar(random, 1.4F + pitchFactorBase * 0.6F, 0.34F);
        yawMinStepState = MathHelper.lerp(0.64F, yawMinStepState, yawMinTarget);
        pitchMinStepState = MathHelper.lerp(0.64F, pitchMinStepState, pitchMinTarget);
        float minYawStep = MathHelper.clamp(yawMinStepState, 0.92F, yawLimit * 0.96F);
        float minPitchStep = MathHelper.clamp(pitchMinStepState, 0.72F, pitchLimit * 0.94F);

        float yawBlendTarget = MathHelper.clamp(PROFILE.sampleYawBlend(mix(yawSampleFactor, crossFactor, 0.42F)) * varyScalar(random, 0.85F, 0.2F), 0.03F, 0.7F);
        float pitchBlendTarget = MathHelper.clamp(PROFILE.samplePitchBlend(mix(pitchSampleFactor, crossFactor, 0.38F)) * varyScalar(random, 0.85F, 0.2F), 0.03F, 0.68F);
        yawBlendState = MathHelper.lerp(0.18F, yawBlendState, yawBlendTarget);
        pitchBlendState = MathHelper.lerp(0.18F, pitchBlendState, pitchBlendTarget);

        float yawInertiaTarget = MathHelper.clamp(PROFILE.sampleYawInertia(yawSampleFactor) * varyScalar(random, 0.75F, 0.24F), 0.12F, 0.46F);
        float pitchInertiaTarget = MathHelper.clamp(PROFILE.samplePitchInertia(pitchSampleFactor) * varyScalar(random, 0.75F, 0.24F), 0.12F, 0.42F);
        yawInertiaState = MathHelper.lerp(0.12F, yawInertiaState, yawInertiaTarget);
        pitchInertiaState = MathHelper.lerp(0.12F, pitchInertiaState, pitchInertiaTarget);

        float yawSmoothingTarget = MathHelper.clamp((hasTrackedTarget ? 0.16F : 0.28F) + yawFactorBase * 0.16F, 0.04F, hasTrackedTarget ? 0.34F : 0.28F);
        float pitchSmoothingTarget = MathHelper.clamp((hasTrackedTarget ? 0.12F : 0.22F) + pitchFactorBase * 0.14F, 0.04F, hasTrackedTarget ? 0.3F : 0.24F);
        yawSmoothingTarget *= varyScalar(random, 0.82F, 0.2F);
        pitchSmoothingTarget *= varyScalar(random, 0.82F, 0.2F);
        yawSmoothingState = MathHelper.lerp(0.12F, yawSmoothingState, yawSmoothingTarget);
        pitchSmoothingState = MathHelper.lerp(0.12F, pitchSmoothingState, pitchSmoothingTarget);

        yawVelocity = MathHelper.lerp(yawInertiaState, yawVelocity, yawDelta);
        pitchVelocity = MathHelper.lerp(pitchInertiaState, pitchVelocity, pitchDelta);

        float yawStepRaw = MathHelper.clamp(yawVelocity, -yawLimit, yawLimit);
        float pitchStepRaw = MathHelper.clamp(pitchVelocity, -pitchLimit, pitchLimit);

        if (yawAbs > minYawStep && Math.abs(yawStepRaw) < minYawStep) {
            float enforced = MathHelper.clamp(minYawStep + MathUtil.getRandom(-0.5F, 0.5F), minYawStep * 0.7F, minYawStep * 1.6F);
            float yawSign = yawDelta == 0.0F ? (random.nextBoolean() ? 1.0F : -1.0F) : Math.signum(yawDelta);
            yawStepRaw = MathHelper.clamp(yawSign * enforced, -yawLimit, yawLimit);
        }

        if (pitchAbs > minPitchStep && Math.abs(pitchStepRaw) < minPitchStep) {
            float enforced = MathHelper.clamp(minPitchStep + MathUtil.getRandom(-0.38F, 0.38F), minPitchStep * 0.7F, minPitchStep * 1.42F);
            float pitchSign = pitchDelta == 0.0F ? (random.nextBoolean() ? 1.0F : -1.0F) : Math.signum(pitchDelta);
            pitchStepRaw = MathHelper.clamp(pitchSign * enforced, -pitchLimit, pitchLimit);
        }

        float yawBlend = MathHelper.clamp(yawBlendState, 0.05F, 0.82F);
        float pitchBlend = MathHelper.clamp(pitchBlendState, 0.05F, 0.78F);
        smoothedYawStep = MathHelper.lerp(yawBlend, smoothedYawStep, yawStepRaw);
        smoothedPitchStep = MathHelper.lerp(pitchBlend, smoothedPitchStep, pitchStepRaw);

        float yawStep = MathHelper.clamp(smoothedYawStep, -yawLimit, yawLimit);
        float pitchStep = MathHelper.clamp(smoothedPitchStep, -pitchLimit, pitchLimit);

        if (hasTrackedTarget && Math.abs(yawDelta) < yawLimit * 0.6F) {
            yawStep += MathUtil.getRandom(-yawLimit * 0.18F, yawLimit * 0.18F) * (0.52F + crossFactor * 0.56F);
        }

        if (hasTrackedTarget && Math.abs(pitchDelta) < pitchLimit * 0.55F) {
            pitchStep += MathUtil.getRandom(-pitchLimit * 0.14F, pitchLimit * 0.14F) * (0.44F + crossFactor * 0.52F);
        }

        float yawSmoothing = MathHelper.clamp(yawSmoothingState, 0.08F, hasTrackedTarget ? 0.7F : 0.58F);
        float pitchSmoothing = MathHelper.clamp(pitchSmoothingState, 0.08F, hasTrackedTarget ? 0.62F : 0.52F);

        float yawBase = currentAngle.getYaw() + yawStep;
        float pitchBase = currentAngle.getPitch() + pitchStep;

        float yaw = MathHelper.lerp(yawSmoothing, currentAngle.getYaw(), yawBase);
        float pitch = MathHelper.lerp(pitchSmoothing, currentAngle.getPitch(), pitchBase + MathUtil.getRandom(-0.12F, 0.12F));
        pitch = MathHelper.clamp(pitch, -89.8F, 90.0F);

        Angle result = new Angle(yaw, pitch);
        return result.adjustSensitivity();
    }

    private Angle getCameraAngle() {
        if (mc.player != null) {
            return new Angle(mc.player.getYaw(), mc.player.getPitch());
        }
        return Angle.DEFAULT;
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
            Vec3d rayPoint = eyePos.add(lookDir.multiply(0.35D));
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
            double playerSpeed = horizontalSpeed(mc.player.getVelocity());
            double trackingBoost = MathHelper.clamp((targetSpeed + playerSpeed) * 0.28D, 0.0D, 0.32D);
            double smoothing = MathHelper.clamp(0.48F + Math.abs(verticalDiff) * 0.05F + trackingBoost, 0.48F, 0.84F);
            lastAnchoredPoint = lastAnchoredPoint.lerp(finalPoint, smoothing);
        }

        return lastAnchoredPoint;
    }

    private void updateAnchorOffset(LivingEntity living, double verticalDiff, long now) {
        Box box = living.getBoundingBox();
        double width = box.getLengthX();
        double depth = box.getLengthZ();
        double horizontalCap = Math.max(0.24D, width * 0.48D);
        double depthCap = Math.max(0.24D, depth * 0.55D);
        double horizontalRadius = MathHelper.clamp(width * 0.42D, 0.2D, horizontalCap);
        double depthRadius = MathHelper.clamp(depth * 0.4D, 0.2D, depthCap);

        double targetSpeed = horizontalSpeed(living.getVelocity());
        double playerSpeed = mc.player != null ? horizontalSpeed(mc.player.getVelocity()) : 0.0D;
        double combinedSpeed = MathHelper.clamp((targetSpeed + playerSpeed) * 0.64D, 0.0D, 1.2D);

        anchorOrbitPhase += 0.055D + combinedSpeed * 0.28D;
        double travelScale = MathHelper.clamp(0.56D + combinedSpeed * 0.28D, 0.56D, 0.94D);
        double orbitX = Math.cos(anchorOrbitPhase) * horizontalRadius * travelScale;
        double orbitZ = Math.sin(anchorOrbitPhase) * depthRadius * travelScale;

        Vec3d dynamicTarget = anchorOffsetTarget.add(orbitX, 0.0D, orbitZ);
        double clampedX = MathHelper.clamp(dynamicTarget.x, -horizontalCap, horizontalCap);
        double clampedZ = MathHelper.clamp(dynamicTarget.z, -depthCap, depthCap);
        dynamicTarget = new Vec3d(clampedX, 0.0D, clampedZ);

        double blend = MathHelper.clamp(0.26D + combinedSpeed * 0.34D + Math.abs(verticalDiff) * 0.04D, 0.26D, 0.6D);
        anchorOffsetCurrent = anchorOffsetCurrent.lerp(dynamicTarget, blend);
    }

    private static double horizontalSpeed(Vec3d velocity) {
        double x = velocity.x;
        double z = velocity.z;
        return Math.sqrt(x * x + z * z);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private static float mix(float base, float randomValue, float weight) {
        float clampedWeight = MathHelper.clamp(weight, 0.0F, 1.0F);
        return base * (1.0F - clampedWeight) + randomValue * clampedWeight;
    }

    private static float clamp01(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }

    private static float varyScalar(ThreadLocalRandom random, float base, float spread) {
        if (spread <= 0.0F) {
            return base;
        }
        float variation = (float) random.nextDouble(-spread, spread);
        return Math.max(0.0F, base + variation);
    }
}
