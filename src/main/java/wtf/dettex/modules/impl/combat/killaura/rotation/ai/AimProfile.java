package wtf.dettex.modules.impl.combat.killaura.rotation.ai;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import wtf.dettex.common.QuickImports;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AimProfile implements QuickImports {
    private static final Identifier PROFILE_ID = Identifier.of("ai/aim_profile.json");
    private static final float EPSILON = 1.0e-4f;
    private static AimProfile INSTANCE;

    @SerializedName("samples")
    private int samples;
    @SerializedName("domain")
    private float[] domain;
    @SerializedName("max_abs_yaw")
    private float maxAbsYaw;
    @SerializedName("max_abs_pitch")
    private float maxAbsPitch;
    @SerializedName("yaw_limit")
    private float[] yawLimit;
    @SerializedName("pitch_limit")
    private float[] pitchLimit;
    @SerializedName("yaw_speed")
    private float[] yawSpeed;
    @SerializedName("pitch_speed")
    private float[] pitchSpeed;
    @SerializedName("yaw_inertia")
    private float[] yawInertia;
    @SerializedName("pitch_inertia")
    private float[] pitchInertia;
    @SerializedName("yaw_blend")
    private float[] yawBlend;
    @SerializedName("pitch_blend")
    private float[] pitchBlend;
    @SerializedName("yaw_min_step")
    private float[] yawMinStep;
    @SerializedName("pitch_min_step")
    private float[] pitchMinStep;

    public static AimProfile getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadProfile();
        }
        return INSTANCE;
    }

    public float normalizeYaw(float absoluteDelta) {
        return normalize(absoluteDelta, maxAbsYaw);
    }

    public float normalizePitch(float absoluteDelta) {
        return normalize(absoluteDelta, maxAbsPitch);
    }

    public float sampleYawLimit(float factor) {
        return sampleCurve(yawLimit, factor);
    }

    public float samplePitchLimit(float factor) {
        return sampleCurve(pitchLimit, factor);
    }

    public float sampleYawSpeed(float factor) {
        return sampleCurve(yawSpeed, factor);
    }

    public float samplePitchSpeed(float factor) {
        return sampleCurve(pitchSpeed, factor);
    }

    public float sampleYawInertia(float factor) {
        return sampleCurve(yawInertia, factor);
    }

    public float samplePitchInertia(float factor) {
        return sampleCurve(pitchInertia, factor);
    }

    public float sampleYawBlend(float factor) {
        return sampleCurve(yawBlend, factor);
    }

    public float samplePitchBlend(float factor) {
        return sampleCurve(pitchBlend, factor);
    }

    public float sampleYawMinStep(float factor) {
        return sampleCurve(yawMinStep, factor);
    }

    public float samplePitchMinStep(float factor) {
        return sampleCurve(pitchMinStep, factor);
    }

    private static AimProfile loadProfile() {
        try {
            if (mc == null) {
                return createFallback();
            }
            Optional<Resource> resourceOptional = mc.getResourceManager().getResource(PROFILE_ID);
            if (resourceOptional.isEmpty()) {
                return createFallback();
            }
            Resource resource = resourceOptional.get();
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                AimProfile profile = gson.fromJson(reader, AimProfile.class);
                if (profile == null) {
                    return createFallback();
                }
                profile.ensureIntegrity();
                return profile;
            }
        } catch (Exception ignored) {
        }
        return createFallback();
    }

    private void ensureIntegrity() {
        maxAbsYaw = Math.max(maxAbsYaw, 1.0f);
        maxAbsPitch = Math.max(maxAbsPitch, 1.0f);

        yawLimit = ensureArray(yawLimit, 20.0f, 16);
        pitchLimit = ensureArray(pitchLimit, 18.0f, yawLimit.length);
        yawSpeed = ensureArray(yawSpeed, 12.0f, yawLimit.length);
        pitchSpeed = ensureArray(pitchSpeed, 10.0f, yawLimit.length);
        yawInertia = ensureArray(yawInertia, 0.32f, yawLimit.length);
        pitchInertia = ensureArray(pitchInertia, 0.33f, yawLimit.length);
        yawBlend = ensureArray(yawBlend, 0.28f, yawLimit.length);
        pitchBlend = ensureArray(pitchBlend, 0.36f, yawLimit.length);
        yawMinStep = ensureArray(yawMinStep, 0.8f, yawLimit.length);
        pitchMinStep = ensureArray(pitchMinStep, 0.7f, yawLimit.length);

        if (domain == null || domain.length != yawLimit.length) {
            domain = new float[yawLimit.length];
            if (domain.length == 1) {
                domain[0] = 0.0f;
            } else {
                float step = 1.0f / (domain.length - 1);
                for (int i = 0; i < domain.length; i++) {
                    domain[i] = MathHelper.clamp(i * step, 0.0f, 1.0f);
                }
            }
        }
    }

    private float[] ensureArray(float[] array, float filler, int expectedLength) {
        if (array == null || array.length == 0) {
            array = new float[expectedLength];
            for (int i = 0; i < expectedLength; i++) {
                array[i] = filler;
            }
            return array;
        }
        if (array.length != expectedLength) {
            float[] resized = new float[expectedLength];
            for (int i = 0; i < expectedLength; i++) {
                float factor = expectedLength == 1 ? 0.0f : (float) i / (expectedLength - 1);
                resized[i] = sampleFrom(array, null, factor);
            }
            return resized;
        }
        return array;
    }

    private float normalize(float absoluteDelta, float maxAbs) {
        float clampedMax = Math.max(maxAbs, 1.0f);
        return MathHelper.clamp(absoluteDelta / clampedMax, 0.0f, 1.0f);
    }

    private float sampleCurve(float[] curve, float factor) {
        return sampleFrom(curve, domain, factor);
    }

    private float sampleFrom(float[] curve, float[] domainValues, float factor) {
        if (curve == null || curve.length == 0) {
            return 0.0f;
        }
        float clampedFactor = MathHelper.clamp(factor, 0.0f, 1.0f);
        int length = curve.length;
        if (length == 1) {
            return curve[0];
        }

        if (domainValues == null || domainValues.length != length) {
            float scaledIndex = clampedFactor * (length - 1);
            int index = MathHelper.floor(scaledIndex);
            int nextIndex = Math.min(index + 1, length - 1);
            float fraction = scaledIndex - index;
            return MathHelper.lerp(fraction, curve[index], curve[nextIndex]);
        }

        float previousDomain = domainValues[0];
        for (int i = 1; i < length; i++) {
            float currentDomain = domainValues[i];
            if (clampedFactor <= currentDomain + EPSILON || i == length - 1) {
                float span = Math.max(currentDomain - previousDomain, EPSILON);
                float fraction = MathHelper.clamp((clampedFactor - previousDomain) / span, 0.0f, 1.0f);
                return MathHelper.lerp(fraction, curve[i - 1], curve[i]);
            }
            previousDomain = currentDomain;
        }
        return curve[length - 1];
    }

    private static AimProfile createFallback() {
        AimProfile fallback = new AimProfile();
        fallback.samples = 0;
        fallback.domain = new float[]{0.0f, 1.0f};
        fallback.maxAbsYaw = 180.0f;
        fallback.maxAbsPitch = 90.0f;
        fallback.yawLimit = new float[]{40.0f, 60.0f};
        fallback.pitchLimit = new float[]{28.0f, 42.0f};
        fallback.yawSpeed = new float[]{20.0f, 24.0f};
        fallback.pitchSpeed = new float[]{12.0f, 16.0f};
        fallback.yawInertia = new float[]{0.28f, 0.34f};
        fallback.pitchInertia = new float[]{0.26f, 0.32f};
        fallback.yawBlend = new float[]{0.24f, 0.3f};
        fallback.pitchBlend = new float[]{0.32f, 0.38f};
        fallback.yawMinStep = new float[]{0.8f, 1.2f};
        fallback.pitchMinStep = new float[]{0.6f, 0.9f};
        return fallback;
    }
}

