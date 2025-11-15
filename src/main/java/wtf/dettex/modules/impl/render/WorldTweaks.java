package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.render.FogEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorldTweaks extends Module {
    public static WorldTweaks getInstance() {
        return Instance.get(WorldTweaks.class);
    }

    public final MultiSelectSetting modeSetting = new MultiSelectSetting("World Setting", "Allows you to customize world")
            .value("Bright", "Time", "Fog", "Silent Hill");

    public final SelectSetting fogType = new SelectSetting("Fog Type", "Type of fog effect")
            .value("Normal", "Dense", "Silent Hill").selected("Normal")
            .visible(() -> modeSetting.isSelected("Fog") || modeSetting.isSelected("Silent Hill"));

    public final ValueSetting brightSetting = new ValueSetting("Bright", "Sets the value of the maximum bright")
            .setValue(1.0F).range(0.0F, 1.0F).visible(() -> modeSetting.isSelected("Bright"));

    public final ValueSetting timeSetting = new ValueSetting("Time", "Sets the value of the time")
            .setValue(12).range(0, 24).visible(() -> modeSetting.isSelected("Time"));

    public final ValueSetting distanceSetting = new ValueSetting("Fog Distance", "Sets the fog distance")
            .setValue(100).range(5, 200).visible(() -> modeSetting.isSelected("Fog") && !fogType.isSelected("Silent Hill"));

    public WorldTweaks() {
        super("WorldTweaks", "World Tweaks", ModuleCategory.RENDER);
        setup(modeSetting, fogType, brightSetting, timeSetting, distanceSetting);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @EventHandler
    public void onFog(FogEvent e) {
        if (modeSetting.isSelected("Fog") || modeSetting.isSelected("Silent Hill")) {
            String type = fogType.getSelected();
            switch (type) {
                case "Normal" -> {
                    e.setDistance(distanceSetting.getValue());
                    e.setColor(ColorUtil.getClientColor());
                }
                case "Dense" -> {
                    e.setDistance(Math.min(30, distanceSetting.getValue()));
                    e.setColor(0xFF404040); // dark gray
                }
                case "Silent Hill" -> {
                    e.setDistance(8); // very dense fog
                    e.setColor(0xFF2A2A2A); // dark gray-brown
                }
            }
            e.cancel();
        }
    }
}
