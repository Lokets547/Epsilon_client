package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.impl.player.FireworkEvent;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraFly extends Module {
    StopWatch stopWatch = new StopWatch(), swapWatch = new StopWatch();
    Script script = new Script();

    SelectSetting flyModeSetting = new SelectSetting("Fly Mode", "Selects the type of mode")
            .value("FireWork Abuse", "SpookyTime");

    @NonFinal long speedRampStartTime = 0;
    @NonFinal boolean isSpeedRamping = false;
    @NonFinal boolean adjustedPitch = false;

    public ElytraFly() {
        super("ElytraFly", "Elytra Fly", ModuleCategory.MOVEMENT);
        setup(flyModeSetting);
    }

    @EventHandler
    public void onFireWork(FireworkEvent e) {
        stopWatch.reset();
    }

    
    @EventHandler
    
    public void onTick(TickEvent e) {
        if (mc.player == null) return;
        if (flyModeSetting.isSelected("FireWork Abuse")) {
            Slot elytra = PlayerInventoryUtil.getSlot(Items.ELYTRA);
            Slot fireWork = PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET);
            if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && elytra != null && fireWork != null && script.isFinished()) {
                if (stopWatch.finished(100)) {
                    int ticks = mc.player.isOnGround() ? 2 : 0;
                    if (ticks != 0) mc.player.jump();
                    script.cleanup().addTickStep(ticks, () -> {
                        PlayerInventoryUtil.moveItem(elytra, 6, false);
                        PlayerIntersectionUtil.startFallFlying();
                        PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET, AngleUtil.cameraAngle(), false);
                        PlayerInventoryUtil.moveItem(elytra, 6, false);
                        PlayerInventoryUtil.closeScreen(true);
                        stopWatch.setMs(-500);
                    });
                }
            }
        } else if (flyModeSetting.isSelected("SpookyTime")) {
            applySpookyTime();
        }
        script.update();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (mc.player != null) {
            mc.player.setVelocity(Vec3d.ZERO);
        }
        isSpeedRamping = false;
        adjustedPitch = false;
    }

    private void applySpookyTime() {
        if (!mc.player.isGliding()) {
            if (mc.options.jumpKey.isPressed()) {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                } else if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    PlayerIntersectionUtil.startFallFlying();
                }
            }
            isSpeedRamping = false;
            adjustedPitch = false;
            return;
        }

        Vec3d velocity = mc.player.getVelocity();
        boolean verticalBoost = velocity.y > 0.08 || mc.player.fallDistance > 0.1f;
        boolean minimalHorizontal = Math.abs(velocity.x) <= 0.01 && Math.abs(velocity.z) <= 0.01;

        if (verticalBoost && minimalHorizontal) {
            mc.player.setVelocity(0.0, velocity.y, 0.0);

            float pitch = mc.player.getPitch();
            float effectivePitch = MathHelper.clamp(pitch, -25.0f, 25.0f);

            if (!isSpeedRamping) {
                speedRampStartTime = System.currentTimeMillis();
                isSpeedRamping = true;
                if (!adjustedPitch && Math.abs(pitch - effectivePitch) > 1.0E-3f) {
                    mc.player.setPitch(effectivePitch);
                }
                adjustedPitch = true;
            }

            long rampDuration = 100L;
            long elapsed = System.currentTimeMillis() - speedRampStartTime;
            float progress = Math.min(elapsed / (float) rampDuration, 1.0f);
            double currentBaseSpeed = 0.05 * progress;

            double maxAddedSpeed = 0.06;
            double maxVerticalSpeed = 1.11;

            float normalizedPitch = effectivePitch / 90.0f;
            double speedAddition = maxAddedSpeed * normalizedPitch * normalizedPitch;

            double speed = currentBaseSpeed + speedAddition;
            double newY = velocity.y + speed;
            if (newY >= maxVerticalSpeed) {
                newY = maxVerticalSpeed;
            }

            mc.player.setVelocity(velocity.x, newY, velocity.z);
        } else {
            isSpeedRamping = false;
            adjustedPitch = false;
        }
    }
}

