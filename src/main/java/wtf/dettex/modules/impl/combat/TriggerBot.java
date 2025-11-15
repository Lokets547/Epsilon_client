package wtf.dettex.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.api.repository.friend.FriendUtils;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.Main;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.impl.combat.killaura.attack.AttackPerpetrator;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RaytracingUtil;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerBot extends Module {
    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }

    private LivingEntity target = null;

    private final MultiSelectSetting attackSetting = new MultiSelectSetting("Attack setting", "Allows you to customize the attack")
            .value("Only Critical", "Dynamic Cooldown", "Break Shield", "UnPress Shield", "No Attack When Eat");
    public BooleanSetting onlySpaceCrits = new BooleanSetting("Only Space Crits", "Crits only if the jump button is pressed").setValue(true).visible(() -> attackSetting.isSelected("Only Critical"));

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        setup(attackSetting);
    }


    @Override
     
    public void deactivate() {
        target = null;
        super.deactivate();
    }


    @EventHandler
     
    public void onTick(TickEvent e) {
        EntityHitResult result = RaytracingUtil.raytraceEntity(3, AngleUtil.cameraAngle(), s -> !FriendUtils.isFriend(s.getName().getString()));
        if (result instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity entity) {
            AttackPerpetrator.AttackPerpetratorConfigurable config = new AttackPerpetrator.AttackPerpetratorConfigurable(target = entity, AngleUtil.cameraAngle(),
                    3.3F, attackSetting.getSelected(), new SelectSetting("Mode", "").value("Default"), target.getBoundingBox(), onlySpaceCrits.isValue());
            Main.getInstance().getAttackPerpetrator().performAttack(config);
        } else target = null;
    }
}
