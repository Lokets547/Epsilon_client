package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.modules.impl.render.Hud;

import java.util.Objects;

public class PlayerInfo extends AbstractDraggable {

    public PlayerInfo() {
        super("Player Info", 0, 0, 120, 14,true);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        int offset = PlayerIntersectionUtil.isChat(mc.currentScreen) ? 0 : 0;
        BlockPos blockPos = Objects.requireNonNull(mc.player).getBlockPos();
        FontRenderer font = Fonts.getSize(15);

        String xyz = "XYZ: " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
        float textWidth = font.getStringWidth(xyz);
        float textHeight = font.getStringHeight(xyz);

        float paddingX = 4.0F;
        float paddingY = 0.5F;
        float boxWidth = textWidth + paddingX * 2;
        float boxHeight = textHeight + paddingY * 2;
        float boxX = getX();
        float boxY = getY();

        setWidth((int) boxWidth);
        setHeight((int) boxHeight);

        setY(Math.max(0, Math.min(getY(), window.getScaledHeight() - getHeight() + offset)));
        setX(Math.max(0, Math.min(getX(), window.getScaledWidth() - getWidth())));

        blurGlass.render(ShapeProperties.create(context.getMatrices(), boxX, boxY, boxWidth, boxHeight)
                .round(3)
                .softness(1)
                .thickness(2)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(Hud.newHudAlpha.getValue()))
                .build());

        font.drawString(context.getMatrices(), xyz, (int) (boxX + paddingX), (int) (boxY + paddingY + 6.5f), ColorUtil.getText());
    }
}

