package wtf.dettex.modules.impl.movement;

import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.math.TimerUtils;
import wtf.dettex.common.util.world.ServerUtil;
import wtf.dettex.event.impl.player.MotionEvent;

public class GrimElytra extends Module {
    private TimerUtils ticks = new TimerUtils();
    int ticksTwo = 0;
    public GrimElytra(){
        super("GrimGlide", ModuleCategory.MOVEMENT);
    }

    @EventHandler
     
    public void onEvent(MotionEvent event) {
        if ((mc.player == null || mc.world == null) || !mc.player.isGliding()) return;
        ticksTwo++;
        Vec3d pos = mc.player.getPos();

        float yaw = mc.player.getYaw();
        double forward = 0.087;
        double motion = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);

        float valuePidor = ServerUtil.isReallyWorld() ? 48 : 52;
        if (motion >= valuePidor) {
            forward = 0f;
            motion = 0;
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;
        mc.player.setVelocity(dx * MathUtil.randomFloat(1.1f, 1.21f), mc.player.getVelocity().y - 0.02f, dz * MathUtil.randomFloat(1.1f, 1.21f));

        if (ticks.passed(50)) {
            mc.player.setPosition(
                    pos.getX() + dx,
                    pos.getY(),
                    pos.getZ() + dz
            );

            ticks.reset();
        }
        mc.player.setVelocity(dx * MathUtil.randomFloat(1.1f, 1.21f), mc.player.getVelocity().y + 0.016f, dz * MathUtil.randomFloat(1.1f, 1.21f));
    }
}


