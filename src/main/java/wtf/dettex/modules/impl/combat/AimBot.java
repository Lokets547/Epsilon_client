package wtf.dettex.modules.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.api.repository.friend.FriendUtils;

import java.util.Comparator;
import java.util.List;

public class AimBot extends Module {

    private final ValueSetting aimSpeed = new ValueSetting("Aim Speed", "Скорость наведения")
            .setValue(4.0f)
            .range(0.5f, 15.0f);

    private final ValueSetting smoothness = new ValueSetting("Smoothness", "Плавность изменения углов")
            .setValue(0.5f)
            .range(0.05f, 1.0f);

    private final ValueSetting fov = new ValueSetting("Aim FOV", "Угол для поиска цели")
            .setValue(70.0f)
            .range(10.0f, 180.0f);

    private final ValueSetting range = new ValueSetting("Range", "Максимальная дистанция")
            .setValue(6.0f)
            .range(2.0f, 12.0f);

    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", "Не целиться в друзей")
            .setValue(true);

    private final BooleanSetting requireVisible = new BooleanSetting("Require Visible", "Только если цель видна")
            .setValue(true);

    public AimBot() {
        super("AimBot", ModuleCategory.COMBAT);
        setup(aimSpeed, smoothness, fov, range, ignoreFriends, requireVisible);
    }

    @EventHandler

    public void onTick(TickEvent event) {
        if (Module.fullNullCheck()) {
            return;
        }

        LivingEntity target = findTarget();
        if (target != null) {
            rotateTowards(target);
        }
    }

    private LivingEntity findTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d viewDir = mc.player.getRotationVecClient().normalize();
        double maxDistance = range.getValue();
        float fovHalfAngle = fov.getValue() * 0.5f;

        Box searchBox = mc.player.getBoundingBox().expand(maxDistance);
        List<LivingEntity> candidates = mc.world.getEntitiesByClass(LivingEntity.class, searchBox, this::isValidTarget);

        return candidates.stream()
                .map(entity -> new Candidate(entity, eyePos, viewDir))
                .filter(candidate -> candidate.angle <= fovHalfAngle)
                .min(Comparator.comparingDouble(Candidate::score))
                .map(candidate -> candidate.entity)
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity == mc.player) {
            return false;
        }
        if (ignoreFriends.isValue() && FriendUtils.isFriend(entity.getName().getString())) {
            return false;
        }
        if (requireVisible.isValue() && mc.player != null && !mc.player.canSee(entity)) {
            return false;
        }
        return mc.player != null && mc.player.squaredDistanceTo(entity) <= range.getValue() * range.getValue();
    }

    private void rotateTowards(LivingEntity target) {
        if (mc.player == null) {
            return;
        }

        Vec3d delta = target.getEyePos().subtract(mc.player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float desiredYaw = (float) (MathHelper.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0f;
        float desiredPitch = (float) (-(MathHelper.atan2(delta.y, horizontal) * (180.0 / Math.PI)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(desiredYaw - currentYaw);
        float pitchDiff = MathHelper.wrapDegrees(desiredPitch - currentPitch);

        float maxStep = aimSpeed.getValue();
        yawDiff = MathHelper.clamp(yawDiff, -maxStep, maxStep);
        pitchDiff = MathHelper.clamp(pitchDiff, -maxStep, maxStep);

        float coupling = 0.18f;
        float mixedYaw = yawDiff + pitchDiff * coupling;
        float mixedPitch = pitchDiff + yawDiff * coupling;

        float smoothFactor = smoothness.getValue();
        float newYaw = currentYaw + mixedYaw * smoothFactor;
        float newPitch = MathHelper.clamp(currentPitch + mixedPitch * smoothFactor, -89.9f, 89.9f);

        if (!Float.isFinite(newYaw) || !Float.isFinite(newPitch)) {
            return;
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }

    private record Candidate(LivingEntity entity, double angle, double distance) {
        Candidate(LivingEntity entity, Vec3d eyePos, Vec3d viewDir) {
            this(entity,
                    computeAngle(entity, eyePos, viewDir),
                    eyePos.distanceTo(entity.getEyePos()));
        }

        private static double computeAngle(LivingEntity entity, Vec3d eyePos, Vec3d viewDir) {
            Vec3d toTarget = entity.getEyePos().subtract(eyePos).normalize();
            double dot = MathHelper.clamp(viewDir.dotProduct(toTarget), -1.0, 1.0);
            double angle = Math.toDegrees(Math.acos(dot));
            return Double.isNaN(angle) ? 180.0 : angle;
        }

        private double score() {
            return angle * 0.8 + distance * 0.2;
        }
    }
}

