package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.event.impl.player.MoveEvent;
import wtf.dettex.event.impl.player.PostTickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AirStuck extends Module {

    public AirStuck() {
        super("AirStuck", "Air Stuck", ModuleCategory.MOVEMENT);
        setup();
    }

    @Override
     
    public void activate() {
        if (mc.player != null) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
            mc.player.fallDistance = 0.0F;
        }
    }

    @Override

    public void deactivate() {
        if (mc.world != null) {
            if (mc.player != null) mc.player.fallDistance = 0.0F;
        }
    }

    @EventHandler

    public void onMove(MoveEvent e) {
        if (mc.player == null || mc.world == null) return;
        e.setMovement(Vec3d.ZERO);
    }

    @EventHandler

    public void onPostTick(PostTickEvent e) {
        if (mc.player == null) return;
        mc.player.setVelocity(0.0, 0.0, 0.0);
        mc.player.fallDistance = 0.0F;
    }
}

