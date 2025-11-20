package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.MovingUtil;
import wtf.dettex.event.impl.player.MoveEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AbilitiesFly extends Module {
    ValueSetting speedSetting = new ValueSetting("Speed", "Select fly speed").setValue(2.0F).range(0.5F, 4.0F);

    public AbilitiesFly() {
        super("AbilitiesFly", "Abilities Fly", ModuleCategory.MOVEMENT);
        setup(speedSetting);
    }
    
    @EventHandler

    public void onMove(MoveEvent e) {
        if (mc.player != null && mc.player.getAbilities().flying) {
            float speed = speedSetting.getValue();
            float y = mc.player.isSneaking() ? -speed : mc.player.jumping ? speed : 0;
            double[] motion = MovingUtil.calculateDirection(speed);
            e.setMovement(new Vec3d(motion[0], y, motion[1]));
        }
    }
}

