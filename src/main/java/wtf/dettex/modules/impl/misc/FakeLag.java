package wtf.dettex.modules.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.modules.setting.implement.GroupSetting;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.MovingUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.common.util.other.Instance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FakeLag extends Module {

    ValueSetting maxDelay = new ValueSetting("Max Delay", "Maximum delay in milliseconds")
            .range(0f, 2000f).setValue(500f);
    ValueSetting minDelay = new ValueSetting("Min Delay", "Minimum delay in milliseconds")
            .range(0f, 2000f).setValue(200f);

    BooleanSetting onlyTarget = new BooleanSetting("Only Target", "Only activate when Aura has a target");
    BooleanSetting inRange = new BooleanSetting("In Range", "Only activate when players are in range");
    ValueSetting maxRange = new ValueSetting("Max Range", "Maximum range to check for players")
            .range(1f, 10f).setValue(5f).visible(inRange::isValue);
    ValueSetting minRange = new ValueSetting("Min Range", "Minimum range to check for players")
            .range(1f, 10f).setValue(2f).visible(inRange::isValue);
    BooleanSetting onlyMoving = new BooleanSetting("Only Moving", "Only activate when player is moving");
    BooleanSetting onlyGround = new BooleanSetting("Only Ground", "Only activate when player is on ground");

    BooleanSetting playerActionPacket = new BooleanSetting("Player Action Packet", "Resume on player action packets");
    BooleanSetting updateVelocityPacket = new BooleanSetting("Update Velocity Packet", "Resume on velocity update packets");
    BooleanSetting playerInteractPacket = new BooleanSetting("Player Interact Packet", "Resume on player interact packets");
    BooleanSetting playerPositionPacket = new BooleanSetting("Player Position Packet", "Resume on player position packets");
    BooleanSetting explosionPacket = new BooleanSetting("Explosion Packet", "Resume on explosion packets");
    BooleanSetting healthUpdatePacket = new BooleanSetting("Health Update Packet", "Resume on health update packets");

    BooleanSetting renderOriginalPos = new BooleanSetting("Render Original Pos", "Render the original position box");

    GroupSetting delayGroup = new GroupSetting("Delay Settings", "Configure delay settings")
            .settings(maxDelay, minDelay).setValue(true);

    GroupSetting conditionGroup = new GroupSetting("Conditions", "Activation conditions")
            .settings(onlyTarget, inRange, maxRange, minRange, onlyMoving, onlyGround).setValue(true);

    GroupSetting packetGroup = new GroupSetting("Packet Settings", "Configure packet handling")
            .settings(playerActionPacket, updateVelocityPacket, playerInteractPacket, playerPositionPacket, explosionPacket, healthUpdatePacket).setValue(true);

    GroupSetting renderGroup = new GroupSetting("Render", "Visual settings")
            .settings(renderOriginalPos).setValue(true);

    final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    final StopWatch packetTimer = new StopWatch();
    PlayerEntity inRangeTarget = null;
    Vec3d originalPos;
    Box originalBox;

    public FakeLag() {
        super("FakeLag", ModuleCategory.MISC);
        setup();
    }

    private void setup() {
        setup(delayGroup, conditionGroup, packetGroup, renderGroup);
    }

    @Override

    public void activate() {
        packetTimer.reset();
        originalPos = mc.player.getPos();
        originalBox = mc.player.getBoundingBox();
    }

    @Override

    public void deactivate() {
        resumePackets();
        originalPos = null;
        originalBox = null;
        inRangeTarget = null;
    }

    @EventHandler

    public void onPacket(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;

        switch (e.getPacket()) {
            case PlayerRespawnS2CPacket respawn -> setState(false);
            case GameJoinS2CPacket join -> setState(false);
            case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) ->
                    setState(false);
            default -> {
                if (e.isSend()) {
                    handleSendPacket(e);
                } else {
                    handleReceivePacket(e);
                }
            }
        }
    }

    private void handleSendPacket(PacketEvent e) {
        if (!shouldActivate()) {
            resumePackets();
            return;
        }

        if (e.getPacket() instanceof PlayerMoveC2SPacket) {
            packets.add(e.getPacket());
            e.cancel();
        } else if (playerActionPacket.isValue() && e.getPacket() instanceof PlayerActionC2SPacket) {
            resumePackets();
        } else if (playerInteractPacket.isValue() && e.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            resumePackets();
        }
    }

    private void handleReceivePacket(PacketEvent e) {
        if (!shouldActivate()) {
            resumePackets();
            return;
        }

        boolean shouldResume = false;

        if (updateVelocityPacket.isValue() && e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && isVelocity(packet)) {
            shouldResume = true;
        } else if (explosionPacket.isValue() && e.getPacket() instanceof ExplosionS2CPacket packet && isKnockback(packet)) {
            shouldResume = true;
        } else if (healthUpdatePacket.isValue() && e.getPacket() instanceof HealthUpdateS2CPacket) {
            shouldResume = true;
        } else if (playerPositionPacket.isValue() && e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            shouldResume = true;
        }

        if (shouldResume) {
            resumePackets();
        }
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        if (!shouldActivate()) {
            resumePackets();
            return;
        }

        if (packetTimer.finished(getCurrentDelay())) {
            resumePackets();
        }
    }

    @EventHandler
     
    public void onWorldRender(WorldRenderEvent e) {
        if (PlayerIntersectionUtil.nullCheck() || !renderOriginalPos.isValue() || originalBox == null) return;
        Render3DUtil.drawBox(originalBox, ColorUtil.getClientColor(), 2, true, true, false);
    }

    private boolean shouldActivate() {
        if (onlyTarget.isValue()) {
            Aura aura = Instance.get(Aura.class);
            if (aura == null || !aura.state) return false;
        }

        if (onlyMoving.isValue() && !MovingUtil.hasPlayerMovement()) return false;
        if (onlyGround.isValue() && !mc.player.isOnGround()) return false;
        if (mc.player.isUsingItem()) return false;
        if (mc.player.horizontalCollision) return false;

        if (inRange.isValue()) {
            inRangeTarget = null;
            float checkRange = MathUtil.getRandom(minRange.getValue(), maxRange.getValue());

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (player.getPos().squaredDistanceTo(mc.player.getEyePos()) <= MathHelper.square(checkRange)) {
                    inRangeTarget = player;
                    break;
                }
            }

            return inRangeTarget != null;
        }

        return true;
    }

    private long getCurrentDelay() {
        return (long) MathUtil.getRandom(minDelay.getValue(), maxDelay.getValue());
    }

    private boolean isVelocity(EntityVelocityUpdateS2CPacket packet) {
        if (packet.getEntityId() != mc.player.getId()) return false;
        return Math.abs(packet.getVelocityX() / 8000.0) > 0.1D ||
                Math.abs(packet.getVelocityY() / 8000.0) > 0.1D ||
                Math.abs(packet.getVelocityZ() / 8000.0) > 0.1D;
    }

    private boolean isKnockback(ExplosionS2CPacket packet) {
        if (packet.playerKnockback().isEmpty()) return false;
        return Math.abs(packet.playerKnockback().get().getX()) > 0.1D ||
                Math.abs(packet.playerKnockback().get().getY()) > 0.1D ||
                Math.abs(packet.playerKnockback().get().getZ()) > 0.1D;
    }

    private void resumePackets() {
        if (!packets.isEmpty()) {
            packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
            packets.clear();
            originalPos = mc.player.getPos();
            originalBox = mc.player.getBoundingBox();
            packetTimer.reset();
        }
    }
}
