package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.event.impl.render.EntityColorEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeeInvisible extends Module {
    ValueSetting alphaSetting = new ValueSetting("Alpha", "Player Alpha").setValue(0.5f).range(0.1F, 1);

    public SeeInvisible() {
        super("SeeInvisible", "See Invisible", ModuleCategory.RENDER);
        setup(alphaSetting);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        e.setColor(ColorUtil.multAlpha(e.getColor(), alphaSetting.getValue()));
        e.cancel();
    }

}
