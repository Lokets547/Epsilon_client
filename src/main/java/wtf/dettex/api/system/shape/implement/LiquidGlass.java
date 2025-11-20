package wtf.dettex.api.system.shape.implement;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;
import wtf.dettex.api.system.shape.Shape;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.render.Backdrop;
import wtf.dettex.modules.impl.render.Hud;


public class LiquidGlass extends Blur implements Shape, QuickImports {

    private final Rectangle rectangle = new Rectangle();
    private final ShaderProgramKey GLASS_SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/liquid_glass"), VertexFormats.POSITION, Defines.EMPTY);

    @Override
    public void render(ShapeProperties shape) {
        Framebuffer worldOnly = Backdrop.getBuffer();
        boolean fallback = (worldOnly == null);
        if (fallback) {
            setup();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = 1.0F; // Don't inherit global shader alpha
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y - 1F);

        boolean liquidMode = Hud.hudType.isSelected("Liquid Glass");
        float quality = (liquidMode && Hud.glassBlur.isValue()) ? Math.max(16f, Hud.glassBlurValue.getValue() * 8f) : Math.max(24f, shape.getQuality());
        float softness = shape.getSoftness();
        float thickness = Math.max(0f, shape.getThickness());
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX() - softness / 2, shape.getY() - softness / 2, shape.getWidth() + softness, shape.getHeight() + softness);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        if (!fallback) {
            RenderSystem.bindTexture(worldOnly.getColorAttachment());
        } else if (input != null) {
            RenderSystem.bindTexture(input.getColorAttachment());
        }
        ShaderProgram shader = RenderSystem.setShader(GLASS_SHADER_KEY);
        shader.getUniformOrDefault("size").set(width, height);
        float fbHeight = mc.getWindow().getFramebufferHeight();
        shader.getUniformOrDefault("location").set(pos.x, fbHeight - height - pos.y);
        shader.getUniformOrDefault("radius").set(round);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("Quality").set(quality);
        if (!fallback) {
            shader.getUniformOrDefault("InputResolution").set(Backdrop.getResolution().x, Backdrop.getResolution().y);
        } else {
            shader.getUniformOrDefault("InputResolution").set(resolution.x, resolution.y);
        }

        shader.getUniformOrDefault("Distortion").set(0.22f);
        float blurRadius = (liquidMode && Hud.glassBlur.isValue()) ? Hud.glassBlurValue.getValue() : 1.25f;
        shader.getUniformOrDefault("BlurRadius").set(blurRadius);
        shader.getUniformOrDefault("EdgeFeather").set(1.0f);

        shader.getUniformOrDefault("color1").set(0, 0, 0, 0);
        shader.getUniformOrDefault("color2").set(0, 0, 0, 0);
        shader.getUniformOrDefault("color3").set(0, 0, 0, 0);
        shader.getUniformOrDefault("outlineColor").set(0, 0, 0, 0);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();

        if (liquidMode) {
            int highlight = ColorUtil.getClientColor();
            ShapeProperties highlightProps = shape.toBuilder()
                    .color(0x00000000, 0x00000000, 0x00000000, 0x00000000)
                    .outlineColor(highlight)
                    .thickness(Hud.glassOutlineThickness.getValue())
                    .softness(Math.max(Hud.glassOutlineSoftness.getValue(), shape.getSoftness()))
                    .build();

            rectangle.render(highlightProps);
        }
    }
}

