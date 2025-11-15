package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import wtf.dettex.Main;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.impl.render.Hud;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();
    private static final Map<ModuleCategory, Identifier> CATEGORY_ICONS = new EnumMap<>(ModuleCategory.class);
    private static final Identifier DEFAULT_ICON = Identifier.of("minecraft", "textures/misc.png");

    static {
        CATEGORY_ICONS.put(ModuleCategory.COMBAT, Identifier.of("minecraft", "textures/combat.png"));
        CATEGORY_ICONS.put(ModuleCategory.MOVEMENT, Identifier.of("minecraft", "textures/movement.png"));
        CATEGORY_ICONS.put(ModuleCategory.PLAYER, Identifier.of("minecraft", "textures/player.png"));
        CATEGORY_ICONS.put(ModuleCategory.RENDER, Identifier.of("minecraft", "textures/render.png"));
        CATEGORY_ICONS.put(ModuleCategory.MISC, Identifier.of("minecraft", "textures/misc.png"));
    }

    public HotKeys() {
        super("Hot Keys", 300, 10, 80, 23,true);
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
        final float iconColumnOffset = 4.0F;
        final float iconColumnWidth = 18.0F;
        final float columnGap = 1.5F;
        final float iconSize = 9.0F;
        final float textSpacing = 5.0F;
        final float nameBindGap = 8.0F;
        final float rightPadding = 6.0F;
        final float textVerticalOffset = 2.0F;
        final float iconVerticalBias = 1.5F;

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
            setWidth(80);
            return;
        }

        float iconsBlockTop = getY() + 17 + headerSpacing;
        float iconPanelWidth = iconColumnOffset + iconColumnWidth;
        float iconsBlockWidth = iconPanelWidth;
        float iconsBlockHeight = Math.max(getHeight() - 17 - headerSpacing, rowHeight + rowSpacing);
        float bindsBlockX = getX() + iconPanelWidth + columnGap;
        float bindsBlockWidth = Math.max(getWidth() - iconPanelWidth - columnGap, 40.0F);

        blurGlass.render(ShapeProperties.create(matrix, getX(), iconsBlockTop, iconsBlockWidth, iconsBlockHeight)
                .round(0,0,0,4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        blurGlass.render(ShapeProperties.create(matrix, bindsBlockX, iconsBlockTop, bindsBlockWidth, iconsBlockHeight)
                .round(0,4,0,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        font.drawString(matrix, getName(), (int) (centerX - font.getStringWidth(getName()) / 2), getY() + 7, ColorUtil.getText());

        int offset = (int) (23 + headerSpacing);
        int maxWidth = 80;

        for (Module module : keysList) {
            String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";
            float animation = module.getAnimation().getOutput().floatValue();
            float rowTop = getY() + offset;
            float rowCenterY = rowTop + rowHeight / 2.0F;
            float moduleNameWidth = fontModule.getStringWidth(module.getName());
            float bindWidth = fontModule.getStringWidth(bind);
            float calculatedWidth = iconPanelWidth + columnGap + textSpacing + moduleNameWidth + nameBindGap + bindWidth + rightPadding;
            final float rowWidth = Math.max(calculatedWidth, maxWidth);

            Identifier icon = CATEGORY_ICONS.getOrDefault(module.getCategory(), DEFAULT_ICON);

            MathUtil.scale(matrix, centerX, rowCenterY, 1, animation, () -> {
                float iconBgHeight = rowHeight - rowSpacing;
                float iconBgX = getX() + iconColumnOffset;
                float iconBgY = rowTop + (rowHeight - iconBgHeight) / 2.0F;

                float iconX = iconBgX + (iconColumnWidth - iconSize) / 3.0F;

                float textHeight = fontModule.getStringHeight(module.getName());
                float textMidY = rowTop + textVerticalOffset + textHeight / 2.0F;
                float idealIconY = textMidY - iconSize * 2.0F;
                float minIconY = iconBgY - iconVerticalBias;
                float maxIconY = iconBgY + iconBgHeight - iconSize;
                float iconY = Math.max(minIconY, Math.min(maxIconY, idealIconY));

                Render2DUtil.drawTexture(matrix, icon, iconX, iconX + iconSize, iconY, iconY + iconSize, 0,
                        16, 16, 0, 0, 16, 16, 0xFFFFFFFF);

                float textBgX = getX() + iconPanelWidth + columnGap;
                float textBgWidth = rowWidth - iconPanelWidth - columnGap;
                float textBgHeight = iconBgHeight;

                float textX = textBgX + textSpacing;
                float textY = rowTop + textVerticalOffset;
                fontModule.drawString(matrix, module.getName(), textX, textY, ColorUtil.getText());

                float bindX = textBgX + textBgWidth - rightPadding - bindWidth;
                fontModule.drawString(matrix, bind, bindX, textY, ColorUtil.getText());
            });

            offset += (int) (animation * rowHeight);
            maxWidth = (int) Math.max(calculatedWidth, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }
}
