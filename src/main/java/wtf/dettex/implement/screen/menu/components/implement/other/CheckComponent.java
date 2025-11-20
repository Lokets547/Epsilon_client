package wtf.dettex.implement.screen.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;


@Setter
@Accessors(chain = true)
public class CheckComponent extends AbstractComponent {
    private boolean state;
    private Runnable runnable;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        // Статичный фон квадрата без влияния HUD и анимаций
        int bgColor = ColorUtil.getGuiRectColor(1);
        int outlineColor = ColorUtil.getOutline();

        rectangle.render(ShapeProperties.create(matrix, x, y, 12, 12)
                .round(2.5F).thickness(2).softness(0.5F)
                .outlineColor(outlineColor)
                .color(bgColor).build());

        if (state) {
            // Зелёная галочка при включённом состоянии (без скиссора, всегда видна)
            int green = 0xFF00FF00; // Ярко-зелёный
            image.setTexture("textures/check.png").render(ShapeProperties.create(matrix, x + 3, y + 3.5, 6, 4.5)
                    .color(green).build());
        } else {
            // Красный крестик при выключенном состоянии (всегда виден, крупнее и по центру)
            FontRenderer markFont = Fonts.getSize(16, Fonts.Type.DEFAULT);
            String mark = "✕";
            float markW = markFont.getStringWidth(mark);
            float markH = markFont.getStringHeight(mark);
            float markX = x + (12.0F - markW) / 2.0F + 0.2F;
            float markY = y + (12.0F - markH) / 2.0F + 8.0F;
            int red = 0xFFFF0000; // Ярко-красный, без opacity
            markFont.drawString(matrix, mark, markX, markY, red);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, 12, 12) && button == 0) {
            runnable.run();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

