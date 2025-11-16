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
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.MovingUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.world.BlockFinder;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.MoveEvent;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Speed extends Module {

    final SelectSetting mode = new SelectSetting("Обход", "Режим ускорения")
            .value("Vanilla", "Collision", "Strafe", "Matrix", "Verus", "Shulker", "HolyWorld", "Funtime Snow", "Realyworldbeta", "Legit")
            .selected("Vanilla");

    final ValueSetting speed = new ValueSetting("Speed", "Множитель скорости")
            .setValue(2.0F).range(0.1F, 4.0F)
            .visible(() -> !mode.isSelected("HolyWorld") && !mode.isSelected("Legit") && !mode.isSelected("Realyworldbeta"));

    final ValueSetting timerSpeed = new ValueSetting("Timer", "Скорость таймера")
            .setValue(1.0F).range(0.1F, 5.0F).visible(() -> mode.isSelected("Matrix") || mode.isSelected("Verus"));

    // HolyWorld настройки
    final ValueSetting collisionRadius = new ValueSetting("Радиус", "Радиус проверки столкновений")
            .setValue(0.25F).range(0.01F, 1.0F).visible(() -> mode.isSelected("HolyWorld") || mode.isSelected("Collision"));

    final ValueSetting collisionSpeed = new ValueSetting("Скорость", "Скорость буста")
            .setValue(0.17F).range(0.05F, 1.0F).visible(() -> mode.isSelected("HolyWorld"));

    final ValueSetting collisionAcceleration = new ValueSetting("Ускорение", "Ускорение движения")
            .setValue(0.1F).range(0.05F, 0.5F).visible(() -> mode.isSelected("HolyWorld"));

    final BooleanSetting silentReverse = new BooleanSetting("Серверный разворот", "Разворот на сервере")
            .setValue(false).visible(() -> mode.isSelected("HolyWorld"));

    final BooleanSetting onlyWhenMoving = new BooleanSetting("Только при движении", "Работать только при движении")
            .setValue(false).visible(() -> mode.isSelected("HolyWorld"));

    // Legit настройки
    final BooleanSetting legitAutoJump = new BooleanSetting("Авто прыжок", "Автоматический прыжок")
            .setValue(true).visible(() -> mode.isSelected("Legit"));

    final BooleanSetting legitStrafe = new BooleanSetting("Страф", "Стрейф в воздухе")
            .setValue(true).visible(() -> mode.isSelected("Legit"));

    final ValueSetting legitDelay = new ValueSetting("Задержка", "Задержка прыжка")
            .setValue(0F).range(0F, 200F).visible(() -> mode.isSelected("Legit"));

    // Общие настройки
    final BooleanSetting onlyGround = new BooleanSetting("Only Ground", "Работать только на земле")
            .setValue(true)
            .visible(() -> !mode.isSelected("Shulker") && !mode.isSelected("HolyWorld") && !mode.isSelected("Legit"));

    final BooleanSetting damageBoost = new BooleanSetting("Damage Boost", "Ускорение при получении урона")
            .setValue(false)
            .visible(() -> !mode.isSelected("Shulker") && !mode.isSelected("HolyWorld") && !mode.isSelected("Legit"));

    private final StopWatch legitJumpTimer = new StopWatch();
    private boolean wasOnGround = false;
    private boolean wasNearShulker = false;
    private boolean wasOpen = false;

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(mode, speed, timerSpeed, collisionRadius, collisionSpeed, collisionAcceleration,
                silentReverse, onlyWhenMoving, legitAutoJump, legitStrafe, legitDelay,
                onlyGround, damageBoost);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Проверка Aura для HolyWorld
        if (mode.isSelected("HolyWorld")) {
            try {
                Aura aura = Aura.getInstance();
                if (aura != null && aura.isState() &&
                        (mc.player.isTouchingWater() || mc.player.isSwimming() || mc.player.isSubmergedIn(FluidTags.WATER))) {
                    return;
                }
            } catch (Throwable ignored) {}
        }

        boolean shulkerMode = mode.isSelected("Shulker");
        boolean holyWorldMode = mode.isSelected("HolyWorld");
        boolean legitMode = mode.isSelected("Legit");

        if (onlyGround.isValue() && !mc.player.isOnGround() && !shulkerMode && !holyWorldMode && !legitMode) return;

        if (!shulkerMode && !holyWorldMode && !legitMode && !MovingUtil.hasPlayerMovement()) return;

        switch (mode.getSelected()) {
            case "Vanilla" -> applyVanillaSpeed();
            case "Collision" -> applyCollisionSpeed();
            case "Strafe" -> applyStrafeSpeed();
            case "Matrix" -> applyMatrixSpeed();
            case "Verus" -> applyVerusSpeed();
            case "Shulker" -> handleShulkerMode();
            case "HolyWorld" -> doHolyWorldBoost();
            case "Funtime Snow" -> applyFuntimeSnow();
            case "Realyworldbeta" -> applyRealyworldbeta();
            case "Legit" -> applyLegitMode();
        }

        if (damageBoost.isValue() && mc.player.hurtTime > 0) {
            double[] motion = MovingUtil.calculateDirection(0.1);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
    }

    @Override
    public void deactivate() {
        if (mc.world != null) {
            mc.world.getTickManager().setTickRate(20.0F);
        }
        wasNearShulker = false;
        wasOpen = false;
        wasOnGround = false;
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

    // ===== HolyWorld Режим =====
    private void doHolyWorldBoost() {
        Box aabb = mc.player.getBoundingBox().expand(collisionRadius.getValue());

        int armorstands = (int) mc.world.getEntitiesByClass(ArmorStandEntity.class, aabb, e -> true).size();
        boolean canBoost = armorstands > 1 || mc.world.getEntitiesByClass(LivingEntity.class, aabb, e -> e != mc.player).size() > 1;

        if (!canBoost) return;

        LivingEntity target = resolveBoostTarget(aabb);
        if (target == null) return;

        float boostFactor = armorstands > 1 ? 1.0F / (float) armorstands : collisionSpeed.getValue();
        Vec3d motion = mc.player.getVelocity();

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double len = Math.hypot(dx, dz);
        if (len < 1.0E-6) return;
        double nx = dx / len;
        double nz = dz / len;

        float accel = boostFactor * collisionAcceleration.getValue();
        double newMotionX = motion.x + nx * accel;
        double newMotionZ = motion.z + nz * accel;

        double horizontalSpeed = Math.sqrt(newMotionX * newMotionX + newMotionZ * newMotionZ);
        if (horizontalSpeed < 0.99) {
            mc.player.setVelocity(newMotionX, motion.y, newMotionZ);
        }
    }

    private LivingEntity resolveBoostTarget(Box searchBox) {
        try {
            Aura aura = Aura.getInstance();
            if (aura != null && aura.isState() && aura.getTarget() != null && aura.getTarget().isAlive()) {
                return aura.getTarget();
            }
        } catch (Throwable ignored) {}

        List<LivingEntity> nearby = mc.world.getEntitiesByClass(LivingEntity.class, searchBox,
                e -> e != null && e.isAlive() && e != mc.player && !(e instanceof ArmorStandEntity));
        if (nearby.isEmpty()) return null;
        nearby.sort(Comparator.comparingDouble(ent -> ent.squaredDistanceTo(mc.player)));
        return nearby.get(0);
    }

    @EventHandler
    public void onPacketSend(PacketEvent e) {
        if (!isState() || e.getType() != PacketEvent.Type.SEND || mc.player == null) return;
        if (!silentReverse.isValue()) return;
        if (!mode.isSelected("HolyWorld")) return;

        if (onlyWhenMoving.isValue() && !MovingUtil.hasPlayerMovement()) return;

        if (e.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround packet) {
            float serverPitch = mc.player.getPitch();
            float reverseYaw = reverseYaw(mc.player.getYaw());
            boolean onGround = mc.player.isOnGround();
            boolean horizontalCollision = mc.player.horizontalCollision;
            e.setPacket(new PlayerMoveC2SPacket.LookAndOnGround(reverseYaw, serverPitch, onGround, horizontalCollision));
        } else if (e.getPacket() instanceof PlayerMoveC2SPacket.Full packet) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float serverPitch = mc.player.getPitch();
            float reverseYaw = reverseYaw(mc.player.getYaw());
            boolean onGround = mc.player.isOnGround();
            boolean horizontalCollision = mc.player.horizontalCollision;
            e.setPacket(new PlayerMoveC2SPacket.Full(x, y, z, reverseYaw, serverPitch, onGround, horizontalCollision));
        }
    }

    // ===== Funtime Snow Режим =====
    private void applyFuntimeSnow() {
        BlockPos playerPos = new BlockPos(
                (int) mc.player.getX(),
                (int) mc.player.getY(),
                (int) mc.player.getZ()
        );
        if (mc.world.getBlockState(playerPos).getBlock() == Blocks.SNOW) {
            if (mc.player.isOnGround() && MovingUtil.hasPlayerMovement()) {
                if (mc.player.horizontalCollision && mc.options.jumpKey.isPressed()) {
                    if (!mc.options.jumpKey.isPressed()) {
                        mc.player.jump();
                    }
                    return;
                }
                mc.player.jump();
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(velocity.x, 0.1, velocity.z);
            }
        }
    }

    // ===== Realyworldbeta Режим =====
    private void applyRealyworldbeta() {
        float timerValue = mc.player.fallDistance <= 0.4f ? 1.3f : (mc.player.fallDistance != Math.ceil(mc.player.fallDistance) ? 0.9f : 1f);
        if (MovingUtil.hasPlayerMovement()) {
            if (mc.world != null) {
                mc.world.getTickManager().setTickRate(20.0F * timerValue);
            }
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        } else {
            if (mc.world != null) {
                mc.world.getTickManager().setTickRate(20.0F * 7.0F);
            }
        }
    }

    // ===== Legit Режим =====
    private void applyLegitMode() {
        if (legitAutoJump.isValue() && mc.player.isOnGround() && MovingUtil.hasPlayerMovement()) {
            if (legitJumpTimer.passedMs((long) legitDelay.getValue())) {
                mc.player.jump();
                legitJumpTimer.reset();
            }
        }
    }

    @EventHandler
    public void onMove(MoveEvent event) {
        if (!isState()) return;
        if (!mode.isSelected("Legit")) return;

        if (mc.player.isOnGround()) {
            if (!wasOnGround) {
                legitJumpTimer.reset();
            }
            wasOnGround = true;
        } else {
            wasOnGround = false;

            if (legitStrafe.isValue() && MovingUtil.hasPlayerMovement()) {
                Vec3d movement = event.getMovement();
                double motionX = movement.x;
                double motionZ = movement.z;

                if (motionX != 0 || motionZ != 0) {
                    double currentSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
                    double acceleration = currentSpeed * 1.0001;

                    if (acceleration < 0.5) {
                        double yaw = Math.toRadians(mc.player.getYaw());
                        double newX = (-Math.sin(yaw) * acceleration);
                        double newZ = (Math.cos(yaw) * acceleration);
                        event.setMovement(new Vec3d(newX, movement.y, newZ));
                    }
                }
            }
        }
    }

    private float reverseYaw(float yaw) {
        return MathHelper.wrapDegrees(yaw + 180.0F);
    }
}
