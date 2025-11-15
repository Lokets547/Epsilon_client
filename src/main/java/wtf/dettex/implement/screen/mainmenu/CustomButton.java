package wtf.dettex.implement.screen.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.mainmenu.render.CustomButtonRenderer;

public class CustomButton extends AbstractCustomButton {
    public CustomButton(String name, Runnable action) {
        super(name, action);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
        CustomButtonRenderer.render(context, x, y, width, height, Text.of(name), hovered, true, 1.0F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            action.run();
            return true;
        }
        return false;
    }
}
