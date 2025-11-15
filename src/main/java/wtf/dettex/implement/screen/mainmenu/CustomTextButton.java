package wtf.dettex.implement.screen.mainmenu;

import net.minecraft.client.gui.DrawContext;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.common.util.math.MathUtil;

public class CustomTextButton extends AbstractCustomButton {
    public CustomTextButton(String name, Runnable action) {
        super(name, action);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float textWidth = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringWidth(name);
        float textHeight = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringHeight(name);

        width = (int) Math.ceil(textWidth);
        height = (int) Math.ceil(textHeight);

        Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), name, x, y, -1);
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
