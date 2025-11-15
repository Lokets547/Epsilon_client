package wtf.dettex.api.system.shape.implement;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.render.Backdrop;
import wtf.dettex.modules.impl.render.Hud;

public class BlurGlass extends Blur {
    private static final ShaderProgramKey BLUR_GLASS_SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur_glass"), VertexFormats.POSITION, Defines.EMPTY);
    private static final float DEFAULT_DISTORTION = 0.25F;

    @Override
    public void render(ShapeProperties shape) {
        Hud hud = Hud.getInstance();
        if (!hud.hudType.isSelected("New")) {
            super.render(shape);
            return;
        }
        Framebuffer backdrop = Backdrop.getBuffer();
        boolean fallback = backdrop == null;
        if (fallback) {
            setup();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = 1.0F;
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);

        boolean newHud = hud.hudType.isSelected("New");

        float quality = Math.max(24.0F, shape.getQuality());
        float softness = shape.getSoftness();
        float thickness = newHud ? 0.0F : shape.getThickness();
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX() - softness / 2.0F, shape.getY() - softness / 2.0F, shape.getWidth() + softness, shape.getHeight() + softness);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        if (!fallback && backdrop != null) {
            RenderSystem.bindTexture(backdrop.getColorAttachment());
        } else if (input != null) {
            RenderSystem.bindTexture(input.getColorAttachment());
        }

        ShaderProgram shader = RenderSystem.setShader(BLUR_GLASS_SHADER_KEY);
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(pos.x, mc.getWindow().getFramebufferHeight() - height - pos.y);
        shader.getUniformOrDefault("radius").set(round);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("Quality").set(quality);
        if (!fallback && Backdrop.getResolution() != null) {
            shader.getUniformOrDefault("InputResolution").set(Backdrop.getResolution().x, Backdrop.getResolution().y);
        } else {
            shader.getUniformOrDefault("InputResolution").set(resolution.x, resolution.y);
        }

        float distortion = shape.getStart() != 0.0F ? shape.getStart() : DEFAULT_DISTORTION;
        shader.getUniformOrDefault("Distortion").set(distortion);

        int baseColor = shape.getColor().x;
        float red = ColorUtil.redf(baseColor);
        float green = ColorUtil.greenf(baseColor);
        float blue = ColorUtil.bluef(baseColor);
        float solidAlpha = ColorUtil.alphaf(ColorUtil.multAlpha(baseColor, alpha));

        shader.getUniformOrDefault("color1").set(red, green, blue, solidAlpha);
        shader.getUniformOrDefault("color2").set(red, green, blue, solidAlpha);
        shader.getUniformOrDefault("color3").set(red, green, blue, solidAlpha);
        shader.getUniformOrDefault("color4").set(red, green, blue, solidAlpha);
        int outlineColor = newHud ? 0x00000000 : shape.getOutlineColor();
        shader.getUniformOrDefault("outlineColor").set(ColorUtil.redf(outlineColor), ColorUtil.greenf(outlineColor), ColorUtil.bluef(outlineColor), ColorUtil.alphaf(ColorUtil.multAlpha(outlineColor, alpha)));

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    @Override
    public void setup() {
        Framebuffer buffer = mc.getFramebuffer();
        if (input == null) {
            input = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
        }
        input.beginWrite(false);
        buffer.draw(input.textureWidth, input.textureHeight);
        buffer.beginWrite(false);
        if (input.textureWidth != mc.getWindow().getFramebufferWidth() || input.textureHeight != mc.getWindow().getFramebufferHeight()) {
            input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        }
        resolution.set(buffer.textureWidth, buffer.textureHeight);
    }
}
