package wtf.dettex.modules.impl.misc;

import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.other.Instance;

public class IRC extends Module {
    public static Module getInstance() {
        return Instance.get(String.valueOf(IRC.class));
    }


    public IRC() {
        super("IRC", "IRC", ModuleCategory.MISC);
    }

    public void onEnable() {

    }
}
