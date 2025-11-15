package wtf.dettex.modules.impl.misc;

import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.packet.EventPacketNew;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;

public class AntiCrasher extends Module {
    private static final double LIMIT = 1E9;

    public AntiCrasher() {
        super("AntiCrash", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacketReceive(EventPacketNew.Receive event) {
        Object packet = event.getPacket();

        if (packet instanceof ExplosionS2CPacket explosion) {
            if (exceeds(explosion.center())) {
                event.cancel();
                return;
            }
            if (explosion.playerKnockback().map(this::exceeds).orElse(false)) {
                event.cancel();
                return;
            }
        }

        if (packet instanceof ParticleS2CPacket particle) {
            if (exceeds(particle.getX(), particle.getY(), particle.getZ())
                    || exceeds(particle.getSpeed())
                    || exceeds(particle.getOffsetX(), particle.getOffsetY(), particle.getOffsetZ())) {
                event.cancel();
                return;
            }
        }

        if (packet instanceof PlayerPositionLookS2CPacket position) {
            PlayerPosition change = position.change();
            if (exceeds(change.position()) || exceeds(change.yaw(), change.pitch())) {
                event.cancel();
            }
        }
    }

    private boolean exceeds(Vec3d vec) {
        return exceeds(vec.x, vec.y, vec.z);
    }

    private boolean exceeds(double... values) {
        for (double value : values) {
            if (Math.abs(value) > LIMIT) {
                return true;
            }
        }
        return false;
    }
}