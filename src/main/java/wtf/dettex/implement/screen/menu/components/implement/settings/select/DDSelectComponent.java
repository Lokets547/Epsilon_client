package wtf.dettex.implement.screen.menu.components.implement.settings.select;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import wtf.dettex.Main;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.render.ScissorManager;
import wtf.dettex.implement.screen.menu.components.implement.settings.AbstractSettingComponent;

import java.util.ArrayList;
import java.util.List;

import static wtf.dettex.api.system.font.Fonts.Type.BOLD;

public class DDSelectComponent extends AbstractSettingComponent {
    private final List<DDSelectedButton> selectedButtons = new ArrayList<>();
    private static final int SELECTOR_HEIGHT = 16;
    private static final int ITEM_HEIGHT = 14;
    private static final int BASE_HEIGHT = 36;
    private static final int DROPDOWN_GAP = 8;

    private final SelectSetting setting;
    private boolean open;

    private float dropdownListX,
            dropDownListY,
            dropDownListWidth,
            dropDownListHeight;

    private final Animation alphaAnimation = new DecelerateAnimation()
            .setMs(300).setValue(1);

    public DDSelectComponent(SelectSetting setting) {
        super(setting);
        this.setting = setting;

        alphaAnimation.setDirection(Direction.BACKWARDS);

        for (String s : setting.getList()) {
            selectedButtons.add(new DDSelectedButton(setting, s));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();

        List<String> fullSettingsList = setting.getList();

        float innerX = x + 3;
        float innerW = width - 6;

        this.dropdownListX = innerX;
        this.dropDownListY = y + 15 + SELECTOR_HEIGHT + 2;
        this.dropDownListWidth = innerW;
        this.dropDownListHeight = fullSettingsList.size() * ITEM_HEIGHT;

        alphaAnimation.setDirection(open ? Direction.FORWARDS : Direction.BACKWARDS);
        float expandProgress = alphaAnimation.getOutput().floatValue();

        float expandable = dropDownListHeight + DROPDOWN_GAP;
        height = BASE_HEIGHT + Math.round(expandable * expandProgress);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        ScissorManager scissorManager = Main.getInstance().getScissorManager();
        float componentClipTop = getClampedClipTop(y);
        float componentClipBottom = getClampedClipBottom(y + height);
        float componentClipHeight = componentClipBottom - componentClipTop;
        if (componentClipHeight <= 0.0F) {
            return;
        }

        scissorManager.push(matrix, x, componentClipTop, width, componentClipHeight);
        try {
            renderSelected(matrices, innerX, innerW);
            if (expandProgress > 0.001F && dropDownListHeight > 0) {
                renderSelectList(context, mouseX, mouseY, delta, expandProgress);
            }

            Fonts.getSize(14, BOLD).drawString(matrices, setting.getName(), (int) (x + 6), (int) (y + 6), 0xFFD4D6E1);
        } finally {
            scissorManager.pop();
        }

        Fonts.getSize(14, BOLD).drawString(matrices, setting.getName(), (int) (x + 6), (int) (y + 6), 0xFFD4D6E1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (MathUtil.isHovered(mouseX, mouseY, dropdownListX, y + 15, dropDownListWidth, SELECTOR_HEIGHT)) {
                open = !open;
            } else if (open && !isHoveredList(mouseX, mouseY)) {
                open = false;
            }

            if (open) {
                // Check if any button was clicked (selected)
                for (DDSelectedButton selectedButton : selectedButtons) {
                    if (selectedButton.mouseClicked(mouseX, mouseY, button)) {
                        // Item was selected, close the dropdown
                        open = false;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        boolean selectorHover = MathUtil.isHovered(mouseX, mouseY, dropdownListX, y + 15, dropDownListWidth, SELECTOR_HEIGHT);
        return selectorHover || (open && isHoveredList(mouseX, mouseY));
    }

    private void renderSelected(MatrixStack matrices, float innerX, float innerW) {
        blurGlass.render(ShapeProperties.create(matrices, innerX, y + 15, innerW, SELECTOR_HEIGHT)
                .round(2).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.45F)).build());

        String selectedName = String.join(", ", setting.getSelected());
        var font = Fonts.getSize(12, BOLD);
        float maxWidth = Math.max(0, innerW - 6);

        String displayName = selectedName;
        if (maxWidth > 0 && font.getStringWidth(selectedName) > maxWidth) {
            float ellipsisWidth = font.getStringWidth("...");
            float availableWidth = Math.max(0, maxWidth - ellipsisWidth);
            String trimmed = font.trimToWidth(selectedName, (int) availableWidth);
            displayName = trimmed + "...";
        }

        font.drawString(matrices, displayName, (int) (innerX + 3), (int) (y + 22), 0xFFD4D6E1);
    }

    private void renderSelectList(DrawContext context, int mouseX, int mouseY, float delta, float expandProgress) {
        if (expandProgress <= 0.001F || dropDownListHeight <= 0) return;

        float opacity = expandProgress;
        float currentHeight = dropDownListHeight * expandProgress;
        if (currentHeight <= 0.5F) currentHeight = 0.5F;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        ScissorManager scissorManager = Main.getInstance().getScissorManager();
        float listClipTop = Math.max(dropDownListY, getClampedClipTop(dropDownListY));
        float listClipBottom = Math.min(dropDownListY + currentHeight, getClampedClipBottom(dropDownListY + currentHeight));
        float listClipHeight = listClipBottom - listClipTop;
        if (listClipHeight <= 0.0F) {
            return;
        }

        scissorManager.push(matrix, dropdownListX, listClipTop, dropDownListWidth, listClipHeight);

        blurGlass.render(ShapeProperties.create(context.getMatrices(), dropdownListX, listClipTop, dropDownListWidth, listClipHeight)
                .round(3).thickness(2).outlineColor(ColorUtil.getOutline(opacity, 1)).color(ColorUtil.getRect(0.45F)).build());

        float offset = dropDownListY;
        float maxVisibleY = listClipBottom;
        for (DDSelectedButton button : selectedButtons) {
            float buttonBottom = offset + ITEM_HEIGHT;
            if (buttonBottom <= listClipTop) {
                offset += ITEM_HEIGHT;
                continue;
            }
            if (offset >= maxVisibleY) break;

            button.x = dropdownListX;
            button.y = offset;
            button.width = dropDownListWidth;
            button.height = ITEM_HEIGHT;

            button.setAlpha(opacity);
            button.render(context, mouseX, mouseY, delta);

            offset += ITEM_HEIGHT;
        }

        scissorManager.pop();
    }

    private boolean isHoveredList(double mouseX, double mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, dropdownListX, dropDownListY - 20, dropDownListWidth, dropDownListHeight + 20);
    }
}
