package wtf.dettex.modules.impl.combat.killaura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryComponent;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.entity.SimulatedPlayer;
import wtf.dettex.event.types.EventType;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.entity.*;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.listener.impl.EventListener;
import wtf.dettex.event.impl.item.UsingItemEvent;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.modules.impl.combat.Criticals;
import wtf.dettex.modules.impl.combat.killaura.rotation.*;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RaytracingUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.impl.movement.AutoSprint;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttackHandler implements QuickImports {
    private final StopWatch attackTimer = new StopWatch(), shieldWatch = new StopWatch();
    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final StopWatch legitSprintTimer = new StopWatch();
    private int count = 0;
    private boolean sprintResetActive = false;
    private int sprintResetTicks = 2;

    void tick() {
        if (mc.player == null) return;

        if (sprintResetActive) {
            // Принудительно отключаем спринт каждый тик пока активен сброс
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }

            int msDelay = sprintResetTicks * 50;
            if (legitSprintTimer.finished(msDelay)) {
                if (AutoSprint.getInstance().isState()) {
                    mc.player.setSprinting(true);
                }
                sprintResetActive = false;
            }
        }
    }

    void reset() {
        sprintResetActive = false;
    }

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }


    void handleAttack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        if (canAttack(config, 1)) preAttackEntity(config);

        boolean blockDueToSprint;
        // Sprint Reset всегда включен: блокируем атаку, если ещё не сброшен спринт
        blockDueToSprint = !sprintResetActive && mc.player.isSprinting() && !mc.player.isGliding() && !mc.player.isTouchingWater();

        if (RaytracingUtil.rayTrace(config) && canAttack(config, 0) && !blockDueToSprint) {
            attackEntity(config);
        }
    }

    void preAttackEntity(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }

        if (!mc.player.isSwimming()) {
            boolean wasSprinting = mc.player.isSprinting();
            if (wasSprinting) {
                String sprintResetMode = Aura.getInstance().getSprintResetMode().getSelected();
                sprintResetTicks = switch (sprintResetMode) {
                    case "Rage" -> 1;
                    case "Normal" -> 2;
                    case "Legit" -> 3;
                    default -> 2;
                };

                // Синхронизация с AutoSprint - блокируем автоспринт на нужное количество тиков
                if (AutoSprint.getInstance().isState()) {
                    AutoSprint.getInstance().tickStop = sprintResetTicks;
                }

                mc.player.setSprinting(false);
                legitSprintTimer.reset();
                sprintResetActive = true;
            }
        }
    }


    void attackEntity(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        attack(config);
        breakShield(config);
        attackTimer.reset();
        count++;
    }

    private void breakShield(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        LivingEntity target = config.getTarget();
        Angle angleToPlayer = AngleUtil.fromVec3d(mc.player.getBoundingBox().getCenter().subtract(target.getEyePos()));
        boolean targetOnShield = target.isUsingItem() && target.getActiveItem().getItem().equals(Items.SHIELD);
        boolean angle = Math.abs(RotationController.computeAngleDifference(target.getYaw(), angleToPlayer.getYaw())) < 90;
        Slot axe = PlayerInventoryUtil.getSlot(s -> s.getStack().getItem() instanceof AxeItem);

        if (config.isShouldBreakShield() && targetOnShield && axe != null && angle && PlayerInventoryComponent.script.isFinished()) {
            PlayerInventoryUtil.swapHand(axe, Hand.MAIN_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
            attack(config);
            PlayerInventoryUtil.swapHand(axe, Hand.MAIN_HAND, false, true);
            PlayerInventoryUtil.closeScreen(true);
        }
    }

    private void attack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        mc.interactionManager.attackEntity(mc.player, config.getTarget());
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isSprinting() {
        return EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
    }

    public boolean canAttack(AttackPerpetrator.AttackPerpetratorConfigurable config, int ticks) {
        for (int i = 0; i <= ticks; i++) {
            if (canCrit(config, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCrit(AttackPerpetrator.AttackPerpetratorConfigurable config, int ticks) {
        if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(config.isUseDynamicCooldown(), ticks)) {
            return false;
        }

        SimulatedPlayer simulated = SimulatedPlayer.simulateLocalPlayer(ticks);
        boolean hasRestrictions = hasMovementRestrictions(simulated);
        boolean critState = isPlayerInCriticalState(simulated, ticks);
        boolean jumpHeld = mc.options.jumpKey.isPressed();

        if (config.isOnlyCritical()) {
            if (config.isOnlySpaceCrits()) {
                if (jumpHeld) {
                    if (hasRestrictions) {
                        return false;
                    }
                    return critState;
                }

                return true;
            }

            if (hasRestrictions) {
                return false;
            }
            return critState;
        }

        return true;
    }

    private boolean hasMovementRestrictions(SimulatedPlayer simulated) {
        return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulated.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerIntersectionUtil.isBoxInBlock(simulated.boundingBox.expand(-1e-3), Blocks.COBWEB)
                || simulated.touchingWater
                || simulated.isSwimming
                || simulated.isSubmergedInWater()
                || simulated.isInLava()
                || simulated.isClimbing()
                || !PlayerIntersectionUtil.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
                || simulated.player.getAbilities().flying;
    }

    private boolean isPlayerInCriticalState(SimulatedPlayer simulated, int ticks) {
        boolean fall = simulated.fallDistance > 0 && (simulated.fallDistance < 0.08 || !SimulatedPlayer.simulateLocalPlayer(ticks + 1).onGround);
        return !simulated.onGround && (fall || Criticals.getInstance().isState());
    }
}
