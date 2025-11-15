package wtf.dettex.modules.impl.combat.killaura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.QuickImports;
import wtf.dettex.event.impl.item.UsingItemEvent;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;

import java.util.List;

@Getter
public class AttackPerpetrator implements QuickImports {
    AttackHandler attackHandler = new AttackHandler();

    public void tick() {
        attackHandler.tick();
    }

    public void onPacket(PacketEvent e) {
        attackHandler.onPacket(e);
    }

    public void onUsingItem(UsingItemEvent e) {
        attackHandler.onUsingItem(e);
    }

    public void performAttack(AttackPerpetratorConfigurable configurable) {
        attackHandler.handleAttack(configurable);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class AttackPerpetratorConfigurable {
        LivingEntity target;
        Angle angle;
        float maximumRange;
        boolean onlyCritical, shouldBreakShield, shouldUnPressShield, useDynamicCooldown, eatAndAttack, onlySpaceCrits;
        Box box;
        SelectSetting aimMode;

        public AttackPerpetratorConfigurable(LivingEntity target, Angle angle, float maximumRange, List<String> options, SelectSetting aimMode, Box box, boolean onlySpaceCrits) {
            this.target = target;
            this.angle = angle;
            this.maximumRange = maximumRange;
            this.onlyCritical = options.contains("Only Critical");
            this.shouldBreakShield = options.contains("Break Shield");
            this.shouldUnPressShield = options.contains("UnPress Shield");
            this.useDynamicCooldown = options.contains("Dynamic Cooldown");
            this.eatAndAttack = options.contains("No Attack When Eat");
            this.box = box;
            this.aimMode = aimMode;
            this.onlySpaceCrits = onlySpaceCrits;
        }
    }
}