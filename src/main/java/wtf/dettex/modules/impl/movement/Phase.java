package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Phase extends Module {
    private static volatile Phase SELF;

    SelectSetting mode = new SelectSetting("Mode", "Phase mode").value("Packet", "Shape", "NoClip").selected("Packet");

    final List<Packet<?>> queued = new CopyOnWriteArrayList<>();
    @NonFinal Box lastBox;
    @NonFinal int tickCounter = 0;

    public Phase() {
        super("Phase", ModuleCategory.MOVEMENT);
        setup(mode);
        SELF = this;
    }

    @Override
     
    public void deactivate() {
        resumePackets();
        lastBox = null;
        tickCounter = 0;
        if (mc.player != null) mc.player.noClip = false;
        super.deactivate();
    }

    @EventHandler
     
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!state) return;

        if (e.getType() == PacketEvent.Type.RECEIVE && e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            resumePackets();
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch(),
                    mc.player.isOnGround(), false
            ));
            return;
        }

        if (mode.isSelected("Packet") && e.isSend() && shouldPhase()) {
            Packet<?> p = e.getPacket();
            if (!(p instanceof KeepAliveC2SPacket) && !(p instanceof CommonPongC2SPacket)) {
                queued.add(p);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        tickCounter++;

        if (mode.isSelected("NoClip")) {
            mc.player.noClip = shouldPhase();
        } else {
            mc.player.noClip = false;
        }

        if (mode.isSelected("Packet")) {
            if (tickCounter >= 10) {
                resumePackets();
                tickCounter = 0;
            }
            if (shouldPhase()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.getYaw(), mc.player.getPitch(),
                        mc.player.isOnGround(), false
                ));
            }
        }

        mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
    }

    @EventHandler
     
    public void onWorldRender(WorldRenderEvent e) {
        if (lastBox != null) {
            Render3DUtil.drawBox(lastBox, 0x80FFFFFF, 2, true, false, true);
        }
    }

    private boolean shouldPhase() {
        if (mc.player == null || mc.world == null) return false;
        Box hitbox = mc.player.getBoundingBox();
        BlockPos min = BlockPos.ofFloored(hitbox.minX, hitbox.minY, hitbox.minZ);
        BlockPos max = BlockPos.ofFloored(hitbox.maxX, hitbox.maxY, hitbox.maxZ);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(mc.world, pos);
                        for (Box b : shape.getBoundingBoxes()) {
                            if (b.offset(pos.getX(), pos.getY(), pos.getZ()).intersects(hitbox)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void resumePackets() {
        if (mc.player == null || mc.world == null) return;
        if (!queued.isEmpty()) {
            for (Packet<?> p : new ArrayList<>(queued)) {
                mc.player.networkHandler.sendPacket(p);
            }
            queued.clear();
            lastBox = mc.player.getBoundingBox();
        }
    }

    public boolean isShapeModeActive() {
        return state && mode.isSelected("Shape");
    }

    public static boolean isShapeModeActiveStatic() {
        Phase p = SELF;
        return p != null && p.state && p.mode.isSelected("Shape");
    }
}

