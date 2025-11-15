package wtf.dettex.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import wtf.dettex.api.other.draggable.AbstractDraggable;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.api.system.font.FontRenderer;
import wtf.dettex.api.system.font.Fonts;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.render.Render2DUtil;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.modules.impl.render.Hud;

import java.util.*;

public class Potions extends AbstractDraggable {
    private final List<Potion> list = new ArrayList<>();
    public Potions() {
        super("Potions", 210, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
        list.forEach(p -> p.effect.update(mc.player,null));
    }

    @Override
    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case EntityStatusEffectS2CPacket effect -> {
                if (!PlayerIntersectionUtil.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
                    RegistryEntry<StatusEffect> effectId = effect.getEffectId();
                    list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectId.getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                    list.add(new Potion(new StatusEffectInstance(effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), new DecelerateAnimation().setMs(150).setValue(1.0F)));
                }
            }
            case RemoveEntityStatusEffectS2CPacket effect -> list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effect.effect().getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
            case PlayerRespawnS2CPacket p -> list.clear();
            case GameJoinS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.DEFAULT);

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F)
                .round(4,0,4,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        blurGlass.render(ShapeProperties.create(matrix, getX(), getY() + 17F, getWidth(), getHeight() - 17)
                .round(0,4,0,4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(Hud.newHudAlpha.getValue())).build());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 23, maxWidth = 80;

        font.drawString(matrix, getName(),(int) (centerX - font.getStringWidth(getName()) / 2.0F), getY() + 7, ColorUtil.getText());
        for (Potion potion : list) {
            StatusEffectInstance effect = potion.effect;
            float animation = potion.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            int amplifier = effect.getAmplifier();

            String name = effect.getEffectType().value().getName().getString();
            String duration = getDuration(effect);
            String lvl = amplifier > 0 ? Formatting.RED + " " + (amplifier + 1) + Formatting.RESET : "";

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                float animRed = effect.getDuration() != -1 && effect.getDuration() <= 120 ? MathUtil.blinking(1000, 8) : 1;
                Render2DUtil.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()), getX() + 5, (int) centerY - 2, 8, 8);
                rectangle.render(ShapeProperties.create(matrix, getX() + 14, centerY - 1, 0.5F, 7).color(ColorUtil.getOutline(0.75F,0.5F)).build());
                fontPotion.drawString(matrix, name + lvl, getX() + 18, centerY + 1, ColorUtil.getText());
                fontPotion.drawString(matrix, duration, getX() + getWidth() - 5 - fontPotion.getStringWidth(duration), centerY + 1, ColorUtil.multRed(ColorUtil.getText(), animRed));
            });

            int width = (int) fontPotion.getStringWidth(name + lvl + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += (int) (11 * animation);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }

    private String getDuration(StatusEffectInstance pe) {
        int var1 = pe.getDuration();
        int mins = var1 / 1200;
        return pe.isInfinite() || mins > 60 ? "**:**": mins + ":" + String.format("%02d", (var1 % 1200) / 20);
    }

    private record Potion(StatusEffectInstance effect, Animation anim) {}
}
