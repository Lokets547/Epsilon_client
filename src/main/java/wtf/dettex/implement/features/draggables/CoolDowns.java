package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.other.StringUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.modules.impl.render.Hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }
    public final List<CoolDown> list = new ArrayList<>();

    public CoolDowns() {
        super("Cool Downs", 120, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        list.stream().filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
    }

   @Override
    public void packet(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new DecelerateAnimation().setMs(150).setValue(1.0F)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F )
                .round(4,0,4,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY() + 17, getWidth(), getHeight() - 17)
                .round(0,4,0,4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        float centerX = getX() + getWidth() / 2.0F;
        font.drawString(matrix, getName(), (int) (centerX - font.getStringWidth(getName()) / 2.0F), getY() + 7, ColorUtil.getText());

        int offset = 23;
        int maxWidth = 80;
        for (CoolDown coolDown : list) {
            float animation = coolDown.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            int time = -coolDown.time.elapsedTime() / 1000;
            String name = coolDown.item.getDefaultStack().getName().getString();
            String duration = StringUtil.getDuration(time);

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                float green = time <= 5 ? MathUtil.blinking(1000, 8) : 1;
                Render2DUtil.defaultDrawStack(context, coolDown.item.getDefaultStack(), getX() + 4, centerY - 3, false, false, 0.5F);
                rectangle.render(ShapeProperties.create(matrix, getX() + 15, centerY - 1, 0.5F, 6).color(ColorUtil.getOutline(1,0.5F)).build());
                fontCoolDown.drawString(matrix, name, getX() + 18, centerY + 1, ColorUtil.getText());
                fontCoolDown.drawString(matrix, duration, getX() + getWidth() - 5 - fontCoolDown.getStringWidth(duration), centerY + 1, ColorUtil.multGreen(ColorUtil.getText(), green));
            });

            int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += (int) (11 * animation);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}
