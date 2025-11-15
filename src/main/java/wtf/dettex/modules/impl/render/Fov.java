package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.GroupSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.event.impl.render.FovEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fov extends Module {
    final GroupSetting fovGroup = new GroupSetting("FOV", "Кастомный угол обзора").setValue(true);
    final ValueSetting customFov = new ValueSetting("Value", "Желаемый FOV").setValue(100f).range(30f, 150f);
    final BooleanSetting overrideSprintFov = new BooleanSetting("Disable Sprint FOV", "Убрать увеличение FOV при беге").setValue(true);

    public Fov() {
        super("Fov", "FOV Changer", ModuleCategory.RENDER);
        setup(fovGroup.settings(customFov), overrideSprintFov);
    }

    @EventHandler
    public void onFov(FovEvent e) {
        if (!fovGroup.isValue()) return;
        float base = customFov.getValue();
        if (overrideSprintFov.isValue()) {
            // Игнорируем vanilla увеличение FOV при спринте
            e.setFov((int) base);
        } else {
            // Сохраняем ванильное поведение спринта: добавим небольшой буст при беге
            boolean sprinting = mc.player != null && mc.player.isSprinting();
            float result = base + (sprinting ? 5f : 0f);
            e.setFov((int) result);
        }
        e.cancel();
    }
}
