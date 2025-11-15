package wtf.dettex.modules.impl.combat.killaura.rotation;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.modules.impl.combat.killaura.rotation.angle.AngleSmoothMode;
import wtf.dettex.modules.impl.combat.killaura.rotation.angle.ReallyWorldSmoothMode;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RotationConfig {
    public static RotationConfig DEFAULT = new RotationConfig(new ReallyWorldSmoothMode(), true, true);
    boolean moveCorrection, freeCorrection;
    AngleSmoothMode angleSmooth;
    int resetThreshold = 3;

    public RotationConfig(boolean moveCorrection, boolean freeCorrection) {
        this(new ReallyWorldSmoothMode(), moveCorrection, freeCorrection);
    }

    public RotationConfig(boolean moveCorrection) {
        this(new ReallyWorldSmoothMode(), moveCorrection, true);
    }

    public RotationConfig(AngleSmoothMode angleSmooth, boolean moveCorrection, boolean freeCorrection) {
        this.angleSmooth = angleSmooth;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = freeCorrection;
    }
    @Native(type = Native.Type.VMProtectBeginUltra)
    public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, int reset) {
        return new RotationPlan(angle, vec, entity, angleSmooth, reset, resetThreshold, moveCorrection, freeCorrection);
    }

    public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
        return new RotationPlan(angle, vec, entity, angleSmooth, 1, resetThreshold, moveCorrection, freeCorrection);
    }
}