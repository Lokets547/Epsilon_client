package wtf.dettex.implement.screen.menu.components.implement.settings.select;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;

import java.util.List;
import java.util.Objects;

import static wtf.dettex.api.system.font.Fonts.Type.BOLD;
import static wtf.dettex.common.util.math.MathUtil.*;

public class SelectedButton extends AbstractComponent {
    private final SelectSetting setting;
    private final String text;

    @Setter
    @Accessors(chain = true)
    private float alpha;

    private final Animation alphaAnimation = new DecelerateAnimation()
            .setMs(300).setValue(0.5F);

    public SelectedButton(SelectSetting setting, String text) {
        this.setting = setting;
        this.text = text;

        alphaAnimation.setDirection(Direction.BACKWARDS);
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();

        alphaAnimation.setDirection(Objects.equals(setting.getSelected(), text) ? Direction.FORWARDS : Direction.BACKWARDS);

        float opacity = alphaAnimation.getOutput().floatValue();
        int selectedOpacity = ColorUtil.multAlpha(ColorUtil.multAlpha(ColorUtil.getClientColor(), opacity), alpha);

        if (!alphaAnimation.isFinished(Direction.BACKWARDS)) {
            rectangle.render(ShapeProperties.create(matrices, x, y, width, height + 0.15F).round(getRound(setting.getList(), text)).color(selectedOpacity).build());
        }
        Fonts.getSize(12, BOLD).drawString(matrices, text, x + 4, y + 5, ColorUtil.multAlpha(0xFFD4D6E1, alpha));
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            setting.setSelected(text);
            return true; // Signal that selection was made
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    public static Vector4f getRound(List<String> list, String text) {
        if (list.size() == 1) return new Vector4f(4);
        if (Objects.equals(list.getLast(), text)) return new Vector4f(0, 4, 0, 4);
        if (Objects.equals(list.getFirst(), text)) return new Vector4f(4, 0, 4, 0);
        return new Vector4f(0);
    }
}
