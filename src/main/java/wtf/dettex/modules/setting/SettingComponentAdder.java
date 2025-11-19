package wtf.dettex.modules.setting;

import wtf.dettex.modules.setting.implement.*;
import wtf.dettex.implement.screen.menu.components.implement.settings.*;
import wtf.dettex.implement.screen.menu.components.implement.settings.multiselect.DDMultiSelectComponent;
import wtf.dettex.implement.screen.menu.components.implement.settings.multiselect.MultiSelectComponent;
import wtf.dettex.implement.screen.menu.components.implement.settings.select.DDSelectComponent;
import wtf.dettex.implement.screen.menu.components.implement.settings.select.SelectComponent;

import java.util.List;

public class SettingComponentAdder {
    private final boolean dropdown;

    public SettingComponentAdder() {
        this.dropdown = true; // Force DropDown layout only
    }

    public void addSettingComponent(List<Setting> settings, List<AbstractSettingComponent> components) {
        settings.forEach(setting -> {
            if (setting instanceof BooleanSetting booleanSetting) {
                components.add(dropdown
                        ? new DDCheckBoxComponent(booleanSetting)
                        : new CheckboxComponent(booleanSetting));
            }

            if (setting instanceof BindSetting bindSetting) {
                components.add(dropdown
                        ? new DDBindComponent(bindSetting)
                        : new BindComponent(bindSetting));
            }

            if (setting instanceof ColorSetting colorSetting) {
                components.add(dropdown
                        ? new DDColorComponent(colorSetting)
                        : new ColorComponent(colorSetting));
            }

            if (setting instanceof TextSetting textSetting) {
                components.add(dropdown
                        ? new DDTextComponent(textSetting)
                        : new TextComponent(textSetting));
            }

            if (setting instanceof ValueSetting valueSetting) {
                components.add(dropdown
                        ? new DDValueComponent(valueSetting)
                        : new ValueComponent(valueSetting));
            }

            if (setting instanceof GroupSetting groupSetting) {
                components.add(dropdown
                        ? new DDGroupComponent(groupSetting)
                        : new GroupComponent(groupSetting));
            }

            if (setting instanceof ButtonSetting buttonSetting) {
                components.add(dropdown
                        ? new DDSButtonComponent(buttonSetting)
                        : new SButtonComponent(buttonSetting));
            }

            if (setting instanceof SelectSetting selectSetting) {
                components.add(dropdown
                        ? new DDSelectComponent(selectSetting)
                        : new SelectComponent(selectSetting));
            }

            if (setting instanceof MultiSelectSetting multiSelectSetting) {
                components.add(dropdown
                        ? new DDMultiSelectComponent(multiSelectSetting)
                        : new MultiSelectComponent(multiSelectSetting));
            }
        });
    }
}
