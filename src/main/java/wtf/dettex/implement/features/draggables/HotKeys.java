package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import wtf.dettex.Main;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.impl.render.Hud;

import java.util.ArrayList;
import java.util.List;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();

    public HotKeys() {
        super("Hot Keys", 300, 10, 68, 23,true);
    }

    @Override
    public boolean visible() {
        return !keysList.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        keysList = Main.getInstance().getModuleProvider().getModules().stream().filter(module -> module.getAnimation().getOutput().floatValue() != 0 && module.getKey() != -1).toList();
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        float centerX = getX() + getWidth() / 2F;

        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);

        final float rowSpacing = 1.5F;
        final float headerSpacing = 1.5F;
        final float rowHeight = 12.0F + rowSpacing;
        final float leftPadding = 6.0F;
        final float textSpacing = 5.0F;
        final float nameBindGap = 8.0F;
        final float rightPadding = 6.0F;
        final float textVerticalOffset = 2.0F;

        boolean noKeys = keysList.isEmpty();

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F)
                .round(noKeys ? 4.0F : 4.0F, noKeys ? 4.0F : 0.0F, noKeys ? 4.0F : 4.0F, noKeys ? 4.0F : 0.0F)
                .softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(Hud.newHudAlpha.getValue()))
                .build());

        if (noKeys) {
            font.drawString(matrix, getName(), (int) (centerX - font.getStringWidth(getName()) / 2), getY() + 7, ColorUtil.getText());
            setHeight(23);
            setWidth(68);
            return;
        }

        float listTop = getY() + 17 + headerSpacing;
        float listHeight = Math.max(getHeight() - 17 - headerSpacing, rowHeight + rowSpacing);
        float listX = getX();
        float listWidth = Math.max(getWidth(), 40.0F);

        blurGlass.render(ShapeProperties.create(matrix, listX, listTop, listWidth, listHeight)
                .round(4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        font.drawString(matrix, getName(), (int) (centerX - font.getStringWidth(getName()) / 2), getY() + 7, ColorUtil.getText());

        int offset = (int) (23 + headerSpacing);
        int maxWidth = 68;

        for (Module module : keysList) {
            String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";
            float animation = module.getAnimation().getOutput().floatValue();
            float rowTop = getY() + offset;
            float rowCenterY = rowTop + rowHeight / 2.0F;
            float moduleNameWidth = fontModule.getStringWidth(module.getName());
            float bindWidth = fontModule.getStringWidth(bind);
            float calculatedWidth = leftPadding + moduleNameWidth + nameBindGap + bindWidth + rightPadding;
            final float rowWidth = Math.max(calculatedWidth, maxWidth);

            MathUtil.scale(matrix, centerX, rowCenterY, 1, animation, () -> {
                float textHeight = fontModule.getStringHeight(module.getName());
                float textMidY = rowTop + textVerticalOffset + textHeight / 2.0F;
                float textX = getX() + leftPadding;
                float textY = rowTop + textVerticalOffset;
                fontModule.drawString(matrix, module.getName(), textX, textY, ColorUtil.getText());

                float bindX = getX() + rowWidth - rightPadding - bindWidth;
                fontModule.drawString(matrix, bind, bindX, textY, ColorUtil.getText());
            });

            offset += (int) (animation * rowHeight);
            maxWidth = (int) Math.max(calculatedWidth, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }
}
