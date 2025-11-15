package wtf.dettex.implement.screen.menu.components.implement.category;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.render.ScissorManager;
import wtf.dettex.implement.screen.menu.components.implement.module.ModuleComponent;
import wtf.dettex.modules.api.ModuleCategory;

public class PanelCategoryComponent extends CategoryComponent {
    private static final float LIST_SPACING = 2.0F;
    private static final float TITLE_HEIGHT = 24.0F;
    private static final float TITLE_PADDING = 6.0F;
    private static final float CONTENT_PADDING = 3.0F;

    public PanelCategoryComponent(ModuleCategory category) {
        super(category);
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        ScissorManager scissorManager = main.getScissorManager();

        int contentHeight = 0;
        for (ModuleComponent component : moduleComponents) {
            contentHeight += component.getComponentHeight() + LIST_SPACING;
        }
        if (!moduleComponents.isEmpty()) {
            contentHeight -= LIST_SPACING;
        }

        height = TITLE_HEIGHT + Math.max(0, contentHeight) + CONTENT_PADDING * 2;

        blurGlass.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(4)
                .thickness(2)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(0.65F))
                .build());

        String title = category.getReadableName();
        var titleFont = Fonts.getSize(20, Fonts.Type.BOLD);
        float titleWidth = titleFont.getStringWidth(title);
        float titleX = x + (width - titleWidth) / 2.0F;
        float titleY = y + TITLE_PADDING;
        titleFont.drawString(matrices, title, (int) titleX, (int) titleY, ColorUtil.getText());

        float listX = x + CONTENT_PADDING;
        float listY = y + TITLE_HEIGHT;
        float listW = width - CONTENT_PADDING * 2;
        float listH = height - TITLE_HEIGHT - CONTENT_PADDING;

        scissorManager.push(positionMatrix, listX, listY, listW, listH);

        float offset = 0.0F;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset;
            component.width = (int) listW;
            component.render(context, mouseX, mouseY, delta);
            offset += compHeight + LIST_SPACING;
        }

        scissorManager.pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layoutModules();
        moduleComponents.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        layoutModules();
        moduleComponents.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        layoutModules();
        moduleComponents.forEach(component -> component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        layoutModules();
        moduleComponents.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        layoutModules();
        moduleComponents.forEach(component -> component.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    private void layoutModules() {
        float listX = x + CONTENT_PADDING;
        float listY = y + TITLE_HEIGHT;
        float listW = width - CONTENT_PADDING * 2;

        float offset = 0.0F;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset;
            component.width = (int) listW;
            offset += compHeight + LIST_SPACING;
        }
    }
}
