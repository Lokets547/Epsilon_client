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
import wtf.dettex.common.util.world.ServerUtil;
import wtf.dettex.modules.impl.render.Hud;
import wtf.dettex.event.impl.container.SetScreenEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends AbstractDraggable {
    private static final Identifier PLAYER_ICON = Identifier.of("minecraft", "textures/player.png");
    private static final Identifier FPS_ICON = Identifier.of("minecraft", "textures/fps.png");
    private static final Identifier PING_ICON = Identifier.of("minecraft", "textures/ping.png");
    private static final Identifier MOVEMENT_ICON = Identifier.of("minecraft", "textures/movement.png");
    private static final Identifier CLIENT_LOGO = Identifier.of("minecraft", "textures/logo.png");
    private static final String LOGO_TEXT = "Epsilon";
    private static final float LOGO_MIN_WIDTH = 0.0F;
    private static final String[] MENU_ITEMS = {"Client", "Role", "Name", "Time", "Coords", "Ping", "FPS", "TPS", "BPS"};
    private static final float MENU_PADDING = 3.0F;
    private int fpsCount = 0;
    private float bps = 0.0F;

    private boolean menuOpen = false;
    private int menuX;
    private int menuY;
    private int menuWidth;
    private int menuHeight;
    private float menuItemHeight;

    // Позиции чекбоксов в меню для точного хитбокса
    private final float[] menuCheckboxX = new float[MENU_ITEMS.length];
    private final float[] menuCheckboxY = new float[MENU_ITEMS.length];

    private boolean showClient = true;
    private boolean showRole = true;
    private boolean showUsername = true;
    private boolean showTime = true;
    private boolean showCoords = true;
    private boolean showPing = true;
    private boolean showFps = true;
    private boolean showTps = true;
    private boolean showBps = true;

    public Watermark() {
        // Жёсткая фиксация позиции: отключаем перетаскивание (canDrag = false)
        super("Watermark", 10, 10, 160, 24,false);
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
        String roleText = "Developer";
        String fpsText = fpsCount + " FPS";
        int ping = Math.max(fetchPing(), 0);
        String pingText = ping + " MS";
        Double bpsValue = MathUtil.round(MovingUtil.getSpeedSqrt(mc.player) * 20.0F, 0.1F);
        String bpsText = StringUtil.formatNumber(bpsValue, 1) + " BPS";

        ClientPlayerEntity player = mc.player;
        String coordsText = "X: " + StringUtil.formatNumber(player.getX(), 1)
                + " Y: " + StringUtil.formatNumber(player.getY(), 1)
                + " Z: " + StringUtil.formatNumber(player.getZ(), 1);

        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String tpsText = StringUtil.formatNumber(ServerUtil.TPS, 1) + " TPS";

        float padding = 4.0F;
        float elementSpacing = 1.5F;
        float chipHeight = 18.0F;
        float iconSize = 7.0F;
        float iconSpacing = 3.0F;
        float lineSpacing = 2.0F;

        float baseX = getX();
        float baseY = getY();

        Hud hud = Hud.getInstance();
        String watermarkStyle = hud != null ? hud.watermarkStyle.getSelected() : "Client Name";

        boolean renderLogoBlock = hud != null && hud.interfaceSettings.isSelected("Watermark");

        float cursorXTop = baseX;
        float topRowY = baseY;

        if (showClient && renderLogoBlock) {
            switch (watermarkStyle) {
                case "Client Logo" -> cursorXTop = renderLogoIconChip(matrix, cursorXTop, topRowY, chipHeight, padding) + elementSpacing;
                case "Client Name" -> cursorXTop = renderLogoChip(matrix, font, cursorXTop, topRowY, chipHeight, padding) + elementSpacing;
                case "Client Name + Logo" -> cursorXTop = renderLogoIconNameChip(matrix, font, cursorXTop, topRowY, chipHeight, padding, iconSpacing) + elementSpacing;
            }
        }

        boolean hasLogoLeft = showClient && renderLogoBlock;
        int topTextCount = 0;
        if (showRole) topTextCount++;
        if (showUsername) topTextCount++;
        if (showTime) topTextCount++;

        int topIndex = 0;
        if (showRole) {
            CornerStyle style = getCornerStyle(hasLogoLeft, topIndex, topTextCount);
            cursorXTop = renderTextChip(e, matrix, font, roleText, FPS_ICON, cursorXTop, topRowY, chipHeight, padding, iconSize, iconSpacing, style);
            cursorXTop += elementSpacing;
            topIndex++;
        }
        if (showUsername) {
            CornerStyle style = getCornerStyle(hasLogoLeft, topIndex, topTextCount);
            cursorXTop = renderTextChip(e, matrix, font, username, PLAYER_ICON, cursorXTop, topRowY, chipHeight, padding, iconSize, iconSpacing, style);
            cursorXTop += elementSpacing;
            topIndex++;
        }
        if (showTime) {
            CornerStyle style = getCornerStyle(hasLogoLeft, topIndex, topTextCount);
            cursorXTop = renderTextChip(e, matrix, font, timeText, FPS_ICON, cursorXTop, topRowY, chipHeight, padding, iconSize, iconSpacing, style);
        }

        float row1EndX = cursorXTop;

        float cursorXBottom = baseX;
        float bottomRowY = baseY + chipHeight + lineSpacing;

        int bottomTextCount = 0;
        if (showCoords) bottomTextCount++;
        if (showPing) bottomTextCount++;
        if (showFps) bottomTextCount++;
        if (showTps) bottomTextCount++;
        if (showBps) bottomTextCount++;

        int bottomIndex = 0;
        if (showCoords) {
            CornerStyle style = getCornerStyle(false, bottomIndex, bottomTextCount);
            cursorXBottom = renderTextChip(e, matrix, font, coordsText, MOVEMENT_ICON, cursorXBottom, bottomRowY, chipHeight, padding, iconSize, iconSpacing, style);
            cursorXBottom += elementSpacing;
            bottomIndex++;
        }
        if (showPing) {
            CornerStyle style = getCornerStyle(false, bottomIndex, bottomTextCount);
            cursorXBottom = renderTextChip(e, matrix, font, pingText, PING_ICON, cursorXBottom, bottomRowY, chipHeight, padding, iconSize, iconSpacing, style);
            cursorXBottom += elementSpacing;
            bottomIndex++;
        }
        if (showFps) {
            CornerStyle style = getCornerStyle(false, bottomIndex, bottomTextCount);
            cursorXBottom = renderTextChip(e, matrix, font, fpsText, FPS_ICON, cursorXBottom, bottomRowY, chipHeight, padding, iconSize, iconSpacing, style);
            cursorXBottom += elementSpacing;
            bottomIndex++;
        }
        if (showTps) {
            CornerStyle style = getCornerStyle(false, bottomIndex, bottomTextCount);
            cursorXBottom = renderTextChip(e, matrix, font, tpsText, FPS_ICON, cursorXBottom, bottomRowY, chipHeight, padding, iconSize, iconSpacing, style);
            cursorXBottom += elementSpacing;
            bottomIndex++;
        }
        if (showBps) {
            CornerStyle style = getCornerStyle(false, bottomIndex, bottomTextCount);
            cursorXBottom = renderTextChip(e, matrix, font, bpsText, MOVEMENT_ICON, cursorXBottom, bottomRowY, chipHeight, padding, iconSize, iconSpacing, style);
        }

        float row2EndX = cursorXBottom;

        float totalWidth = Math.max(row1EndX, row2EndX) - baseX;
        setWidth((int) totalWidth);
        setHeight((int) (chipHeight * 2.0F + lineSpacing));

        if (menuOpen) {
            drawMenu(matrix, font);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            menuOpen = !menuOpen;
            if (menuOpen) {
                menuX = getX();
                menuY = getY() + getHeight() + 4;
            }
            return true;
        }

        if (button == 0 && menuOpen) {
            if (handleMenuClick(mouseX, mouseY)) {
                return true;
            }
            if (!isInsideMenu(mouseX, mouseY)) {
                menuOpen = false;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void drawMenu(MatrixStack matrix, FontRenderer font) {
        String[] labels = new String[MENU_ITEMS.length];
        boolean[] enabledFlags = new boolean[MENU_ITEMS.length];
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            String name = MENU_ITEMS[i];
            boolean enabled = switch (i) {
                case 0 -> showClient;
                case 1 -> showRole;
                case 2 -> showUsername;
                case 3 -> showTime;
                case 4 -> showCoords;
                case 5 -> showPing;
                case 6 -> showFps;
                case 7 -> showTps;
                case 8 -> showBps;
                default -> true;
            };
            labels[i] = name;
            enabledFlags[i] = enabled;
        }

        float textHeight = font.getStringHeight("Text");
        // Высота строки: немного больше текста
        float rowHeight = textHeight + 2.0F;
        float rowGap = 2.0F;
        float itemHeight = rowHeight + rowGap;
        int columns = 2;
        int rows = (labels.length + columns - 1) / columns;

        float maxLabelWidth = 0.0F;
        for (String label : labels) {
            float w = font.getStringWidth(label);
            if (w > maxLabelWidth) {
                maxLabelWidth = w;
            }
        }

        // Размер чекбокса как в CheckComponent (12x12)
        float checkboxSize = 12.0F;
        float textLeftPadding = 9.0F;
        float checkboxRightOffset = 20.0F;
        // Ширина колонки: левый отступ текста + длина самого длинного текста + зона под чекбокс и отступ до правого края
        float columnWidth = textLeftPadding + maxLabelWidth + checkboxRightOffset;

        float x = menuX;
        float y = menuY;
        float width = MENU_PADDING + columns * columnWidth;
        float height = MENU_PADDING * 2.0F + rows * itemHeight;

        // Более прозрачный фон меню с закруглёнными углами
        blurGlass.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(4.0F, 4.0F, 4.0F, 4.0F)
                .softness(1.0F).thickness(1.0F)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(Hud.newHudAlpha.getValue() * 0.6F)).build());

        float baseTextX = x + MENU_PADDING;
        float baseTextY = y + MENU_PADDING;

        int index = 0;
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < rows; row++) {
                if (index >= labels.length) break;
                String label = labels[index];
                boolean enabled = enabledFlags[index];

                float itemX = baseTextX + col * columnWidth;
                float rowTop = baseTextY + row * itemHeight;

                float cbSize = checkboxSize;
                float cbX = itemX + columnWidth - checkboxRightOffset;
                float cbY = rowTop + (rowHeight - cbSize) / 2.0F;

                // Статичный фон квадрата без влияния HUD для обоих состояний
                int bgColor = ColorUtil.getGuiRectColor(1);
                int outlineColor = ColorUtil.getOutline();

                rectangle.render(ShapeProperties.create(matrix, cbX, cbY, cbSize, cbSize)
                        .round(2.5F).thickness(2.0F).softness(0.5F)
                        .outlineColor(outlineColor)
                        .color(bgColor).build());

                float textX = itemX + textLeftPadding;
                float textY = rowTop + (rowHeight - textHeight) / 2.0F + 6.5F;
                font.drawString(matrix, label, textX, textY, ColorUtil.getText());

                if (enabled) {
                    // Зелёная галочка при включённом состоянии
                    int green = 0xFF00FF00; // Ярко-зелёный
                    image.setTexture("textures/check.png").render(ShapeProperties.create(matrix,
                                    cbX + 3.0F, cbY + 3.5F, 6.0F, 4.5F)
                            .color(green).build());
                } else {
                    // Красный крестик при выключенном состоянии (крупнее и по центру)
                    FontRenderer markFont = Fonts.getSize(16, Fonts.Type.DEFAULT);
                    String mark = "✕";
                    float markW = markFont.getStringWidth(mark);
                    float markH = markFont.getStringHeight(mark);
                    float markX = cbX + (cbSize - markW) / 2.0F + 0.2F;
                    float markY = cbY + (cbSize - markH) / 2.0F + 8.0F;
                    int red = 0xFFFF0000; // Ярко-красный
                    markFont.drawString(matrix, mark, markX, markY, red);
                }

                // Сохраняем позицию чекбокса для кликов
                menuCheckboxX[index] = cbX;
                menuCheckboxY[index] = cbY;

                index++;
            }
        }

        menuWidth = (int) width;
        menuHeight = (int) height;
        menuItemHeight = itemHeight;
    }

    private boolean isInsideMenu(double mouseX, double mouseY) {
        return mouseX >= menuX && mouseX <= menuX + menuWidth
                && mouseY >= menuY && mouseY <= menuY + menuHeight;
    }

    private boolean handleMenuClick(double mouseX, double mouseY) {
        if (!isInsideMenu(mouseX, mouseY) || menuItemHeight <= 0.0F) {
            return false;
        }

        float cbSize = 12.0F; // как в drawMenu

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            float cbX = menuCheckboxX[i];
            float cbY = menuCheckboxY[i];

            boolean insideCheckbox = mouseX >= cbX && mouseX <= cbX + cbSize
                    && mouseY >= cbY && mouseY <= cbY + cbSize;
            if (!insideCheckbox) continue;

            switch (i) {
            case 0 -> showClient = !showClient;
            case 1 -> showRole = !showRole;
            case 2 -> showUsername = !showUsername;
            case 3 -> showTime = !showTime;
            case 4 -> showCoords = !showCoords;
            case 5 -> showPing = !showPing;
            case 6 -> showFps = !showFps;
            case 7 -> showTps = !showTps;
            case 8 -> showBps = !showBps;
            }

            return true;
        }

        return false;
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        super.setScreen(e);
        // Закрываем меню при смене экрана (в том числе при выходе из чата через ESC)
        menuOpen = false;
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

    private float renderLogoIconNameChip(MatrixStack matrix, FontRenderer font, float cursorX, float top, float chipHeight, float padding, float iconSpacing) {
        float iconSize = 10.0F;
        float textWidth = font.getStringWidth(LOGO_TEXT);
        float chipWidth = Math.max(LOGO_MIN_WIDTH, textWidth + padding * 2.0F + iconSize + iconSpacing);

        renderChipBackground(matrix, cursorX, top, chipHeight, chipWidth, CornerStyle.LEFT_OUTER);

        float iconX = cursorX + padding;
        float iconY = top + (chipHeight - iconSize) / 2.0F;
        Render2DUtil.drawTexture(matrix, CLIENT_LOGO, iconX, iconX + iconSize, iconY, iconY + iconSize, 0,
                32, 32, 0, 0, 32, 32, 0xFFFFFFFF);

        float textHeight = font.getStringHeight(LOGO_TEXT);
        float textY = top + (chipHeight - textHeight) + 6.5f;
        float available = chipWidth - padding * 2.0F - iconSize - iconSpacing;
        float startX = iconX + iconSize + iconSpacing + Math.max(0.0F, (available - textWidth) / 2.0F);

        font.drawString(matrix, LOGO_TEXT, startX, textY, ColorUtil.getClientColor());
        return cursorX + chipWidth;
    }

    private enum CornerStyle {
        LEFT_OUTER,
        RIGHT_OUTER,
        MIDDLE,
        BOTH_OUTER
    }

    private CornerStyle getCornerStyle(boolean hasLogoLeft, int index, int count) {
        if (count <= 0) return CornerStyle.MIDDLE;

        boolean first = index == 0;
        boolean last = index == count - 1;

        // Единственный элемент в строке
        if (count == 1) {
            if (hasLogoLeft) {
                // Слева логотип, поэтому скругляем только правый край
                return CornerStyle.RIGHT_OUTER;
            }
            // Без логотипа слева скругляем оба края
            return CornerStyle.BOTH_OUTER;
        }

        if (first) {
            // Если слева логотип, первый текстовый чип идёт между блоками
            return hasLogoLeft ? CornerStyle.MIDDLE : CornerStyle.LEFT_OUTER;
        }

        if (last) {
            return CornerStyle.RIGHT_OUTER;
        }

        return CornerStyle.MIDDLE;
    }

    private int fetchPing() {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}
