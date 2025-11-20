package wtf.dettex.implement.screen.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import wtf.dettex.modules.setting.implement.ColorSetting;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.menu.components.implement.window.AbstractWindow;
import wtf.dettex.implement.screen.menu.components.implement.window.implement.settings.color.ColorWindow;

import static wtf.dettex.api.system.font.Fonts.Type.*;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting setting;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        // Compact height; no detailed description below
        height = 22;

        Fonts.getSize(13, BOLD).drawString(matrix, setting.getName(), x + 9, y + 6, 0xFFD4D6E1);

        blurGlass.render(ShapeProperties.create(matrix, x + width - 14, y + 7, 7, 7)
                .round(3.5F).color(setting.getColor()).build());

        blurGlass.render(ShapeProperties.create(matrix, x + width - 14, y + 7, 7, 7)
                .round(3.5F).thickness(2).softness(1).outlineColor(ColorUtil.getText()).color(0x0FFFFFF).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x + width - 15, y + 6.7F, 7, 7) && button == 0) {
            AbstractWindow existingWindow = null;

            for (AbstractWindow window : windowManager.getWindows()) {
                if (window instanceof ColorWindow) {
                    existingWindow = window;
                    break;
                }
            }

            if (existingWindow != null) {
                windowManager.delete(existingWindow);
            } else {
                AbstractWindow colorWindow = new ColorWindow(setting)
                        .position((int) (mouseX + 185), (int) (mouseY - 82))
                        .size(150, 165)
                        .draggable(true);

                windowManager.add(colorWindow);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

