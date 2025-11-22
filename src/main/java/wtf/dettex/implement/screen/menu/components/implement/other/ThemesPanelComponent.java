package wtf.dettex.implement.screen.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import wtf.dettex.Main;
import wtf.dettex.api.repository.theme.Theme;
import wtf.dettex.api.repository.theme.ThemeRepository;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;
import wtf.dettex.implement.screen.menu.components.implement.window.implement.settings.color.ColorWindow;
import wtf.dettex.modules.setting.implement.ColorSetting;

public class ThemesPanelComponent extends AbstractComponent {
    private static final float HEADER = 24f;
    private static final float PANEL_HEIGHT = 280f;
    private static final float ROW_H = 18f;
    private static final float PADDING = 6f;

    private final ThemeRepository repo = Main.getInstance().getThemeRepository();

    private final ColorSetting primary = new ColorSetting("Primary", "");
    private final ColorSetting secondary = new ColorSetting("Secondary", "");
    private final ColorSetting background = new ColorSetting("Background", "");
    private final ColorSetting module = new ColorSetting("Module", "");
    private final ColorSetting setting = new ColorSetting("Setting", "");
    private final ColorSetting text = new ColorSetting("Text", "");

    private final ButtonComponent newBtn = new ButtonComponent();
    private final ButtonComponent applyBtn = new ButtonComponent();

    public ThemesPanelComponent() {
        height = PANEL_HEIGHT;
        loadFromActive();
        newBtn.setText("New").setRunnable(() -> {
            Theme t = repo.create("theme");
            applyTo(t);
            loadFromActive();
        });
        applyBtn.setText("Apply").setRunnable(() -> {
            Theme t = repo.getActive();
            if (t != null) applyTo(t);
        });
    }

    private void loadFromActive() {
        Theme t = repo.getActive();
        if (t == null) return;
        primary.setColor(t.getPrimaryColor());
        secondary.setColor(t.getSecondaryColor());
        background.setColor(t.getBackgroundColor());
        module.setColor(t.getModuleColor());
        setting.setColor(t.getSettingColor());
        text.setColor(t.getTextColor());
    }

    private void applyTo(Theme t) {
        t.setPrimaryColor(primary.getColor());
        t.setSecondaryColor(secondary.getColor());
        t.setBackgroundColor(background.getColor());
        t.setModuleColor(module.getColor());
        t.setSettingColor(setting.getColor());
        t.setTextColor(text.getColor());
        repo.apply(t);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack m = context.getMatrices();
        blurGlass.render(ShapeProperties.create(m, x, y, width, height)
                .round(12).thickness(1).outlineColor(ColorUtil.BLACK).color(ColorUtil.getRect(0.30F)).build());

        String title = "Themes";
        var titleFont = Fonts.getSize(20, Fonts.Type.BOLD);
        float titleW = titleFont.getStringWidth(title);
        titleFont.drawString(m, title, (int) (x + (width - titleW) / 2f), (int) (y + 6), ColorUtil.getText());

        // theme chips row
        float chipX = x + PADDING;
        float chipY = y + HEADER - 4;
        var chipFont = Fonts.getSize(12);
        for (Theme t : repo.getThemes()) {
            String nm = t.getName();
            float w = chipFont.getStringWidth(nm) + 10;
            int bg = repo.getActive() == t ? ColorUtil.getClientColor(0.35f) : ColorUtil.getRect(0.18f);
            rectangle.render(ShapeProperties.create(m, chipX, chipY, w, 14).round(4).color(bg).build());
            chipFont.drawString(m, nm, (int) (chipX + 5), (int) (chipY + 4), ColorUtil.getText());
            chipX += w + 4;
        }

        float ly = y + HEADER + 14;
        drawColorRow(context, mouseX, mouseY, ly, "Primary color", primary); ly += ROW_H + 4;
        drawColorRow(context, mouseX, mouseY, ly, "Secondary color", secondary); ly += ROW_H + 4;
        drawColorRow(context, mouseX, mouseY, ly, "Background", background); ly += ROW_H + 4;
        drawColorRow(context, mouseX, mouseY, ly, "Module color", module); ly += ROW_H + 4;
        drawColorRow(context, mouseX, mouseY, ly, "Setting color", setting); ly += ROW_H + 4;
        drawColorRow(context, mouseX, mouseY, ly, "Text color", text);

        // bottom actions
        float by = y + height - 20;
        int aw = (int) (Fonts.getSize(12).getStringWidth("Apply") + 13);
        newBtn.position(x + PADDING, by);
        newBtn.render(context, mouseX, mouseY, delta);

        applyBtn.position(x + width - aw - PADDING, by);
        applyBtn.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float by = y + height - 20;
        int aw = (int) (Fonts.getSize(12).getStringWidth("Apply") + 13);
        newBtn.position(x + PADDING, by);
        applyBtn.position(x + width - aw - PADDING, by);
        newBtn.mouseClicked(mouseX, mouseY, button);
        applyBtn.mouseClicked(mouseX, mouseY, button);
        // theme chips click detection
        float chipX = x + PADDING;
        float chipY = y + HEADER - 4;
        var chipFont = Fonts.getSize(12);
        for (Theme t : repo.getThemes()) {
            String nm = t.getName();
            float w = chipFont.getStringWidth(nm) + 10;
            if (MathUtil.isHovered(mouseX, mouseY, chipX, chipY, w, 14) && button == 0) {
                repo.apply(t);
                loadFromActive();
                break;
            }
            chipX += w + 4;
        }
        // color swatch clicks
        float ly = y + HEADER + 14;
        if (handleColorClick(mouseX, mouseY, button, ly, primary)) return true; ly += ROW_H + 4;
        if (handleColorClick(mouseX, mouseY, button, ly, secondary)) return true; ly += ROW_H + 4;
        if (handleColorClick(mouseX, mouseY, button, ly, background)) return true; ly += ROW_H + 4;
        if (handleColorClick(mouseX, mouseY, button, ly, module)) return true; ly += ROW_H + 4;
        if (handleColorClick(mouseX, mouseY, button, ly, setting)) return true; ly += ROW_H + 4;
        if (handleColorClick(mouseX, mouseY, button, ly, text)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawColorRow(DrawContext ctx, int mouseX, int mouseY, float rowY, String name, ColorSetting setting) {
        MatrixStack m = ctx.getMatrices();
        float rx = x + PADDING;
        float rw = width - PADDING * 2;
        rectangle.render(ShapeProperties.create(m, rx, rowY, rw, ROW_H)
                .round(4).color(ColorUtil.getRect(0.18F)).build());
        Fonts.getSize(14).drawString(m, name, (int) (rx + 6), (int) (rowY + 6), ColorUtil.getText());

        float sw = 20;
        float sx = rx + rw - sw - 4;
        rectangle.render(ShapeProperties.create(m, sx, rowY + 4, sw, ROW_H - 8)
                .round(3).color(setting.getColor()).build());

    }

    private boolean handleColorClick(double mouseX, double mouseY, int button, float rowY, ColorSetting setting) {
        float rx = x + PADDING;
        float rw = width - PADDING * 2;
        float sw = 20;
        float sx = rx + rw - sw - 4;
        if (MathUtil.isHovered(mouseX, mouseY, sx, rowY + 4, sw, ROW_H - 8) && button == 0) {
            ColorWindow window = new ColorWindow(setting);
            window.position(sx - 120, rowY + 20).size(150, 125);
            windowManager.add(window);
            return true;
        }
        return false;
    }
}
