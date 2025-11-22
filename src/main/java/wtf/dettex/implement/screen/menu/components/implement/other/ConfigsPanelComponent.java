package wtf.dettex.implement.screen.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import wtf.dettex.Main;
import wtf.dettex.api.file.FileController;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ConfigsPanelComponent extends AbstractComponent {
    private static final float HEADER = 24f;
    private static final float PANEL_HEIGHT = 280f;
    private static final float ROW_H = 18f;
    private static final float PADDING = 6f;

    private final SearchComponent nameInput = new SearchComponent();
    private final List<String> configs = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean editing = false;
    private int editingIndex = -1;

    private final ButtonComponent newBtn = new ButtonComponent();
    private final ButtonComponent saveBtn = new ButtonComponent();
    private final ButtonComponent loadBtn = new ButtonComponent();
    private final ButtonComponent delBtn = new ButtonComponent();

    public ConfigsPanelComponent() {
        height = PANEL_HEIGHT;
        refresh();
        newBtn.setText("New").setRunnable(() -> {
            String base = nameInput.getText().isEmpty() ? "config" : nameInput.getText();
            String n = uniqueName(base);
            save(n);
        });
        saveBtn.setText("Save").setRunnable(() -> {
            String target = selectedIndex >= 0 && selectedIndex < configs.size() ? configs.get(selectedIndex) : nameInput.getText();
            if (target == null || target.isBlank()) target = uniqueName("config");
            save(target);
        });
        loadBtn.setText("Load").setRunnable(() -> {
            if (selectedIndex >= 0 && selectedIndex < configs.size()) load(configs.get(selectedIndex));
        });
        delBtn.setText("Del").setRunnable(() -> {
            if (selectedIndex >= 0 && selectedIndex < configs.size()) delete(configs.get(selectedIndex));
        });
    }

    private File configsDir() { return Main.getInstance().getClientInfoProvider().configsDir(); }

    private void refresh() {
        configs.clear();
        File dir = configsDir();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".json"));
            if (files != null) {
                configs.addAll(Arrays.stream(files).map(f -> f.getName().replaceFirst("\\.json$", "")).sorted().collect(Collectors.toList()));
            }
        }
        if (selectedIndex >= configs.size()) selectedIndex = configs.isEmpty() ? -1 : 0;
    }

    private String uniqueName(String base) {
        String candidate = base;
        int i = 1;
        while (configs.contains(candidate)) candidate = base + "-" + (i++);
        return candidate;
    }

    private void save(String name) {
        try {
            FileController fc = Main.getInstance().getFileController();
            fc.saveFile(name + ".json");
            refresh();
            selectedIndex = configs.indexOf(name);
        } catch (Exception ignored) {}
    }

    private void load(String name) {
        try {
            FileController fc = Main.getInstance().getFileController();
            fc.loadFile(name + ".json");
        } catch (Exception ignored) {}
    }

    private void rename(String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank()) return;
        newName = uniqueName(newName);
        File o = new File(configsDir(), oldName + ".json");
        File n = new File(configsDir(), newName + ".json");
        if (o.exists()) {
            //noinspection ResultOfMethodCallIgnored
            o.renameTo(n);
            refresh();
            selectedIndex = configs.indexOf(newName);
        }
    }

    private void delete(String name) {
        if (name == null) return;
        File f = new File(configsDir(), name + ".json");
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            refresh();
            selectedIndex = Math.min(selectedIndex, configs.size() - 1);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack m = context.getMatrices();
        blurGlass.render(ShapeProperties.create(m, x, y, width, height)
                .round(12).thickness(1).outlineColor(ColorUtil.BLACK).color(ColorUtil.getRect(0.30F)).build());

        String title = "Configs";
        var titleFont = Fonts.getSize(20, Fonts.Type.BOLD);
        float titleW = titleFont.getStringWidth(title);
        titleFont.drawString(m, title, (int) (x + (width - titleW) / 2f), (int) (y + 6), ColorUtil.getText());

        // list area
        float listX = x + PADDING;
        float listY = y + HEADER;
        float listW = width - PADDING * 2;
        float reserved = 96f; // space for input + 5 buttons stacked + margins
        float listH = height - HEADER - reserved;

        rectangle.render(ShapeProperties.create(m, listX, listY, listW, listH)
                .round(6).color(ColorUtil.getRect(0.18F)).build());

        // rows
        int maxRows = (int) Math.floor(listH / ROW_H);
        for (int i = 0; i < Math.min(maxRows, configs.size()); i++) {
            float ry = listY + i * ROW_H;
            if (i == selectedIndex) {
                rectangle.render(ShapeProperties.create(m, listX + 2, ry + 2, listW - 4, ROW_H - 4)
                        .round(4).color(ColorUtil.getClientColor(0.25f)).build());
            }
            Fonts.getSize(14).drawString(m, configs.get(i), (int) (listX + 6), (int) (ry + 5), ColorUtil.getText());
        }

        // input: inline on editing row, otherwise bottom area
        if (editing && editingIndex >= 0 && editingIndex < configs.size()) {
            float editY = listY + editingIndex * ROW_H;
            nameInput.position(listX + 4, editY + 2);
            nameInput.render(context, mouseX, mouseY, delta);
        } else {
            nameInput.position(listX, y + height - reserved + 4);
            nameInput.render(context, mouseX, mouseY, delta);
        }

        // buttons stacked in a single column under input
        float bx = x + PADDING;
        float by = y + height - reserved + 25;
        float step = 14f; // 12px button height + 2px gap

        newBtn.position(bx, by);
        newBtn.render(context, mouseX, mouseY, delta);

        saveBtn.position(bx, by + step);
        saveBtn.render(context, mouseX, mouseY, delta);

        loadBtn.position(bx, by + step * 2);
        loadBtn.render(context, mouseX, mouseY, delta);

        delBtn.position(bx, by + step * 3);
        delBtn.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // list selection
        float listX = x + PADDING;
        float listY = y + HEADER;
        float listW = width - PADDING * 2;
        float reserved = 96f; // must match render()
        float listH = height - HEADER - reserved;
        if (MathUtil.isHovered(mouseX, mouseY, listX, listY, listW, listH) && button == 0) {
            int index = (int) ((mouseY - listY) / ROW_H);
            if (index >= 0 && index < configs.size()) {
                selectedIndex = index;
                editing = true;
                editingIndex = index;
                nameInput.setText(configs.get(index));
                SearchComponent.typing = true;
            }
        }

        // forward click to input based on its current location
        if (editing && editingIndex >= 0 && editingIndex < configs.size()) {
            float editY = listY + editingIndex * ROW_H;
            nameInput.position(listX + 4, editY + 2);
        } else {
            nameInput.position(listX, y + height - reserved + 4);
        }
        nameInput.mouseClicked(mouseX, mouseY, button);

        float bx = x + PADDING;
        float by = y + height - reserved + 25;
        float step = 14f;

        newBtn.position(bx, by);
        newBtn.mouseClicked(mouseX, mouseY, button);
        saveBtn.position(bx, by + step);
        saveBtn.mouseClicked(mouseX, mouseY, button);
        loadBtn.position(bx, by + step * 2);
        loadBtn.mouseClicked(mouseX, mouseY, button);
        delBtn.position(bx, by + step * 3);
        delBtn.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        nameInput.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        nameInput.keyPressed(keyCode, scanCode, modifiers);
        if (editing && keyCode == GLFW.GLFW_KEY_ENTER) {
            if (editingIndex >= 0 && editingIndex < configs.size()) {
                rename(configs.get(editingIndex), nameInput.getText());
            }
            editing = false;
            editingIndex = -1;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        nameInput.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }
}
