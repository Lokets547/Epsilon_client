package wtf.dettex.implement.screen.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.implement.screen.menu.components.implement.other.CheckComponent;

import static wtf.dettex.api.system.font.Fonts.Type.BOLD;

public class CheckboxComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final BooleanSetting setting;

    public CheckboxComponent(BooleanSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Compact height; no detailed description below
        height = 22;

        Fonts.getSize(13, BOLD).drawString(context.getMatrices(), setting.getName(), x + 9, y + 9, 0xFFD4D6E1);

        ((CheckComponent) checkComponent.position(x + width - 16, y + 3.0F))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        checkComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
