package wtf.dettex.implement.features.draggables;

import antidaunleak.api.UserProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.MovingUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.modules.impl.render.Hud;

public class Watermark extends AbstractDraggable {
    private static final Identifier PLAYER_ICON = Identifier.of("minecraft", "textures/player.png");
    private static final Identifier FPS_ICON = Identifier.of("minecraft", "textures/fps.png");
    private static final Identifier PING_ICON = Identifier.of("minecraft", "textures/ping.png");
    private static final Identifier MOVEMENT_ICON = Identifier.of("minecraft", "textures/movement.png");
    private static final Identifier CLIENT_LOGO = Identifier.of("minecraft", "textures/logo.png");
    private static final String LOGO_TEXT = "Dettex Recode";
    private static final float LOGO_MIN_WIDTH = 0.0F;
    private int fpsCount = 0;
    private float bps = 0.0F;

    public Watermark() {
        super("Watermark", 10, 10, 160, 24,true);
    }

    @Override
    public void tick() {
        fpsCount = (int) MathUtil.interpolate(fpsCount, mc.getCurrentFps());
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            float speed = (float) (MovingUtil.getSpeedSqrt(player) * 20.0);
            bps = (float) MathUtil.interpolate(bps, speed);
        }
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);

        String username = UserProfile.getInstance().profile("username");
        String fpsText = fpsCount + " FPS";
        int ping = Math.max(fetchPing(), 0);
        String pingText = ping + " MS";
        Double bps = MathUtil.round(MovingUtil.getSpeedSqrt(mc.player) * 20.0F, 0.1F);
        String bpsText = StringUtil.formatNumber(bps, 1) + " BPS";

        float padding = 4.0F;
        float elementSpacing = 1.5F;
        float chipHeight = 18.0F;
        float iconSize = 7.0F;
        float iconSpacing = 3.0F;

        float cursorX = getX();
        float top = getY();

        Hud hud = Hud.getInstance();
        String watermarkStyle = hud != null ? hud.watermarkStyle.getSelected() : "Client Name";

        boolean renderLogoBlock = hud != null && hud.interfaceSettings.isSelected("Watermark") && !"None".equals(watermarkStyle);
        CornerStyle usernameStyle = renderLogoBlock ? CornerStyle.MIDDLE : CornerStyle.LEFT_OUTER;

        if (renderLogoBlock) {
            switch (watermarkStyle) {
                case "Client Logo" -> cursorX = renderLogoIconChip(matrix, cursorX, top, chipHeight, padding) + elementSpacing;
                case "Client Name" -> cursorX = renderLogoChip(matrix, font, cursorX, top, chipHeight, padding) + elementSpacing;
            }
        }

        cursorX = renderTextChip(e, matrix, font, username, PLAYER_ICON, cursorX, top, chipHeight, padding, iconSize, iconSpacing, usernameStyle);
        cursorX += elementSpacing;
        cursorX = renderTextChip(e, matrix, font, fpsText, FPS_ICON, cursorX, top, chipHeight, padding, iconSize, iconSpacing, CornerStyle.MIDDLE);
        cursorX += elementSpacing;
        cursorX = renderTextChip(e, matrix, font, pingText, PING_ICON, cursorX, top, chipHeight, padding, iconSize, iconSpacing, CornerStyle.MIDDLE);
        cursorX += elementSpacing;
        cursorX = renderTextChip(e, matrix, font, bpsText, MOVEMENT_ICON, cursorX, top, chipHeight, padding, iconSize, iconSpacing, CornerStyle.RIGHT_OUTER);

        setWidth((int) (cursorX - getX()));
        setHeight((int) chipHeight);
    }

    private void renderChipBackground(MatrixStack matrix, float x, float y, float height, float width, CornerStyle style) {
        float outerCorner = 7.0F;
        float mainCorner = 1.5F;

        float topLeft;
        float bottomLeft;
        float topRight;
        float bottomRight;

        switch (style) {
            case LEFT_OUTER -> {
                topLeft = outerCorner;
                bottomLeft = outerCorner;
                topRight = mainCorner;
                bottomRight = mainCorner;
            }
            case RIGHT_OUTER -> {
                topLeft = mainCorner;
                bottomLeft = mainCorner;
                topRight = outerCorner;
                bottomRight = outerCorner;
            }
            default -> {
                topLeft = mainCorner;
                bottomLeft = mainCorner;
                topRight = mainCorner;
                bottomRight = mainCorner;
            }
        }

        blurGlass.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(topRight, bottomRight, bottomLeft, topLeft)
                .softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());
    }

    private float renderTextChip(DrawContext context, MatrixStack matrix, FontRenderer font, String text, Identifier icon,
                                 float cursorX, float top, float chipHeight, float padding, float iconSize, float iconSpacing,
                                 CornerStyle style) {
        float textWidth = font.getStringWidth(text);
        float chipWidth = textWidth + padding * 2.0F + iconSize + iconSpacing;
        renderChipBackground(matrix, cursorX, top, chipHeight, chipWidth, style);

        float iconX = cursorX + padding;
        float iconY = top + (chipHeight - iconSize) / 2.0F;
        Render2DUtil.drawTexture(matrix, icon, iconX, iconX + iconSize, iconY, iconY + iconSize, 0,
                16, 16, 0, 0, 16, 16, ColorUtil.getClientColor());

        float textBaseline = top + (chipHeight - font.getStringHeight(text)) / 0.14F;
        float textX = iconX + iconSize + iconSpacing;
        font.drawString(matrix, text, textX, textBaseline, ColorUtil.getText());
        return cursorX + chipWidth;
    }

    private float renderLogoChip(MatrixStack matrix, FontRenderer font, float cursorX, float top, float chipHeight, float padding) {
        float textWidth = font.getStringWidth(LOGO_TEXT);
        float chipWidth = Math.max(LOGO_MIN_WIDTH, textWidth + padding * 2.0F);

        renderChipBackground(matrix, cursorX, top, chipHeight, chipWidth, CornerStyle.LEFT_OUTER);

        float textHeight = font.getStringHeight(LOGO_TEXT);
        float textY = top + (chipHeight - textHeight) + 6.5f;
        float available = chipWidth - padding * 2.0F;
        float startX = cursorX + padding + Math.max(0.0F, (available - textWidth) / 2.0F);

        font.drawString(matrix, LOGO_TEXT, startX, textY, ColorUtil.getClientColor());
        return cursorX + chipWidth;
    }

    private float renderLogoIconChip(MatrixStack matrix, float cursorX, float top, float chipHeight, float padding) {
        float iconSize = 10.0F;
        float chipWidth = iconSize + padding * 2.0F;

        renderChipBackground(matrix, cursorX, top, chipHeight, chipWidth, CornerStyle.LEFT_OUTER);

        float iconX = cursorX + padding;
        float iconY = top + (chipHeight - iconSize) / 2.0F;
        Render2DUtil.drawTexture(matrix, CLIENT_LOGO, iconX, iconX + iconSize, iconY, iconY + iconSize, 0,
                32, 32, 0, 0, 32, 32, 0xFFFFFFFF);
        return cursorX + chipWidth;
    }

    private enum CornerStyle {
        LEFT_OUTER,
        RIGHT_OUTER,
        MIDDLE
    }

    private int fetchPing() {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}
