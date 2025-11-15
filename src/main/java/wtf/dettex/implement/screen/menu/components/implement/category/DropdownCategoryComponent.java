package wtf.dettex.implement.screen.menu.components.implement.category;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.render.ScissorManager;
import wtf.dettex.implement.screen.menu.components.implement.module.ModuleComponent;
import wtf.dettex.modules.api.ModuleCategory;

public class DropdownCategoryComponent extends CategoryComponent {
    private static final float HEADER_HEIGHT = 20.0F;
    private static final float PANEL_HEIGHT = 260.0F;
    private static final float CONTENT_HORIZONTAL_PADDING = 3.0F;
    private static final float CONTENT_VERTICAL_PADDING = 2.0F;
    private static final float BOTTOM_PADDING = 3.0F;
    private static final double SCROLL_SMOOTHING = 0.18;
    private static final double SCROLL_STEP = 5.0;

    private double maxScroll = 0.0;

    public DropdownCategoryComponent(ModuleCategory category) {
        super(category);
        height = PANEL_HEIGHT;
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        ScissorManager scissorManager = main.getScissorManager();

        float contentHeight = calculateContentHeight();
        float viewportHeight = getViewportHeight();
        maxScroll = Math.max(0.0F, contentHeight - viewportHeight);

        scroll = MathHelper.clamp(scroll, 0.0, maxScroll);
        smoothedScroll = MathHelper.clamp(smoothedScroll + (scroll - smoothedScroll) * SCROLL_SMOOTHING, 0.0, maxScroll);

        // Panel background
        blurGlass.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(6)
                .thickness(2)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(0.5F))
                .build());

        // Category title centered at top
        String title = category.getReadableName();
        var titleFont = Fonts.getSize(20, Fonts.Type.BOLD);
        float titleWidth = titleFont.getStringWidth(title);
        float titleX = x + (width - titleWidth) / 2.0F;
        float titleY = y + 6.0F;
        titleFont.drawString(matrices, title, (int) titleX, (int) titleY, ColorUtil.getText());

        // Content area with clipping
        float listX = x + CONTENT_HORIZONTAL_PADDING;
        float listY = y + HEADER_HEIGHT;
        float listW = width - CONTENT_HORIZONTAL_PADDING * 2.0F;
        float listH = Math.max(0.0F, viewportHeight - BOTTOM_PADDING);

        if (listH <= 0.0F) {
            return;
        }

        scissorManager.push(positionMatrix, listX, listY, listW, listH);

        float smoothingOffset = (float) smoothedScroll;
        float contentY = listY - smoothingOffset;
        float offset = 0.0F;
        float bottomLimit = listY + listH;
        for (ModuleComponent component : moduleComponents) {
            int componentHeight = component.getComponentHeight();
            component.x = listX;
            component.setExternalClip(listY, bottomLimit);
            component.y = contentY + offset;
            component.width = (int) listW;

            if (component.y >= bottomLimit) {
                break;
            }

            component.render(context, mouseX, mouseY, delta);

            offset += componentHeight + CONTENT_VERTICAL_PADDING;
        }
        scissorManager.pop();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isHover(mouseX, mouseY)) {
            return false;
        }
        float listY = y + HEADER_HEIGHT;
        float listH = Math.max(0.0F, getViewportHeight() - BOTTOM_PADDING);
        if (!MathUtil.isHovered(mouseX, mouseY, x, listY, width, listH)) {
            return false;
        }

        if (maxScroll <= 0.0) {
            return false;
        }

        scroll = MathHelper.clamp(scroll - amount * SCROLL_STEP, 0.0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layoutModulesForInteraction();
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        layoutModulesForInteraction();
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        layoutModulesForInteraction();
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        layoutModulesForInteraction();
        moduleComponents.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        layoutModulesForInteraction();
        moduleComponents.forEach(component -> component.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    private void layoutModulesForInteraction() {
        float listX = x + CONTENT_HORIZONTAL_PADDING;
        float smoothingOffset = (float) smoothedScroll;
        float listY = y + HEADER_HEIGHT - smoothingOffset;
        float listW = width - CONTENT_HORIZONTAL_PADDING * 2.0F;

        float offset = 0.0F;
        float clipTop = y + HEADER_HEIGHT;
        float clipBottom = clipTop + Math.max(0.0F, getViewportHeight() - BOTTOM_PADDING);
        for (ModuleComponent component : moduleComponents) {
            int componentHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset;
            component.width = (int) listW;
            component.setExternalClip(clipTop, clipBottom);
            offset += componentHeight + CONTENT_VERTICAL_PADDING;
        }
    }

    private float calculateContentHeight() {
        float total = BOTTOM_PADDING;
        for (ModuleComponent component : moduleComponents) {
            total += component.getComponentHeight() + CONTENT_VERTICAL_PADDING;
        }
        return total;
    }

    private float getViewportHeight() {
        return PANEL_HEIGHT - HEADER_HEIGHT;
    }
}
