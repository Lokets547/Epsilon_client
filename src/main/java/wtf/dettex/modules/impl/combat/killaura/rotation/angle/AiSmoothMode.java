package wtf.dettex.modules.impl.combat.killaura.rotation.angle;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.api.ai.deeplearning.DeepLearningManager;
import wtf.dettex.api.ai.training.DatasetLogger;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;

public class AiSmoothMode extends AngleSmoothMode {

    private final DeepLearningManager dl;
    private Angle lerpTarget = Angle.DEFAULT;

    public AiSmoothMode(DeepLearningManager dl) {
        super("AI");
        this.dl = dl;
    }

    
    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        // Обновляем цель-лерп при резком смене направления
        Angle deltaToLerpTarget = AngleUtil.calculateDelta(currentAngle, lerpTarget);
        Angle deltaToTarget = AngleUtil.calculateDelta(currentAngle, targetAngle);
        if (Math.abs(deltaToTarget.getYaw() - deltaToLerpTarget.getYaw()) > 80f) {
            lerpTarget = targetAngle;
        }

        // Предыдущая дельта (предыдущий -> текущий)
        Angle prev = RotationController.INSTANCE.getPreviousRotation();
        Angle curr = RotationController.INSTANCE.getRotation();
        Angle prevDelta = AngleUtil.calculateDelta(prev, curr);

        // Пересчитаем дельту до актуальной цели-лерпа
        deltaToLerpTarget = AngleUtil.calculateDelta(currentAngle, lerpTarget);

        float[] input = new float[]{
                prevDelta.getYaw(),
                prevDelta.getPitch(),
                deltaToLerpTarget.getYaw(),
                deltaToLerpTarget.getPitch()
        };

        float outYaw = 0f, outPitch = 0f;
        try {
            float[] out = dl.getSlowModel().predict(input);
            if (out != null && out.length >= 2) {
                outYaw = out[0];
                outPitch = out[1];
            }
        } catch (Exception ignored) {
        }

        Angle next = new Angle(currentAngle.getYaw() + outYaw, currentAngle.getPitch() + outPitch);

        // Dataset logging when enabled in Aura
        try {
            Aura aura = Aura.getInstance();
            if (aura != null) {
                // Teacher step (ReallyWorld) as ground truth
                ReallyWorldSmoothMode teacher = new ReallyWorldSmoothMode();
                Angle teacherNext = teacher.limitAngleChange(currentAngle, targetAngle, vec3d, entity);
                float labelYaw = teacherNext.getYaw() - currentAngle.getYaw();
                float labelPitch = teacherNext.getPitch() - currentAngle.getPitch();
                DatasetLogger.append(prevDelta.getYaw(), prevDelta.getPitch(),
                        deltaToLerpTarget.getYaw(), deltaToLerpTarget.getPitch(),
                        labelYaw, labelPitch);
            }
        } catch (Throwable ignored) {
        }

        // Если приблизились к цели-лерпа, подтягиваем цель к реальной
        if (Math.hypot(deltaToLerpTarget.getYaw(), deltaToLerpTarget.getPitch()) < 10f) {
            lerpTarget = targetAngle;
        }

        return next;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

