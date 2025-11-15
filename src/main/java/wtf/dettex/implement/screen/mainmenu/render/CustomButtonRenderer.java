package wtf.dettex.implement.screen.mainmenu.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;

public final class CustomButtonRenderer {
    private CustomButtonRenderer() {
    }

    public static void render(DrawContext context, int x, int y, int width, int height, Text text, boolean hovered, boolean active, float fade) {
        float clampedFade = Math.max(0F, Math.min(1F, fade));

        float baseAlpha = active ? 0.7F : 0.6F;
        if (hovered) {
            baseAlpha = Math.min(0.8F, baseAlpha + 0.05F);
        } else {
            baseAlpha = Math.max(0.6F, baseAlpha);
        }
        float outlineAlpha = Math.min(0.8F, baseAlpha + 0.05F);

        int fillColor = ColorUtil.getAltManager(baseAlpha);
        int outlineColor = ColorUtil.getAltManager(outlineAlpha);

        QuickImports.blurGlass.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(5)
                .thickness(2)
                .outlineColor(ColorUtil.replAlpha(outlineColor, (int) (ColorUtil.alpha(outlineColor) * clampedFade)))
                .color(ColorUtil.replAlpha(fillColor, (int) (ColorUtil.alpha(fillColor) * clampedFade)))
                .build());

        int textColor = ColorUtil.replAlpha(0xFFFFFFFF, (int) (255 * clampedFade));
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), text.getString(), x + width / 2F - 1, y + height / 2F - 2, textColor);
    }
}
