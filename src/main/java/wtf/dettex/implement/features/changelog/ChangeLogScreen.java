package wtf.dettex.implement.features.changelog;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.implement.screen.mainmenu.CustomMainMenu;

public class ChangeLogScreen extends Screen implements QuickImports {
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    private float listX;
    private float listY;
    private float listW;
    private float listH;

    private double scroll;
    private double targetScroll;

    public ChangeLogScreen() {
        super(Text.of("ChangeLog"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use the same main menu background (no additional blur applied here)
        CustomMainMenu.renderTitleBackground(context, this.width, this.height);
        computeLayout();

        float alpha = 0.6F;
        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX, panelY, panelW, panelH)
                .round(8).thickness(2)
                .outlineColor(ColorUtil.getAltManager(Math.min(0.9F, alpha + 0.1F)))
                .color(ColorUtil.getAltManager(alpha)).build());

        // Title centered
        String title = "Список изменений";
        float titleW = Fonts.getSize(18, Fonts.Type.DEFAULT).getStringWidth(title);
        Fonts.getSize(18, Fonts.Type.DEFAULT).drawString(context.getMatrices(), title, panelX + (panelW - titleW) / 2F, panelY + 10, 0xFFFFFFFF);

        // Scroll
        double step = 0.18;
        scroll += (targetScroll - scroll) * step;

        // List background similar to AltManager
        rectangle.render(ShapeProperties.create(context.getMatrices(), listX, listY, listW, listH)
                .round(5).thickness(2)
                .outlineColor(ColorUtil.getAltManager(0.7F))
                .color(ColorUtil.getAltManager(0.6F)).build());

        int scX1 = (int) Math.floor(listX);
        int scY1 = (int) Math.floor(listY);
        int scX2 = (int) Math.ceil(listX + listW);
        int scY2 = (int) Math.ceil(listY + listH);
        context.enableScissor(scX1, scY1, scX2, scY2);

        float y = listY - (float) scroll;
        float lineGap = 4.0F;
        float lineHeight = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringHeight("A") / 2.0F + 4.0F;
        float maxLineWidth = listW - 16;

        for (String entry : ChangeLog.ENTRIES) {
            String remaining = "• " + entry;
            while (!remaining.isEmpty()) {
                String part = Fonts.getSize(16, Fonts.Type.DEFAULT).trimToWidth(remaining, (int) maxLineWidth);
                if (part.isEmpty()) break;

                if (y + lineHeight >= listY && y <= listY + listH) {
                    Fonts.getSize(16, Fonts.Type.DEFAULT).drawString(context.getMatrices(), part, listX + 8, y, 0xFFFFFFFF);
                }
                y += lineHeight;
                remaining = remaining.substring(part.length());
            }
            y += lineGap;
        }

        context.disableScissor();

        // Clamp scroll to content height
        float contentH = 0F;
        for (String entry : ChangeLog.ENTRIES) {
            String remaining = "• " + entry;
            while (!remaining.isEmpty()) {
                String part = Fonts.getSize(16, Fonts.Type.DEFAULT).trimToWidth(remaining, (int) maxLineWidth);
                if (part.isEmpty()) break;
                contentH += lineHeight;
                remaining = remaining.substring(part.length());
            }
            contentH += lineGap;
        }
        float max = Math.max(0F, contentH - listH);
        if (targetScroll > max) targetScroll = max;
        if (scroll > max) scroll = max;
        if (targetScroll < 0) targetScroll = 0;
        if (scroll < 0) scroll = 0;
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float lineGap = 4.0F;
        float lineHeight = Fonts.getSize(16, Fonts.Type.DEFAULT).getStringHeight("A") / 2.0F + 4.0F;
        float maxLineWidth = listW - 16;
        float contentH = 0F;
        for (String e : ChangeLog.ENTRIES) {
            String remaining = "• " + e;
            while (!remaining.isEmpty()) {
                String part = Fonts.getSize(16, Fonts.Type.DEFAULT).trimToWidth(remaining, (int) maxLineWidth);
                if (part.isEmpty()) break;
                contentH += lineHeight;
                remaining = remaining.substring(part.length());
            }
            contentH += lineGap;
        }
        float max = Math.max(0F, contentH - listH);
        targetScroll = Math.max(0F, Math.min(max, targetScroll - verticalAmount * 14));
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(new CustomMainMenu());
    }

    private void computeLayout() {
        // Slightly larger than AltManager (which uses ~280x240)
        panelW = Math.min(320, this.width - 40);
        panelH = Math.min(280, this.height - 60);
        panelX = this.width / 2F - panelW / 2F;
        panelY = this.height / 2F - panelH / 2F;

        float padding = 14;
        listX = panelX + padding;
        listY = panelY + padding + 18; // below title
        listW = panelW - padding * 2;
        listH = panelH - (listY - panelY) - padding;
    }
}
