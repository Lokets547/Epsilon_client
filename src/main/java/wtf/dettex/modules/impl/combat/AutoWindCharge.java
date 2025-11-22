package wtf.dettex.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.item.ItemUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.task.TaskPriority;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.setting.implement.*;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.AngleUtil;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationConfig;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.setting.implement.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoWindCharge extends Module {
    SelectSetting mode = new SelectSetting("Mode", "Режим работы")
            .value("Bind", "Auto", "Both").selected("Both");
    BindSetting throwBind = new BindSetting("Throw Bind", "Кинуть заряд ветра по бинду");

    ValueSetting autoDelay = new ValueSetting("Auto Delay", "Задержка между бросками в авто-режиме, мс")
            .setValue(350F).range(100F, 2000F).visible(() -> !mode.isSelected("Bind"));
    ValueSetting pitchDown = new ValueSetting("Pitch Down", "Угол вниз для броска под себя")
            .setValue(85F).range(60F, 90F);
    BooleanSetting rotateBeforeThrow = new BooleanSetting("Rotate", "Поворачивать камеру перед броском")
            .setValue(true);

    GroupSetting settings = new GroupSetting("Wind Charge", "Настройки AutoWindCharge")
            .settings(mode, throwBind, autoDelay, pitchDown, rotateBeforeThrow).setValue(true);

    StopWatch watch = new StopWatch();

    public AutoWindCharge() {
        super("AutoWindCharge", "Auto Wind Charge", ModuleCategory.COMBAT);
        setup(settings);
    }

    @EventHandler

    public void onKey(KeyEvent e) {
        if (mode.isSelected("Bind") || mode.isSelected("Both")) {
            if (e.isKeyDown(throwBind.getKey())) {
                if (watch.finished(150)) {
                    tryThrow(true);
                }
            }
        }
    }

    @EventHandler
    
    public void onTick(TickEvent e) {
        if (mode.isSelected("Auto") || mode.isSelected("Both")) {
            if (watch.finished((long) autoDelay.getValue())) {
                tryThrow(false);
            }
        }
    }

    private void tryThrow(boolean fromBind) {
        if (mc == null || mc.player == null) return;
        if (ItemUtil.getCooldownProgress(Items.WIND_CHARGE) > 0) return;
        if (PlayerInventoryUtil.getSlot(Items.WIND_CHARGE) == null) return;

        Angle angle = AngleUtil.pitch(pitchDown.getValue());
        if (rotateBeforeThrow.isValue()) {
            RotationController.INSTANCE.rotateTo(angle, new RotationConfig(true, false), TaskPriority.HIGH_IMPORTANCE_2, this);
        }
        PlayerInventoryUtil.swapAndUse(Items.WIND_CHARGE, angle, true);
        watch.reset();
    }
}

