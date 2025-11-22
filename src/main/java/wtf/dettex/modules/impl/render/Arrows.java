package wtf.dettex.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.GroupSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.api.repository.friend.FriendUtils;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.math.MathUtil;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.event.impl.render.DrawEvent;

import java.util.List;

import static net.minecraft.client.render.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Arrows extends Module {
    Identifier[] iconIds = {
            Identifier.of("textures/arrowf.png"),
            Identifier.of("textures/arrow1.png"),
            Identifier.of("textures/arrow2.png"),
            Identifier.of("textures/arrow3.png"),
            Identifier.of("textures/arrow4.png"),
            Identifier.of("textures/arrow5.png")
    };
    Animation radiusAnim = new DecelerateAnimation().setMs(150).setValue(12);

    ValueSetting radiusSetting = new ValueSetting("Radius", "Radius of arrows")
            .setValue(50).range(30, 100);

    ValueSetting sizeSetting = new ValueSetting("Size", "Size of arrows")
            .setValue(16).range(8, 20);

    SelectSetting arrowType = new SelectSetting("Arrow Type", "Selects the type of arrow texture")
            .value("Стандартные", "Дельта", "Новые", "Упрощённые", "Современные", "Минималистичные");

    SelectSetting animationMode = new SelectSetting("Animation Mode", "Выбор поведения стрелочек")
            .value("Анимированые", "Статические");

    SelectSetting blurMode = new SelectSetting("Blur Type", "Выбор вида блюра").value("New", "Default");
    ValueSetting defaultBlurStrength = new ValueSetting("Blur Strength", "Сила блюра")
            .setValue(6).range(1, 12).visible(() -> blurMode.isSelected("Default"));
    GroupSetting blurGroup = new GroupSetting("Blur", "Настройки блюра стрелок")
            .settings(blurMode, defaultBlurStrength)
            .setValue(false);

    public Arrows() {
        super("Arrows", "Arrows", ModuleCategory.RENDER);
        setup(radiusSetting, sizeSetting, arrowType, animationMode, blurGroup);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (animationMode.getSelected().equals("Анимированые")) {
            radiusAnim.setDirection(mc.player.isSprinting() ? Direction.FORWARDS : Direction.BACKWARDS);
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        MatrixStack matrix = e.getDrawContext().getMatrices();
        List<AbstractClientPlayerEntity> players = mc.world.getPlayers().stream().filter(p -> p != mc.player).toList();

        float middleW = mc.getWindow().getScaledWidth() / 2f;
        float middleH = mc.getWindow().getScaledHeight() / 2f;
        float posY = middleH - radiusSetting.getValue() - (animationMode.getSelected().equals("Анимированые") ? radiusAnim.getOutput().floatValue() : 0);
        float size = sizeSetting.getValue();

        if (!mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON) && !players.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);

            int selectedIndex = switch (arrowType.getSelected()) {
                case "Дельта" -> 1;
                case "Новые" -> 2;
                case "Упрощённые" -> 3;
                case "Современные" -> 4;
                case "Минималистичные" -> 5;
                default -> 0;
            };
            RenderSystem.setShaderTexture(0, iconIds[selectedIndex]);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(QUADS, POSITION_TEXTURE_COLOR);

            players.forEach(player -> {
                int color = FriendUtils.isFriend(player) ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
                float yaw = getRotations(player) - mc.player.getYaw();
                matrix.push();
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
                matrix.translate(-middleW, -middleH, 0.0F);
                Matrix4f matrix4f = matrix.peek().getPositionMatrix();
                if (blurGroup.isValue()) {
                    if (blurMode.isSelected("New")) {
                        int samples = 15; // fixed strength for New mode
                        float maxOffset = size * 0.6f;
                        float step = maxOffset / samples;
                        for (int i = samples; i >= 1; i--) {
                            float off = i * step;
                            float alphaFactor = Math.max(0.05f, 0.35f * (1.0f - (i - 1) / (float) samples));
                            int blurColorEdge = ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), alphaFactor);
                            int blurColorMain = ColorUtil.multAlpha(color, alphaFactor);
                            // draw blurred copy shifted further from center (tail)
                            buffer.vertex(matrix4f, middleW - (size / 2f), (posY + off) + size, 0).texture(0f, 1f).color(blurColorEdge);
                            buffer.vertex(matrix4f, middleW + size / 2f, (posY + off) + size, 0).texture(1f, 1f).color(blurColorEdge);
                            buffer.vertex(matrix4f, middleW + size / 2f, (posY + off), 0).texture(1f, 0).color(blurColorMain);
                            buffer.vertex(matrix4f, middleW - (size / 2f), (posY + off), 0).texture(0, 0).color(blurColorMain);
                        }
                    } else if (blurMode.isSelected("Default")) {
                        int rings = Math.max(1, Math.min(20, Math.round(defaultBlurStrength.getValue())));
                        for (int i = rings; i >= 1; i--) {
                            float expand = i * 0.6f; // slight expansion around edges
                            float alpha = Math.max(0.03f, 0.08f * (1.0f - (i - 1) / (float) rings));
                            int glowColor = ColorUtil.multAlpha(color, alpha);
                            // expanded quad for soft edge glow
                            buffer.vertex(matrix4f, middleW - (size / 2f) - expand, (posY + size) + expand, 0).texture(0f, 1f).color(glowColor);
                            buffer.vertex(matrix4f, middleW + (size / 2f) + expand, (posY + size) + expand, 0).texture(1f, 1f).color(glowColor);
                            buffer.vertex(matrix4f, middleW + (size / 2f) + expand, (posY - expand), 0).texture(1f, 0).color(glowColor);
                            buffer.vertex(matrix4f, middleW - (size / 2f) - expand, (posY - expand), 0).texture(0, 0).color(glowColor);
                        }
                    }
                }
                buffer.vertex(matrix4f, middleW - (size / 2f), posY + size, 0).texture(0f, 1f).color(ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), 0.5F));
                buffer.vertex(matrix4f, middleW + size / 2f, posY + size, 0).texture(1f, 1f).color(ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), 0.5F));
                buffer.vertex(matrix4f, middleW + size / 2f, posY, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix4f, middleW - (size / 2f), posY, 0).texture(0, 0).color(color);
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
                matrix.translate(-middleW, -middleH, 0.0F);
                matrix.pop();
            });

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    public static float getRotations(Entity entity) {
        double x = MathUtil.interpolate(entity.prevX, entity.getX()) - MathUtil.interpolate(mc.player.prevX, mc.player.getX());
        double z = MathUtil.interpolate(entity.prevZ, entity.getZ()) - MathUtil.interpolate(mc.player.prevZ, mc.player.getZ());
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }
}
