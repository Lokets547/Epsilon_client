package wtf.dettex.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryComponent;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.entity.SimulatedPlayer;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.task.TaskPriority;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.player.MotionEvent;
import wtf.dettex.event.impl.player.RotationUpdateEvent;
import wtf.dettex.event.types.EventType;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationConfig;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.impl.combat.killaura.rotation.angle.SnapSmoothMode;
import wtf.dettex.modules.impl.combat.killaura.rotation.angle.SpookyTimeSmoothMode;
import wtf.dettex.modules.setting.implement.SelectSetting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Spider extends Module {

    Script blockScript = new Script();
    StopWatch funTimeWatch = new StopWatch();
    StopWatch waterBucketWatch = new StopWatch();

    SelectSetting mode = new SelectSetting("Mode", "Spider bypass mode").value("Block", "FunTime", "WaterBucket").selected("FunTime");

    Set<Item> placeableItems = new HashSet<>(Arrays.asList(
            Items.POPPY,
            Items.BLUE_ORCHID,
            Items.ALLIUM,
            Items.AZURE_BLUET,
            Items.RED_TULIP,
            Items.ORANGE_TULIP,
            Items.WHITE_TULIP,
            Items.PINK_TULIP,
            Items.KELP,
            Items.OXEYE_DAISY,
            Items.LILY_OF_THE_VALLEY,
            Items.SHORT_GRASS,
            Items.TALL_GRASS,
            Items.OAK_SAPLING,
            Items.SPRUCE_SAPLING,
            Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING,
            Items.PEONY,
            Items.ACACIA_SAPLING,
            Items.DARK_OAK_SAPLING,
            Items.SUNFLOWER,
            Items.REPEATER,
            Items.FERN,
            Items.NETHER_WART,
            Items.LILAC,
            Items.RED_MUSHROOM,
            Items.BROWN_MUSHROOM,
            Items.SUGAR_CANE
    ));

    public Spider() {
        super("Spider", "Spider", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
     
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!state || !mode.isSelected("Block") || fullNullCheck() || event.getType() != EventType.PRE) return;

        boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
        int slotId = PlayerInventoryUtil.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() instanceof BlockItem);
        BlockPos blockPos = findPos();

        if (!blockScript.isFinished() || (!offHand && slotId == -1) || blockPos.equals(BlockPos.ORIGIN)) {
            return;
        }

        ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
        Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        Vec3d vec = blockPos.toCenterPos();
        Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
        Angle angle = AngleUtil.calculateAngle(vec.subtract(new Vec3d(direction.getVector()).multiply(0.5))); 
        Angle.VecRotation vecRotation = new Angle.VecRotation(angle, angle.toVector());
        RotationController.INSTANCE.rotateTo(vecRotation, mc.player, 1, new RotationConfig(new SnapSmoothMode(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);

        if (canPlace(stack)) {
            int prev = mc.player.getInventory().selectedSlot;
            if (!offHand) mc.player.getInventory().selectedSlot = slotId;
            BlockHitResult hitResult = new BlockHitResult(vec, direction.getOpposite(), blockPos, false);
            mc.interactionManager.interactBlock(mc.player, hand, hitResult);
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            if (!offHand) mc.player.getInventory().selectedSlot = prev;
        }
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        if (!state || mc.player == null) return;

        if (mode.isSelected("FunTime")) {
            handleFunTime(event);
        } else if (mode.isSelected("WaterBucket")) {
            handleWaterBucket(event);
        }
    }

    private void handleFunTime(MotionEvent event) {
        if (!mc.player.horizontalCollision) {
            funTimeWatch.reset();
            return;
        }

        long delay = MathHelper.clamp(500L - 100L, 0L, 500L);
        if (!funTimeWatch.finished(delay)) return;

        event.setOnGround(true);
        mc.player.setOnGround(true);
        mc.player.fallDistance = 0;
        mc.player.jump();
        funTimeWatch.reset();

        int slot = getBlockSlot();
        if (slot == -1) return;

        if (mc.player.fallDistance > 0 && mc.player.fallDistance < 1.5F) {
            placeFunTimeBlock(event, slot);
        }
    }

    private void handleWaterBucket(MotionEvent event) {
        if (!mc.player.horizontalCollision) {
            waterBucketWatch.reset();
            return;
        }

        if (!waterBucketWatch.finished(150)) {
            return;
        }

        boolean offhandBucket = mc.player.getOffHandStack().isOf(Items.WATER_BUCKET);
        int bucketSlot = findWaterBucketSlot();
        if (!offhandBucket && bucketSlot == -1) {
            return;
        }

        Hand hand = offhandBucket ? Hand.OFF_HAND : Hand.MAIN_HAND;
        int previous = mc.player.getInventory().selectedSlot;

        if (!offhandBucket && previous != bucketSlot) {
            mc.player.getInventory().selectedSlot = bucketSlot;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = eyePos.add(mc.player.getRotationVector().multiply(4.0D));
        Angle angle = AngleUtil.calculateAngle(targetPos);
        RotationController.INSTANCE.rotateTo(new Angle.VecRotation(angle, angle.toVector()), mc.player, 1,
                new RotationConfig(new SpookyTimeSmoothMode(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);

        HitResult hitResult = mc.player.raycast(4.0D, mc.getRenderTickCounter().getTickDelta(true), false);

        if (hitResult instanceof BlockHitResult blockHit) {
            PlayerIntersectionUtil.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(hand, blockHit, sequence));

            BlockPos placePos = blockHit.getBlockPos().offset(blockHit.getSide());
            BlockHitResult pickupHit = new BlockHitResult(mc.player.getPos(), blockHit.getSide(), placePos, false);
            PlayerIntersectionUtil.sendSequencedPacket(sequence -> new PlayerInteractBlockC2SPacket(hand, pickupHit, sequence));

            PlayerInventoryComponent.addTask(() -> {
                if (!offhandBucket && previous != bucketSlot) {
                    mc.player.getInventory().selectedSlot = previous;
                }
            });

            mc.player.setVelocity(mc.player.getVelocity().x, 0.36, mc.player.getVelocity().z);
            event.setOnGround(false);
        }

        waterBucketWatch.reset();
    }

    private void placeFunTimeBlock(MotionEvent event, int slot) {
        int previous = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        float currentYaw = mc.player.getYaw();
        float yaw = currentYaw;
        float pitch = 80.0F;
        event.setYaw(yaw);
        event.setPitch(pitch);

        float prevPitch = mc.player.getPitch();
        mc.player.setPitch(pitch);

        HitResult hitResult = mc.player.raycast(4.0D, mc.getRenderTickCounter().getTickDelta(true), false);

        mc.player.setPitch(prevPitch);

        if (hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos().offset(blockHit.getSide());
            if (mc.world.isAir(pos)) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }

        mc.player.getInventory().selectedSlot = previous;
        mc.player.setYaw(currentYaw);
        mc.player.fallDistance = 0;
    }

    private int findWaterBucketSlot() {
        return PlayerInventoryUtil.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET));
    }

    private boolean canPlace(ItemStack stack) {
        BlockPos blockPos = getBlockPos();
        if (blockPos.getY() >= mc.player.getBlockY()) return false;
        BlockItem blockItem = (BlockItem) stack.getItem();
        VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
        if (shape.isEmpty()) return false;
        Box box = shape.getBoundingBox().offset(blockPos);
        return !box.intersects(mc.player.getBoundingBox()) && box.intersects(SimulatedPlayer.simulateLocalPlayer(4).boundingBox);
    }

    private BlockPos findPos() {
        BlockPos blockPos = getBlockPos();
        if (!mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) return BlockPos.ORIGIN;
        return Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north())
                .filter(pos -> !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty())
                .findFirst()
                .orElse(BlockPos.ORIGIN);
    }

    private BlockPos getBlockPos() {
        return BlockPos.ofFloored(SimulatedPlayer.simulateLocalPlayer(1).pos.add(0, -1e-3, 0));
    }

    private int getBlockSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item instanceof BlockItem || placeableItems.contains(item)) {
                return i;
            }
        }
        return -1;
    }
}
