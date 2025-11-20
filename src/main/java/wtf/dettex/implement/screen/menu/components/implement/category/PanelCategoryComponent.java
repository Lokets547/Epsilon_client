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

public class PanelCategoryComponent extends CategoryComponent {
    private static final float LIST_SPACING = 2.0F;
    private static final float TITLE_HEIGHT = 24.0F;
    private static final float TITLE_PADDING = 6.0F;
    private static final float CONTENT_PADDING = 3.0F;
    private static final float PANEL_HEIGHT = 280.0F;
    private static final double SCROLL_SMOOTHING = 0.18;
    private static final double SCROLL_STEP = 5.0;
    private static final float SCROLLBAR_WIDTH = 3.0F;
    private static final float SCROLLBAR_PADDING = 2.0F;

    private double maxScroll = 0.0;
    private boolean draggingScrollbar = false;
    private double dragStartY = 0.0;
    private double scrollAtDragStart = 0.0;

    public PanelCategoryComponent(ModuleCategory category) {
        super(category);
        height = PANEL_HEIGHT;
    }

    @Override
    protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        ScissorManager scissorManager = main.getScissorManager();

        float contentHeight = calculateContentHeight();
        float viewportHeight = PANEL_HEIGHT - TITLE_HEIGHT - CONTENT_PADDING;
        maxScroll = Math.max(0.0F, contentHeight - viewportHeight);

        scroll = MathHelper.clamp(scroll, 0.0, maxScroll);
        smoothedScroll = MathHelper.clamp(smoothedScroll + (scroll - smoothedScroll) * SCROLL_SMOOTHING, 0.0, maxScroll);

        blurGlass.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(12)
                .thickness(1)
                .outlineColor(ColorUtil.BLACK)
                .color(ColorUtil.getRect(0.3F))
                .build());

        String title = category.getReadableName();
        var titleFont = Fonts.getSize(20, Fonts.Type.BOLD);
        float titleWidth = titleFont.getStringWidth(title);
        float titleX = x + (width - titleWidth) / 2.0F;
        float titleY = y + TITLE_PADDING;
        titleFont.drawString(matrices, title, (int) titleX, (int) titleY, ColorUtil.getText());

        float listX = x + CONTENT_PADDING;
        float listY = y + TITLE_HEIGHT;
        float listW = width - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        float listH = viewportHeight;

        scissorManager.push(positionMatrix, listX, listY, listW, listH);

        float smoothingOffset = (float) smoothedScroll;
        float offset = -smoothingOffset;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset;
            component.width = (int) listW;
            component.render(context, mouseX, mouseY, delta);
            offset += compHeight + LIST_SPACING;
        }

        scissorManager.pop();

        // Draw scrollbar if needed
        if (maxScroll > 0) {
            float scrollbarX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING - 1;
            float scrollbarY = listY + SCROLLBAR_PADDING;
            float scrollbarHeight = listH - SCROLLBAR_PADDING * 2;
            
            float thumbHeight = Math.max(20.0F, scrollbarHeight * (viewportHeight / contentHeight));
            float thumbY = scrollbarY + (float)((scrollbarHeight - thumbHeight) * (smoothedScroll / maxScroll));
            
            // Scrollbar thumb
            blurGlass.render(ShapeProperties.create(matrices, scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight)
                    .round(SCROLLBAR_WIDTH / 2)
                    .color(ColorUtil.getClientColor(0.6F))
                    .build());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isHover(mouseX, mouseY)) {
            return false;
        }
        
        if (maxScroll <= 0.0) {
            return false;
        }

        scroll = MathHelper.clamp(scroll - amount * SCROLL_STEP, 0.0, maxScroll);
        return true;
    }

    private float calculateContentHeight() {
        float total = 0.0F;
        for (ModuleComponent component : moduleComponents) {
            total += component.getComponentHeight() + LIST_SPACING;
        }
        if (!moduleComponents.isEmpty()) {
            total -= LIST_SPACING;
        }
        return total;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is within the panel bounds
        if (!isHover(mouseX, mouseY)) {
            return false;
        }
        
        if (button == 0 && maxScroll > 0) {
            float listY = y + TITLE_HEIGHT;
            float viewportHeight = PANEL_HEIGHT - TITLE_HEIGHT - CONTENT_PADDING;
            float contentHeight = calculateContentHeight();
            
            float scrollbarX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING - 1;
            float scrollbarY = listY + SCROLLBAR_PADDING;
            float scrollbarHeight = viewportHeight - SCROLLBAR_PADDING * 2;
            
            float thumbHeight = Math.max(20.0F, scrollbarHeight * (viewportHeight / contentHeight));
            float thumbY = scrollbarY + (float)((scrollbarHeight - thumbHeight) * (smoothedScroll / maxScroll));
            
            if (MathUtil.isHovered(mouseX, mouseY, scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight)) {
                draggingScrollbar = true;
                dragStartY = mouseY;
                scrollAtDragStart = scroll;
                return true;
            }
        }
        
        // Check if click is within the content viewport (not in title area)
        float listY = y + TITLE_HEIGHT;
        float viewportHeight = PANEL_HEIGHT - TITLE_HEIGHT - CONTENT_PADDING;
        if (mouseY < listY || mouseY > listY + viewportHeight) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        layoutModules();
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        
        // Check if release is within the content viewport
        float listY = y + TITLE_HEIGHT;
        float viewportHeight = PANEL_HEIGHT - TITLE_HEIGHT - CONTENT_PADDING;
        if (mouseY < listY || mouseY > listY + viewportHeight) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        
        layoutModules();
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbar && button == 0) {
            float listY = y + TITLE_HEIGHT;
            float viewportHeight = PANEL_HEIGHT - TITLE_HEIGHT - CONTENT_PADDING;
            float contentHeight = calculateContentHeight();
            
            float scrollbarY = listY + SCROLLBAR_PADDING;
            float scrollbarHeight = viewportHeight - SCROLLBAR_PADDING * 2;
            float thumbHeight = Math.max(20.0F, scrollbarHeight * (viewportHeight / contentHeight));
            
            double dragDistance = mouseY - dragStartY;
            double scrollDistance = dragDistance / (scrollbarHeight - thumbHeight) * maxScroll;
            
            scroll = MathHelper.clamp(scrollAtDragStart + scrollDistance, 0.0, maxScroll);
            return true;
        }
        
        layoutModules();
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        layoutModules();
        for (ModuleComponent component : moduleComponents) {
            if (component.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        layoutModules();
        for (ModuleComponent component : moduleComponents) {
            if (component.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    private void layoutModules() {
        float listX = x + CONTENT_PADDING;
        float listY = y + TITLE_HEIGHT;
        float listW = width - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        float smoothingOffset = (float) smoothedScroll;

        float offset = -smoothingOffset;
        for (ModuleComponent component : moduleComponents) {
            int compHeight = component.getComponentHeight();
            component.x = listX;
            component.y = listY + offset;
            component.width = (int) listW;
            offset += compHeight + LIST_SPACING;
        }
    }
}

