package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.MathHelper;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.event.impl.player.SwimmingEvent;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.common.util.other.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WaterSpeed extends Module {

    SelectSetting modeSetting = new SelectSetting("Mode", "Select bypass mode").value("Old", "FunTime");
    ValueSetting funTimeBoost = new ValueSetting("FunTime Boost", "Скорость буста FunTime").setValue(1.15f).range(1.0f, 2.0f)
            .visible(() -> modeSetting.isSelected("FunTime"));
    private final StopWatch toggleWatch = new StopWatch();
    private final StopWatch time = new StopWatch();
    @NonFinal private boolean pressJumpPhase = true;

    public WaterSpeed() {
        super("WaterSpeed", "Water Speed", ModuleCategory.MOVEMENT);
        setup(modeSetting, funTimeBoost);
    }

    @Override
    public void deactivate() {
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (modeSetting.isSelected("Old")) {
            boolean inWater = mc.player.isTouchingWater() || mc.player.isSwimming();

            if (inWater) {
                if (toggleWatch.every(100)) {
                    pressJumpPhase = !pressJumpPhase;
                }

                mc.options.jumpKey.setPressed(pressJumpPhase);
                mc.options.sneakKey.setPressed(!pressJumpPhase);

                if (mc.player.isOnGround()) {
                    mc.player.jump();
                    mc.player.velocity.y = 0.1;
                }
            } else {
                mc.options.jumpKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
            }
        } else if (modeSetting.isSelected("FunTime")) {
            if ((mc.world.getBlockState(mc.player.getBlockPos().up()).getBlock() != Blocks.WATER && mc.options.jumpKey.isPressed()) || !mc.player.isTouchingWater()) {
                time.reset();
            }

            if (mc.player.isTouchingWater() && time.finished(160)) {
                float ySpeed = mc.options.jumpKey.isPressed() ? 0.05f : mc.options.sneakKey.isPressed() ? -0.05f : !mc.player.isSprinting() ? 0.005f : 0;
                float boost = funTimeBoost.getValue();
                float walkBoost = MathHelper.lerp(MathHelper.clamp(boost - 1.0f, 0.0f, 1.0f), 1.02f, boost);

                if (mc.player.isSprinting()) {
                    mc.player.setVelocity(mc.player.getVelocity().x * boost, mc.player.getVelocity().y + ySpeed, mc.player.getVelocity().z * boost);
                } else {
                    mc.player.setVelocity(mc.player.getVelocity().x * walkBoost, mc.player.getVelocity().y + ySpeed, mc.player.getVelocity().z * walkBoost);
                }
            }
        }
    }

    @EventHandler
    public void onSwimming(SwimmingEvent e) {
        if (modeSetting.isSelected("Old")) {
            if (mc.options.jumpKey.isPressed()) {
                float pitch = RotationController.INSTANCE.getRotation().getPitch();
                float boost = pitch >= 0 ? MathHelper.clamp(pitch / 45, 1, 2) : 1;
                e.getVector().y = 1 * boost;
            } else if (mc.options.sneakKey.isPressed()) {
                e.getVector().y = -0.8;
            }
        }
    }
}

