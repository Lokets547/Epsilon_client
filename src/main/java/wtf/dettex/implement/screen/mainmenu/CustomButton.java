package wtf.dettex.implement.screen.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.mainmenu.render.CustomButtonRenderer;

public class CustomButton extends AbstractCustomButton {
    private float hoverAnim = 0.0f;
    public CustomButton(String name, Runnable action) {
        super(name, action);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
        float target = hovered ? 1.0f : 0.0f;
        // Smoothly approach target
        hoverAnim += (target - hoverAnim) * 0.2f;
        CustomButtonRenderer.render(context, x, y, width, height, Text.of(name), hovered, true, hoverAnim);
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

