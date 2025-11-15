package wtf.dettex.modules.impl.render;

import lombok.Getter;
import lombok.experimental.Accessors;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.SelectSetting;

@Getter
@Accessors(fluent = true)
public class ClickGUI extends Module {
    private static ClickGUI instance;

    private final SelectSetting mode = new SelectSetting("ClickGUI style", "ClickGUI style")
            .value("Panels", "DropDown")
            .selected("Panels");

    public ClickGUI() {
        super("ClickGUI", ModuleCategory.RENDER);
        instance = this;
        setup(mode);
    }

    public static ClickGUI getInstance() {
        return instance;
    }
}
