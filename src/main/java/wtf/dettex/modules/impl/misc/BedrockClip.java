package wtf.dettex.modules.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.entity.MoveUtil;
import wtf.dettex.event.impl.player.TickEvent;

public class BedrockClip extends Module {

    public BedrockClip() {
        super("BedrockClip", ModuleCategory.MISC);
    }

    @EventHandler
    
    public void onTick(TickEvent event) {
        if (fullNullCheck()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof BoatEntity && mc.player.distanceTo(entity) <= 2f) {

                mc.player.interact(entity, mc.player.getActiveHand());


                if (mc.player.getVehicle() == entity) {
                    if (MoveUtil.isMoving()) {
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), -1, mc.player.getZ(),mc.player.getYaw(), mc.player.getPitch(),mc.player.horizontalCollision,mc.player.horizontalCollision));
                    }
                }

                break;
            }
        }
    }
}

