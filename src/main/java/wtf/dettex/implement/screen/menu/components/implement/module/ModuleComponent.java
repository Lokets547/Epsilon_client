package wtf.dettex.implement.screen.menu.components.implement.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import wtf.dettex.Main;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.render.ScissorManager;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;
import wtf.dettex.implement.screen.menu.components.implement.settings.AbstractSettingComponent;
import wtf.dettex.implement.screen.menu.components.implement.settings.ValueComponent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.setting.SettingComponentAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;



@Getter
public class ModuleComponent extends AbstractComponent {
    private static final float HEADER_HEIGHT = 16.0F;
    private static final float ANIMATION_THRESHOLD = 0.001F;

    private final List<AbstractSettingComponent> components = new ArrayList<>();
    private final Module module;
    private boolean expanded;
    private final Animation expandAnimation = new DecelerateAnimation().setMs(220).setValue(1);
    private boolean binding;
    private final Animation bindAnimation = new DecelerateAnimation().setMs(500).setValue(1);

    private float externalClipTop = Float.NEGATIVE_INFINITY;
    private float externalClipBottom = Float.POSITIVE_INFINITY;
    private float currentClipTop = Float.NaN;
    private float currentClipBottom = Float.NaN;

    private void initialize() {
        new SettingComponentAdder().addSettingComponent(module.settings(), components);
    }

    public ModuleComponent(Module module) {
        this.module = module;
        initialize();
        expandAnimation.setDirection(Direction.BACKWARDS);
        expandAnimation.reset();
        bindAnimation.setDirection(Direction.BACKWARDS);
        bindAnimation.reset();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float animationValue = expandAnimation.getOutput().floatValue();
        float expandedHeight = getFullSettingsHeight();
        height = HEADER_HEIGHT + expandedHeight * animationValue;

        // Uniform semi-transparent background for all modules (independent of state)
        float headerAlpha = module.state ? 0.42F : 0.15F;

        int backgroundColor = module.state
                ? ColorUtil.getRect(headerAlpha)
                : ColorUtil.getColor(255, 255, 255, 0.08F);

        blurGlass.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(3)
                .color(backgroundColor)
                .build());

        bindAnimation.setDirection(binding ? Direction.FORWARDS : Direction.BACKWARDS);
        float bindProgress = bindAnimation.getOutput().floatValue();

        String defaultName = module.getVisibleName();
        int keyCode = module.getKey();
        String bindName = keyCode == GLFW.GLFW_KEY_UNKNOWN ? "NONE" : StringUtil.getBindName(keyCode);
        String bindText = "Bind | " + bindName;

        float baseCenter = x + width / 2F;
        float textY = y + 7.5F;
        // Text always white
        int activeColor = 0xFFD4D6E1;

        float defaultWidth = Fonts.getSize(14, Fonts.Type.BOLD).getStringWidth(defaultName);
        float defaultX = baseCenter - defaultWidth / 2F + bindProgress * 1.5F;
        if (bindProgress < 0.999F) {
            float alphaScale = Math.max(0.0F, 1.0F - bindProgress);
            Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), defaultName, (int) defaultX, (int) textY, ColorUtil.multAlpha(activeColor, alphaScale));
        }

        if (bindProgress > 0.001F) {
            float bindWidth = Fonts.getSize(14, Fonts.Type.BOLD).getStringWidth(bindText);
            float bindX = baseCenter - bindWidth / 2F - (1.0F - bindProgress) * 2.0F;
            float bindAlpha = Math.min(1.0F, bindProgress * 1.1F);
            Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), bindText, (int) bindX, (int) textY, ColorUtil.multAlpha(ColorUtil.getClientColor(), bindAlpha));
        }

        if (hasVisibleSettings()) {
            float dotSize = 4.5F;
            float dotX = x + width - dotSize - 4.5F;
            float dotY = y + (HEADER_HEIGHT - dotSize) / 2F;
            blurGlass.render(ShapeProperties.create(context.getMatrices(), dotX, dotY, dotSize, dotSize)
                    .round(dotSize)
                    .color(ColorUtil.getClientColor(0.8F))
                    .build());
        }

        currentClipTop = Float.NaN;
        currentClipBottom = Float.NaN;

        if (animationValue > ANIMATION_THRESHOLD) {
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            ScissorManager scissorManager = Main.getInstance().getScissorManager();
            float settingsTop = y + HEADER_HEIGHT;
            float settingsBottom = y + height;
            float clipTop = Math.max(settingsTop, externalClipTop);
            float clipBottom = Math.min(settingsBottom, externalClipBottom);
            float clipHeight = Math.max(0.0F, clipBottom - clipTop);

            if (clipHeight > 0.0F) {
                currentClipTop = clipTop;
                currentClipBottom = clipBottom;

                scissorManager.push(matrix, x, clipTop, width, clipHeight);
                try {
                    float offsetY = settingsTop + 2;

                    for (AbstractSettingComponent component : components) {
                        Supplier<Boolean> visible = component.getSetting().getVisible();
                        if (visible != null && !visible.get()) continue;

                        int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                        float componentBottom = offsetY + componentHeight;

                        component.x = x;
                        component.width = width;
                        component.y = offsetY;
                        component.setClipBounds(clipTop, clipBottom);

                        if (componentBottom > clipTop && offsetY < clipBottom) {
                            component.render(context, mouseX, mouseY, delta);
                        }

                        offsetY += componentHeight + 2;
                        if (offsetY >= clipBottom) {
                            break;
                        }
                    }
                } finally {
                    scissorManager.pop();
                }
            }
        }
    }

    public void setExternalClip(float clipTop, float clipBottom) {
        this.externalClipTop = clipTop;
        this.externalClipBottom = clipBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                setBinding(false);
                module.switchState();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                setBinding(false);
                expanded = !expanded;
                expandAnimation.setDirection(expanded ? Direction.FORWARDS : Direction.BACKWARDS);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                setBinding(true);
            }
            return true;
        }

        if (isExpandedVisible()) {
            layoutSettings();
            ClipBounds clipBounds = computeClipBounds();
            boolean handled = false;
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                if (component.isHover(mouseX, mouseY)) {
                    component.mouseClicked(mouseX, mouseY, button);
                    handled = true;
                    break;
                }
            }
            if (handled) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) return true;
        if (isExpandedVisible()) {
            ClipBounds clipBounds = computeClipBounds();
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                if (component.isHover(mouseX, mouseY)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isExpandedVisible()) {
            layoutSettings();
            ClipBounds clipBounds = computeClipBounds();
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isExpandedVisible()) {
            layoutSettings();
            ClipBounds clipBounds = computeClipBounds();
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isExpandedVisible()) {
            layoutSettings();
            ClipBounds clipBounds = computeClipBounds();
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                component.mouseScrolled(mouseX, mouseY, amount);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setBinding(false);
            } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(GLFW.GLFW_KEY_UNKNOWN);
                setBinding(false);
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                module.setKey(keyCode);
                setBinding(false);
            }
            return true;
        }

        if (isExpandedVisible()) {
            layoutSettings();
            ClipBounds clipBounds = computeClipBounds();
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setBinding(boolean state) {
        if (binding == state) {
            return;
        }
        binding = state;
        bindAnimation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
        bindAnimation.reset();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isExpandedVisible()) {
            layoutSettings();
            ClipBounds clipBounds = computeClipBounds();
            for (AbstractSettingComponent component : components) {
                int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
                if (!clipBounds.isVisible(component.y, componentHeight)) continue;
                component.charTyped(chr, modifiers);
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public int getComponentHeight() {
        float animationValue = expandAnimation.getOutput().floatValue();
        if (!expanded && expandAnimation.isFinished(Direction.BACKWARDS)) {
            return (int) HEADER_HEIGHT;
        }
        return (int) (HEADER_HEIGHT + getFullSettingsHeight() * animationValue);
    }

    private void layoutSettings() {
        float offsetY = y + HEADER_HEIGHT + 2;
        float clipTop = Math.max(y + HEADER_HEIGHT, externalClipTop);
        float clipBottom = Math.min(y + height, externalClipBottom);
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
            component.x = x;
            component.width = width;
            component.y = offsetY;
            component.setClipBounds(clipTop, clipBottom);
            offsetY += componentHeight + 2;
        }
    }

    private float getFullSettingsHeight() {
        float total = 0;
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            int componentHeight = component.height > 0 ? (int) component.height : getDefaultSettingHeight(component);
            total += componentHeight + 2;
        }
        return total;
    }

    private int getDefaultSettingHeight(AbstractSettingComponent component) {
        if (component instanceof ValueComponent) return 28;
        return 20;
    }

    private ClipBounds computeClipBounds() {
        float animationValue = expandAnimation.getOutput().floatValue();
        if (animationValue <= ANIMATION_THRESHOLD) {
            return ClipBounds.EMPTY;
        }

        float defaultTop = y + HEADER_HEIGHT;
        float defaultBottom = y + height;
        float clipTop = Float.isNaN(currentClipTop) ? Math.max(defaultTop, externalClipTop) : currentClipTop;
        float clipBottom = Float.isNaN(currentClipBottom) ? Math.min(defaultBottom, externalClipBottom) : currentClipBottom;

        if (clipBottom - clipTop <= 0.0F) {
            return ClipBounds.EMPTY;
        }

        return new ClipBounds(clipTop, clipBottom);
    }

    private boolean isExpandedVisible() {
        float value = expandAnimation.getOutput().floatValue();
        if (expanded) {
            return value > ANIMATION_THRESHOLD;
        }
        return value > ANIMATION_THRESHOLD && !expandAnimation.isFinished(Direction.BACKWARDS);
    }

    private static final class ClipBounds {
        private static final ClipBounds EMPTY = new ClipBounds(Float.NaN, Float.NaN);

        private final float top;
        private final float bottom;

        ClipBounds(float top, float bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        boolean isVisible(float componentY, float componentHeight) {
            if (Float.isNaN(top) || Float.isNaN(bottom)) {
                return false;
            }
            return componentY < bottom && componentY + componentHeight > top;
        }
    }

    private boolean hasVisibleSettings() {
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible == null || visible.get()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleComponent that = (ModuleComponent) o;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module);
    }
}