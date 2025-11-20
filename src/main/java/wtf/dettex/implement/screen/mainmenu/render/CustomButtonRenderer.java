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

        // Use constant tint to avoid color differences between buttons
        float visAlpha = 0.62F;
        float visOutlineAlpha = 0.72F;

        int fillColor = ColorUtil.getRect(visAlpha);
        int outlineColor = ColorUtil.getRect(visOutlineAlpha);

        // Scale up slightly on hover using fade
        float scale = 1.0f + 0.03f * clampedFade;
        context.getMatrices().push();
        float cx = x + width / 2f;
        float cy = y + height / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        int drawX = Math.round(-width / 2f);
        int drawY = Math.round(-height / 2f);

        QuickImports.blurGlass.render(ShapeProperties.create(context.getMatrices(), drawX, drawY, width, height)
                .round(6)
                .softness(1.3F)
                .thickness(2)
                .outlineColor(outlineColor)
                .color(fillColor)
                .build());

        // Uniform overlay to equalize perceived color across different backgrounds
        QuickImports.rectangle.render(ShapeProperties.create(context.getMatrices(), drawX, drawY, width, height)
                .round(6)
                .color(ColorUtil.getRect(0.20F))
                .build());

        int textColor = ColorUtil.replAlpha(0xFFFFFFFF, (int) (255 * (0.85f + 0.15f * clampedFade)));
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), text.getString(), drawX + width / 2F - 1, drawY + height / 2F - 2, textColor);
        context.getMatrices().pop();
    }
}

