package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.modules.impl.combat.Aura;
import wtf.dettex.modules.impl.combat.killaura.rotation.Angle;
import wtf.dettex.modules.impl.combat.killaura.rotation.RotationController;
import wtf.dettex.modules.setting.implement.SelectSetting;

public class Rotations extends AbstractDraggable {

    public Rotations() {
        super("Rotations", 10, 40, 140, 18, true);
    }

    @Override
    public boolean visible() {
        Aura aura = Aura.getInstance();
        if (aura == null) return PlayerIntersectionUtil.isChat(mc.currentScreen);
        SelectSetting aimMode = aura.getAimMode();
        boolean show = aimMode != null && (aimMode.isSelected("Trax") || aimMode.isSelected("Manda"));
        return show || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        Aura aura = Aura.getInstance();
        if (aura == null) return;

        SelectSetting aimMode = aura.getAimMode();
        if (aimMode == null) return;

        boolean isTrax = aimMode.isSelected("Trax");
        boolean isManda = aimMode.isSelected("Manda");
        if (!isTrax && !isManda) return;

        Angle angle = RotationController.INSTANCE.getRotation();
        float yaw = angle.getYaw();
        float pitch = angle.getPitch();

        String label = isTrax ? "Trax" : "Manda";
        String text = label + ": Y " + String.format("%.1f", yaw) + "  P " + String.format("%.1f", pitch);

        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);

        float textWidth = font.getStringWidth(text);
        float textHeight = font.getStringHeight(text);
        float paddingX = 4.0F;
        float paddingY = 0.5F;
        float boxWidth = textWidth + paddingX * 2;
        float boxHeight = textHeight + paddingY * 2;
        float boxX = getX();
        float boxY = getY();

        setWidth((int) boxWidth);
        setHeight((int) boxHeight);

        setY(Math.max(0, Math.min(getY(), window.getScaledHeight() - getHeight())));
        setX(Math.max(0, Math.min(getX(), window.getScaledWidth() - getWidth())));

        blurGlass.render(ShapeProperties.create(context.getMatrices(), boxX, boxY, boxWidth, boxHeight)
                .round(3)
                .softness(1)
                .thickness(2)
                .outlineColor(ColorUtil.getOutline())
                .color(ColorUtil.getRect(wtf.dettex.modules.impl.render.Hud.newHudAlpha.getValue()))
                .build());

        font.drawString(context.getMatrices(), text, (int) (boxX + paddingX), (int) (boxY + paddingY + 6.5f), ColorUtil.getText());
    }
}

