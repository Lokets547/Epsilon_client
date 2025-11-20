package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.vehicle.BoatEntity;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BoatBoost extends Module {

    final ValueSetting horizontalSpeed = new ValueSetting("Horizontal Speed", "Горизонтальная скорость буста")
            .setValue(0.4f)
            .range(0.1f, 4.0f);

    final ValueSetting verticalSpeed = new ValueSetting("Vertical Speed", "Вертикальная скорость буста")
            .setValue(1.3f)
            .range(0.1f, 3.0f);

    boolean wasInBoat;

    public BoatBoost() {
        super("BoatBoost", "Boat Boost", ModuleCategory.MOVEMENT);
        setup(horizontalSpeed, verticalSpeed);
    }

    @Override
    public void activate() {
        wasInBoat = mc.player != null && mc.player.getVehicle() instanceof BoatEntity;
    }

    @Override
    public void deactivate() {
        wasInBoat = false;
    }

    @EventHandler
     
    public void onTick(TickEvent event) {
        if (fullNullCheck()) return;

        boolean isInBoat = mc.player.getVehicle() instanceof BoatEntity;
        if (wasInBoat && !isInBoat) {
            double horizontal = horizontalSpeed.getValue();
            double vertical = verticalSpeed.getValue();
            double yawRad = Math.toRadians(mc.player.getYaw());
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);

            mc.player.setVelocity(-sin * horizontal, vertical, cos * horizontal);
        }

        wasInBoat = isInBoat;
    }
}

