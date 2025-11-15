package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.time.TimerUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.event.impl.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Timer extends Module {

    final SelectSetting mode = new SelectSetting("Mode", "Режим работы таймера")
            .value("Обычный", "Grim").selected("Обычный");

    final ValueSetting timerAmount = new ValueSetting("Скорость", "Множитель скорости таймера")
            .setValue(2.0F).range(1.0F, 5.0F);

    final BooleanSetting smart = new BooleanSetting("Умный", "Умное управление скоростью")
            .setValue(true).visible(() -> !mode.isSelected("Grim"));

    final BooleanSetting movingUp = new BooleanSetting("Добавлять в движении", "Увеличивать скорость при движении")
            .setValue(false).visible(() -> !mode.isSelected("Grim"));

    final ValueSetting upValue = new ValueSetting("Значение", "Значение увеличения при движении")
            .setValue(0.02F).range(0.01F, 0.5F).visible(() -> movingUp.isValue());

    final ValueSetting ticks = new ValueSetting("Скорость убывания", "Скорость убывания нарушений")
            .setValue(1.0F).range(0.15F, 3.0F).visible(() -> !mode.isSelected("Grim"));

    final float maxViolation = 100.0F;
    float violation = 0.0F;
    double prevPosX;
    double prevPosY;
    double prevPosZ;
    float yaw;
    float pitch;
    TimerUtil timerUtil = new TimerUtil();

    public Timer() {
        super("Timer", "Timer", ModuleCategory.MOVEMENT);
        setup(mode, timerAmount, smart, movingUp, upValue, ticks);
    }

    public static Timer getInstance() {
        return Instance.get(Timer.class);
    }

    public static float getActiveMultiplier() {
        Timer t = getInstance();
        if (t == null || !t.state) return 1.0F;
        if (t.mode.isSelected("Grim")) return 1.0F;
        return Math.max(0.1F, t.timerAmount.getValue());
    }

    @Override
    public void activate() {
        reset();
        super.activate();
    }

    @Override
    public void deactivate() {
        reset();
        if (mc.world != null) {
            mc.world.getTickManager().setTickRate(20.0F);
        }
        timerUtil.reset();
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (timerUtil.hasTimeElapsed(25000L)) {
            reset();
            timerUtil.reset();
        }

        if (!mc.player.isOnGround()) {
            violation += 0.1F;
            violation = MathHelper.clamp(violation, 0.0F, maxViolation / (mode.isSelected("Grim") ? 1.0F : timerAmount.getValue()));
        }

        if (!mode.isSelected("Grim")) {
            float targetSpeed = timerAmount.getValue();
            mc.world.getTickManager().setTickRate(20.0F * targetSpeed);

            if (smart.isValue() && targetSpeed > 1.0F) {
                if (violation < maxViolation / timerAmount.getValue()) {
                    violation += mode.isSelected("Grim") ? 0.05F : ticks.getValue();
                    violation = MathHelper.clamp(violation, 0.0F, maxViolation / (mode.isSelected("Grim") ? 1.0F : timerAmount.getValue()));
                } else {
                    resetSpeed();
                }
            }
        }

        if (mode.isSelected("Grim")) {
            updateTimer(mc.player.getYaw(), mc.player.getPitch(), mc.player.getX(), mc.player.getY(), mc.player.getZ());
            float target = Math.max(1.0F, timerAmount.getValue());
            float budget = Math.max(0.0F, maxViolation - violation) / maxViolation; // 0..1
            float mul = 1.0F + (target - 1.0F) * budget;
            if (mc.world != null) {
                mc.world.getTickManager().setTickRate(20.0F * mul);
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!mode.isSelected("Grim")) return;

        if (e.getType() == PacketEvent.Type.RECEIVE) {
            if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                resetSpeed();
                reset();
            }

            if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
                if (packet.getEntityId() == mc.player.getId()) {
                    reset();
                    resetSpeed();
                }
            }
        }

    }

    public void updateTimer(float yaw, float pitch, double posX, double posY, double posZ) {
        if (notMoving()) {
            if (mode.isSelected("Grim")) {
                violation = (float)((double)violation - 0.05000000074505806D);
            } else {
                violation = (float)((double)violation - ((double)ticks.getValue() + 0.4D));
            }
        } else if (movingUp.isValue() && !mode.isSelected("Grim")) {
            violation -= upValue.getValue();
        }

        violation = (float)MathHelper.clamp((double)violation, 0.0D, Math.floor((double)maxViolation));
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    private boolean notMoving() {
        return prevPosX == mc.player.getX() &&
                prevPosY == mc.player.getY() &&
                prevPosZ == mc.player.getZ() &&
                yaw == mc.player.getYaw() &&
                pitch == mc.player.getPitch();
    }

    public float getViolation() {
        return violation;
    }

    public boolean isGrim() {
        return mode.isSelected("Grim");
    }

    public float getTimerAmountValue() {
        return timerAmount.getValue();
    }

    public float getMaxViolation() {
        return maxViolation;
    }

    public void resetSpeed() {
        if (mc.world != null) {
            mc.world.getTickManager().setTickRate(20.0F);
        }
    }

    public void reset() {
        if (mode.isSelected("Grim")) {
            violation = 0.0F;
        }
    }
}
