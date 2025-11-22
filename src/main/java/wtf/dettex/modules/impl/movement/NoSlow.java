package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import wtf.dettex.common.util.entity.MovingUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryComponent;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.player.MotionEvent;
import wtf.dettex.event.types.EventType;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.entity.*;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.impl.item.UsingItemEvent;
import wtf.dettex.event.impl.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends Module {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;
    private int rwTicks;
    int cals;
    public final MultiSelectSetting slowTypeSetting = new MultiSelectSetting("Target Type", "Filters the entire list of targets by type").value("Using Item", "Web");
    public final SelectSetting itemMode = new SelectSetting("Item Mode", "Select bypass mode").value("Grim Old", "Test", "ReallyWorld", "FunTimeOld", "Advanced", "FTCrossbowOld", "SpookyTime").visible(() -> slowTypeSetting.isSelected("Using Item"));
    public final SelectSetting webMode = new SelectSetting("Web Mode", "Select bypass mode").value("Grim").visible(() -> slowTypeSetting.isSelected("Web"));

    public NoSlow() {
        super("NoSlow", "No Slow", ModuleCategory.MOVEMENT);
        setup(slowTypeSetting, itemMode, webMode);
    }

    @EventHandler

    public void onTick(TickEvent e) {
        if (slowTypeSetting.isSelected("Web") && PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB)) {
            double[] speed = MovingUtil.calculateDirection(0.64);
            mc.player.addVelocity(speed[0], 0, speed[1]);
            mc.player.velocity.y = mc.options.jumpKey.isPressed() ? 1.2 : mc.options.sneakKey.isPressed() ? -2 : 0;
        }
        if (itemMode.isSelected("ReallyWorld")) {
            if (mc.player != null && mc.player.isUsingItem()) {
                rwTicks++;
            } else {
                rwTicks = 0;
            }
        }
        if (slowTypeSetting.isSelected("Using Item") && mc.player != null && mc.player.isUsingItem()) {
            if (itemMode.isSelected("FTCrossbowOld") && hasCrossbowInHands()) {
                applyMovementCompensation();
            } else if (itemMode.isSelected("New") && isGenericActiveItem()) {
                applyMovementCompensation();
            }
        }
        if (PlayerInventoryComponent.script.isFinished() && MovingUtil.hasPlayerMovement()) {
            script.update();
        }
    }

    @EventHandler
    public void onUsingItem(UsingItemEvent e) {
        if (slowTypeSetting.isSelected("Using Item")) {
            Hand first = mc.player.getActiveHand();
            Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            switch (e.getType()) {
                case EventType.ON -> {
                    switch (itemMode.getSelected()) {
                        case "xyinia" -> {//TODO короче хуйня не нужная, пока оставлю в коде
                            if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                                PlayerIntersectionUtil.interactItem(first);
                                PlayerIntersectionUtil.interactItem(second);
                                e.cancel();
                            }
                        }
                        case "Grim" -> {
                            if (mc.player.getItemUseTime() < 7) {
                                PlayerInventoryUtil.updateSlots();
                                PlayerInventoryUtil.closeScreen(true);
                            } else e.cancel();
                        }
                        case "Test" -> {
                            boolean offhandNotBlockingEatWithMain =
                                    (mc.player.getOffHandStack().getUseAction() != UseAction.BLOCK || mc.player.getActiveHand() != Hand.MAIN_HAND) &&
                                    (mc.player.getOffHandStack().getUseAction() != UseAction.EAT || mc.player.getActiveHand() != Hand.MAIN_HAND);
                            if (offhandNotBlockingEatWithMain) {
                                if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
                                    PlayerIntersectionUtil.interactItem(Hand.OFF_HAND);
                                    e.cancel();
                                } else {
                                    e.cancel();
                                    if (mc.getNetworkHandler() != null && MovingUtil.hasPlayerMovement()) {
                                        int sel = mc.player.getInventory().selectedSlot;
                                        int other = (sel + 1) % 9;
                                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(other));
                                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(sel));
                                    }
                                }
                            }
                        }
                        case "ReallyWorld" -> {
                            if (rwTicks >= 3) {
                                e.cancel();
                                rwTicks = 0;
                            }
                        }

                        case "SpookyTime" -> {
                            cals++;
                            if ((cals % 2) == 0){
                                if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) {
                                    mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                                } else {
                                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                                }
                                    e.cancel();
                                }
                        }

                        case "FunTimeOld" -> {
                            if (mc.player.getActiveHand() == Hand.MAIN_HAND && mc.getNetworkHandler() != null) {
                                int slot = mc.player.getInventory().selectedSlot;
                                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                                e.cancel();
                            }
                        }
                        case "Packet" -> {//TODO короче хуйня не нужная, пока оставлю в коде
                            if (mc.getNetworkHandler() != null) {
                                BlockPos pos = mc.player.getBlockPos().up();
                                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.NORTH));
                                e.cancel();
                            }
                        }
                        case "Advanced" -> {
                            PlayerIntersectionUtil.interactItem(first);
                            PlayerIntersectionUtil.interactItem(second);
                            e.cancel();
                        }
                        case "FTCrossbowOld" -> {
                            if (hasCrossbowInHands()) {
                                e.cancel();
                            }
                        }
                        case "New" -> {//TODO короче хуйня не нужная, пока оставлю в коде
                            if (isGenericActiveItem()) {
                                e.cancel();
                            }
                        }
                    }
                }
                case EventType.POST -> {
                    while (!script.isFinished()) script.update();
                }
            }
        }
    }

    private boolean hasCrossbowInHands() {
        return mc.player != null && (mc.player.getMainHandStack().isOf(Items.CROSSBOW) || mc.player.getOffHandStack().isOf(Items.CROSSBOW));
    }

    private boolean isGenericActiveItem() {
        if (mc.player == null) {
            return false;
        }
        var activeHand = mc.player.getActiveHand();
        if (activeHand == null) {
            return false;
        }
        var stack = mc.player.getStackInHand(activeHand);
        var action = stack.getUseAction();
        return action != UseAction.NONE && action != UseAction.CROSSBOW;
    }

@EventHandler
    public void onEating(MotionEvent e) {
        if (!mc.player.isUsingItem()) {
            cals = 0;
        }
    }

    private void applyMovementCompensation() {
        if (mc.player == null) {
            return;
        }
        boolean isFalling = mc.player.fallDistance > 0.725f;
        var vel = mc.player.getVelocity();
        if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
            if (mc.player.age % 2 == 0) {
                float speedMultiplier = 0.45f;
                mc.player.setVelocity(vel.x * speedMultiplier, vel.y, vel.z * speedMultiplier);
            }
        } else if (isFalling) {
            float speedMultiplier = mc.player.fallDistance > 1.4f ? 0.95f : 0.97f;
            mc.player.setVelocity(vel.x * speedMultiplier, vel.y, vel.z * speedMultiplier);
        }
    }
}

