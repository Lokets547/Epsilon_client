package wtf.dettex.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.MovingUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.world.BlockFinder;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Speed extends Module {

    final SelectSetting mode = new SelectSetting("Mode", "Режим ускорения")
            .value("Vanilla", "Collision", "Strafe", "Matrix", "Verus", "Shulker").selected("Vanilla");

    final ValueSetting speed = new ValueSetting("Speed", "Множитель скорости")
            .setValue(2.0F).range(0.1F, 4.0F);

    final ValueSetting timerSpeed = new ValueSetting("Timer", "Скорость таймера")
            .setValue(1.0F).range(0.1F, 5.0F).visible(() -> mode.isSelected("Matrix") || mode.isSelected("Verus"));


    final BooleanSetting onlyGround = new BooleanSetting("Only Ground", "Работать только на земле")
            .setValue(true)
            .visible(() -> !mode.isSelected("Shulker"));

    final BooleanSetting damageBoost = new BooleanSetting("Damage Boost", "Ускорение при получении урона")
            .setValue(false)
            .visible(() -> !mode.isSelected("Shulker"));

    final ValueSetting collisionRadius = new ValueSetting("Collision Radius", "Радиус проверки столкновений")
            .setValue(0.6F).range(0.1F, 3.0F).visible(() -> mode.isSelected("Collision"));


    private boolean wasNearShulker = false;
    private boolean wasOpen = false;

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(mode, speed, timerSpeed, onlyGround, damageBoost, collisionRadius);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        boolean shulkerMode = mode.isSelected("Shulker");

        if (onlyGround.isValue() && !mc.player.isOnGround() && !shulkerMode) return;

        if (!shulkerMode && !MovingUtil.hasPlayerMovement()) return;

        switch (mode.getSelected()) {
            case "Vanilla" -> applyVanillaSpeed();
            case "Collision" -> applyCollisionSpeed();
            case "Strafe" -> applyStrafeSpeed();
            case "Matrix" -> applyMatrixSpeed();
            case "Verus" -> applyVerusSpeed();
            case "Shulker" -> handleShulkerMode();
//            case "FunTimeSnow" -> applyFunTimeSnow();
        }

        if (damageBoost.isValue() && mc.player.hurtTime > 0) {
            double[] motion = MovingUtil.calculateDirection(0.1);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
    }

    @Override
    public void deactivate() {
        wasNearShulker = false;
        wasOpen = false;
        super.deactivate();
    }

    private void applyVanillaSpeed() {
        double[] motion = MovingUtil.calculateDirection(speed.getValue() * 0.1);
        mc.player.addVelocity(motion[0], 0.0, motion[1]);
    }

    private void applyCollisionSpeed() {
        if (!mc.player.isOnGround()) {
        boolean anyCollision = false;
        Box expandedBox = mc.player.getBoundingBox().expand(collisionRadius.getValue());

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player) continue;
            if ((ent instanceof LivingEntity || ent instanceof BoatEntity)
                    && expandedBox.intersects(ent.getBoundingBox())) {
                anyCollision = true;
                break;
            }
        }

        if (anyCollision) {
            double[] motion = MovingUtil.calculateDirection(speed.getValue() * 0.04);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
            }
        }
    }
    private void applyStrafeSpeed() {
        if (mc.player.isOnGround()) {
            double[] motion = MovingUtil.calculateDirection(speed.getValue() * 0.05);
            mc.player.setVelocity(motion[0], mc.player.getVelocity().y, motion[1]);
        }
    }

    private void applyMatrixSpeed() {
        if (mc.player.isOnGround()) {
            double[] motion = MovingUtil.calculateDirection(speed.getValue() * 0.03);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
    }

    private void applyVerusSpeed() {
        if (mc.player.isOnGround() && mc.player.age % 2 == 0) {
            double[] motion = MovingUtil.calculateDirection(speed.getValue() * 0.02);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
    }

    private void handleShulkerMode() {
        boolean isNearShulker = false;
        boolean isOpen = false;

        for (BlockEntity blockEntity : BlockFinder.INSTANCE.blockEntities) {
            if (!(blockEntity instanceof ShulkerBoxBlockEntity shulker)) continue;

            BlockPos pos = shulker.getPos();
            double dx = mc.player.getX() - (pos.getX() + 0.7);
            double dz = mc.player.getZ() - (pos.getZ() + 0.7);
            double dy = Math.abs(mc.player.getY() - pos.getY());

            if (Math.sqrt(dx * dx + dz * dz) <= 1.5 && dy <= 2.5) {
                float progress = shulker.getAnimationProgress(mc.getRenderTickCounter().getTickDelta(true));
                if (progress > 0.0f) {
                    isOpen = true;
                }

                isNearShulker = true;
                break;
            }
        }

        if (wasNearShulker && wasOpen && !isOpen && mc.player.isOnGround()) {
            applyLongJumpBoost();
        }

        wasNearShulker = isNearShulker;
        wasOpen = isOpen;
    }

    private void applyLongJumpBoost() {
        float yawRad = (float) Math.toRadians(mc.player.getYaw());
        double multiplier = Math.max(speed.getValue(), 1.0);
        double boost = multiplier * 0.9;
        double verticalBoost = multiplier * 0.4;

        double motionX = -Math.sin(yawRad) * boost;
        double motionZ = Math.cos(yawRad) * boost;

        mc.player.setVelocity(motionX, verticalBoost, motionZ);
    }

//    private void applyFunTimeSnow() {
//        if (mc.player == null || mc.world == null) return;
//        if (!mc.player.isOnGround()) return;
//        if (!hasOffhandBlock()) return;
//
//        float prevPitch = mc.player.getPitch();
//        mc.player.setPitch(90.0F);
//
//        HitResult hitResult = mc.player.raycast(4.0D, mc.getRenderTickCounter().getTickDelta(true), false);
//
//        if (hitResult instanceof BlockHitResult blockHit) {
//            int slot = PlayerInventoryUtil.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() instanceof BlockItem);
//            int previous = mc.player.getInventory().selectedSlot;
//
//            if (slot != -1 && slot != previous && mc.getNetworkHandler() != null) {
//                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
//            }
//
//            PlayerIntersectionUtil.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, blockHit, sequence));
//
//            if (slot != -1 && slot != previous && mc.getNetworkHandler() != null) {
//                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previous));
//            }
//        }
//
//        mc.player.setPitch(prevPitch);
//
//        Vec3d velocity = mc.player.getVelocity();
//        float boost = funTimeBoost.getValue();
//
//        if (isInSnow()) {
//            float snowBoost = funTimeSnowBoost.getValue();
//            mc.player.setVelocity(velocity.x * snowBoost, 1.0E-6, velocity.z * snowBoost);
//        } else {
//            mc.player.setVelocity(velocity.x * boost, velocity.y, velocity.z * boost);
//        }
//
//        mc.player.setPitch(90.0F);
//    }

    private boolean hasOffhandBlock() {
        if (mc.player == null) return false;
        ItemStack offhand = mc.player.getOffHandStack();
        return !offhand.isEmpty() && offhand.getItem() instanceof BlockItem;
    }

    private boolean isInSnow() {
        if (mc.player == null) return false;
        BlockPos pos = mc.player.getBlockPos().down();
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.SNOW || block == Blocks.SNOW_BLOCK;
    }
}
