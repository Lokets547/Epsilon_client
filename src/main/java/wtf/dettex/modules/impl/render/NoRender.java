package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.common.util.other.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NoRender extends Module {
    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Elements", "Select elements to be ignored")
            .value("Fire", "Bad Effects", "Block Overlay", "Hurt Cam");

    public NoRender() {
        super("NoRender","No Render",ModuleCategory.RENDER);
        setup(modeSetting);
    }

}

