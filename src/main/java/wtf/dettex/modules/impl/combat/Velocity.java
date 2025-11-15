package wtf.dettex.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Velocity extends Module {

    final SelectSetting mode = new SelectSetting("Mode", "Velocity handling mode")
            .value("Cancel", "Grim Skip", "Grim Cancel", "Grim Cancel 2", "Grim New", "Funtime")
            .selected("Cancel");

    int skip = 0;
    boolean cancel;
    boolean damaged;
    boolean flag;
    int ccCooldown;

    public Velocity() {
        super("Velocity", ModuleCategory.COMBAT);
        setup(mode);
    }

    @Override
     
    public void activate() {
        skip = 0;
        cancel = false;
        damaged = false;
        flag = false;
        ccCooldown = 0;
    }

    @EventHandler
    
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        Packet<?> packet = e.getPacket();
        if (e.getType() == PacketEvent.Type.RECEIVE) {
            // Receive side handling
            if (mode.isSelected("Cancel")) {
                if (packet instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
                    e.cancel();
                }
                return;
            }

            if (mode.isSelected("Grim Skip")) {
                if (packet instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
                    skip = 6;
                    e.cancel();
                }
                return;
            }

            if (mode.isSelected("Grim Cancel")) {
                if (packet instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
                    e.cancel();
                    cancel = true;
                }
                if (packet instanceof PlayerPositionLookS2CPacket) {
                    skip = 3;
                }
                return;
            }

            if (mode.isSelected("Grim Cancel 2")) {
                if (packet instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
                    skip = 8;
                    e.cancel();
                }
                if (packet instanceof PlayerPositionLookS2CPacket) {
                    // server correction resets the window
                    skip = -8;
                }
                return;
            }

            if (mode.isSelected("Funtime")) {
                if (packet instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
                    if (skip >= 2) return;
                    e.cancel();
                    damaged = true;
                }
                if (packet instanceof PlayerPositionLookS2CPacket) {
                    skip = 3;
                }
                return;
            }

            if (mode.isSelected("Grim New")) {
                if (ccCooldown > 0) {
                    // no-op, tick reduces
                } else {
                    if (packet instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
                        e.cancel();
                        flag = true;
                    }
                    if (packet instanceof ExplosionS2CPacket) {
                        // optionally allow cancel; here we cancel client knockback
                        e.cancel();
                        flag = true;
                    }
                    if (packet instanceof PlayerPositionLookS2CPacket) {
                        ccCooldown = 5;
                    }
                }
                return;
            }
        } else {
            // Send side handling
            if (mode.isSelected("Grim Skip")) {
                if (packet instanceof PlayerMoveC2SPacket) {
                    if (skip > 0) {
                        skip--;
                        e.cancel();
                    }
                }
                return;
            }

            if (mode.isSelected("Grim Cancel")) {
                if (packet instanceof PlayerMoveC2SPacket) {
                    skip--;
                    if (cancel) {
                        if (skip <= 0) {
                            // send corrective packets to keep server in sync
                            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                    mc.player.getYaw(), mc.player.getPitch(),
                                    mc.player.isOnGround(), false
                            ));
                            BlockPos pos = mc.player.getBlockPos();
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP
                            ));
                        }
                        cancel = false;
                    }
                }
                return;
            }

            if (mode.isSelected("Grim Cancel 2")) {
                if (packet instanceof PlayerMoveC2SPacket) {
                    if (skip > 1) {
                        skip--;
                        e.cancel();
                    } else if (skip < 0) {
                        skip++;
                    }
                }
                return;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.isSelected("Funtime")) {
            skip--;
            if (damaged) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.getYaw(), mc.player.getPitch(),
                        mc.player.isOnGround(), false
                ));
                BlockPos pos = mc.player.getBlockPos();
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP
                ));
                damaged = false;
            }
        }

        if (mode.isSelected("Grim New")) {
            if (flag) {
                if (ccCooldown <= 0) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.player.getYaw(), mc.player.getPitch(),
                            mc.player.isOnGround(), false
                    ));
                    BlockPos pos = mc.player.getBlockPos();
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP
                    ));
                }
                flag = false;
            }
            if (ccCooldown > 0) ccCooldown--;
        }
    }
}
