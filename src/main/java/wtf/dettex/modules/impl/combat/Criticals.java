package wtf.dettex.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.player.AttackEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Criticals extends Module {
    public static Criticals getInstance() {
        return Instance.get(Criticals.class);
    }

    SelectSetting mode = new SelectSetting("Mode", "Select bypass mode").value("Grim");

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);
        setup(mode);
    }

    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onAttack(AttackEvent e) {
        if (mc.player.isTouchingWater()) return;
        if (mode.isSelected("Grim")) {
            if (!mc.player.isOnGround() && mc.player.fallDistance == 0) {
                PlayerIntersectionUtil.grimSuperBypass$$$(-(mc.player.fallDistance = MathUtil.getRandom(1e-5F, 1e-4F)), RotationController.INSTANCE.getRotation().random(1e-3F));
            }
        }
    }
}