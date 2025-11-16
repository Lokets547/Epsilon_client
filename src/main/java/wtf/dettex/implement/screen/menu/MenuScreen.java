package wtf.dettex.implement.screen.menu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import wtf.dettex.implement.screen.menu.components.implement.other.CategoryContainerComponent;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.sound.SoundManager;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;
import wtf.dettex.implement.screen.menu.components.implement.category.DropdownCategoryComponent;
import wtf.dettex.implement.screen.menu.components.implement.other.*;
import wtf.dettex.implement.screen.menu.components.implement.settings.TextComponent;
import wtf.dettex.modules.impl.render.ClickGUI;

import java.util.ArrayList;
import java.util.List;

import static wtf.dettex.api.system.animation.Direction.BACKWARDS;
import static wtf.dettex.api.system.animation.Direction.FORWARDS;

@Getter
@Setter
public class MenuScreen extends Screen implements QuickImports {
    public static MenuScreen INSTANCE = new MenuScreen();
    private final List<AbstractComponent> components = new ArrayList<>();
    private CategoryContainerComponent panelContainer;
    private final List<DropdownCategoryComponent> dropdownComponents = new ArrayList<>();
    public final Animation animation = new DecelerateAnimation().setMs(200).setValue(1);
    public ModuleCategory category = ModuleCategory.COMBAT;
    public int x, y, width, height;
    private String cachedLayout = "";

    public void initialize() {
        animation.setDirection(FORWARDS);
        panelContainer = new CategoryContainerComponent();
        rebuildLayout();
    }

    public MenuScreen() {
        super(Text.of("MenuScreen"));
        initialize();
    }

    private boolean isDropdownLayout() {
        ClickGUI clickGUI = ClickGUI.getInstance();
        return clickGUI != null && clickGUI.mode().isSelected("DropDown");
    }

    private void rebuildLayout() {
        components.clear();
        dropdownComponents.clear();

        cachedLayout = isDropdownLayout() ? "DropDown" : "Panels";

        if ("DropDown".equalsIgnoreCase(cachedLayout)) {
            panelContainer = null;
            for (ModuleCategory moduleCategory : ModuleCategory.values()) {
                DropdownCategoryComponent component = new DropdownCategoryComponent(moduleCategory);
                dropdownComponents.add(component);
                components.add(component);
            }
        } else {
            panelContainer = new CategoryContainerComponent();
            panelContainer.initializeCategoryComponents();
            components.add(panelContainer);
        }
    }

    public void openGui() {
        animation.setDirection(FORWARDS);
        rebuildLayout();
        mc.setScreen(this);
        SoundManager.playSound(SoundManager.OPEN_GUI);
    }

    public float getScaleAnimation() {
        return animation.getOutput().floatValue();
    }

    @Override
    public void tick() {
        close();
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String currentLayout = isDropdownLayout() ? "DropDown" : "Panels";
        if (!currentLayout.equalsIgnoreCase(cachedLayout)) {
            rebuildLayout();
            currentLayout = cachedLayout;
        }

        x = window.getScaledWidth() / 2 - 318;
        y = window.getScaledHeight() / 2 - 160;
        // Width to accommodate 5 panels of 108px each + 6px gaps and margins
        width = 636;
        // Height to accommodate 280px panels + top/bottom margins
        height = 320;

        if ("DropDown".equalsIgnoreCase(currentLayout)) {
            float offsetX = 0F;
            float spacing = 6F;
            for (DropdownCategoryComponent component : dropdownComponents) {
                component.position(x + 6 + offsetX, y + 12);
                component.size(115F, component.height);
                offsetX += component.width + spacing;
            }
        } else if (panelContainer != null) {
            panelContainer.position(x, y);
        }

        MathUtil.scale(context.getMatrices(), x + (float) width / 2, y + (float) height / 2, getScaleAnimation(), () -> {
            components.forEach(component -> component.render(context, mouseX, mouseY, delta));
            windowManager.render(context, mouseX, mouseY, delta);
        });
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double[] p = unscale(mouseX, mouseY);
        if (windowManager.mouseClicked(p[0], p[1], button)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseClicked(p[0], p[1], button);
        }
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double[] p = unscale(mouseX, mouseY);
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseReleased(p[0], p[1], button);
        }
        handled |= windowManager.mouseReleased(p[0], p[1], button);
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double[] p = unscale(mouseX, mouseY);
        if (windowManager.mouseDragged(p[0], p[1], button, deltaX, deltaY)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseDragged(p[0], p[1], button, deltaX, deltaY);
        }
        return handled || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double[] p = unscale(mouseX, mouseY);
        if (windowManager.mouseScrolled(p[0], p[1], vertical)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.mouseScrolled(p[0], p[1], vertical);
        }
        return handled || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            animation.setDirection(BACKWARDS);
            return true;
        }

        if (windowManager.keyPressed(keyCode, scanCode, modifiers)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.keyPressed(keyCode, scanCode, modifiers);
        }
        return handled || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (windowManager.charTyped(chr, modifiers)) return true;
        boolean handled = false;
        for (AbstractComponent component : components) {
            handled |= component.charTyped(chr, modifiers);
        }
        return handled || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (animation.isFinished(BACKWARDS)) {
            TextComponent.typing = false;
            super.close();
        }
    }

    private double[] unscale(double mx, double my) {
        float s = getScaleAnimation();
        if (s == 0) s = 1f;
        float cx = x + (float) width / 2f;
        float cy = y + (float) height / 2f;
        double ux = (mx - cx) / s + cx;
        double uy = (my - cy) / s + cy;
        return new double[]{ux, uy};
    }
}