package wtf.dettex.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.event.impl.player.MotionEvent;
import wtf.dettex.event.impl.player.PostTickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFall extends Module {

    public NoFall() {
        super("NoFall", "No Fall", ModuleCategory.PLAYER);
        setup();
    }

    @EventHandler
     
    public void onMotion(MotionEvent e) {
        if (mc.player == null) return;
        boolean fallingFast = mc.player.getVelocity().y < -0.08;
        if (mc.player.fallDistance > 2.5f || fallingFast) {
            e.setOnGround(true);
        }
    }

    @EventHandler
    public void onPostTick(PostTickEvent e) {
        if (mc.player == null) return;
        mc.player.fallDistance = 0.0F;
    }
}

