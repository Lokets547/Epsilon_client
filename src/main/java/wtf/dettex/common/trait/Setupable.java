package wtf.dettex.common.trait;

import wtf.dettex.modules.setting.Setting;

public interface Setupable {
    void setup(Setting... settings);
}