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
        // КРИТИЧЕСКАЯ ОПТИМИЗАЦИЯ: используем setupIfNeeded() вместо setup()
        // Это гарантирует только ОДИН вызов setup() за кадр вместо 42+
        setupIfNeeded();
        
        // Группируем GL state changes
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = 1.0F;
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        
        // Используем кэшированные векторы из родительского класса
        matrix4f.transformPosition(shape.getX(), shape.getY(), 0, tempPos).mul(scale);
        matrix4f.getScale(tempSize).mul(scale);
        shape.getRound().mul(tempSize.y, tempRound);

        // Always render using glass shader with Backdrop input for consistent GUI regardless of other modules
        boolean newHud = false; // preserve provided thickness/outline from shape

        float quality = Math.max(24.0F, shape.getQuality());
        float softness = shape.getSoftness();
        float thickness = newHud ? 0.0F : shape.getThickness();
        float width = shape.getWidth() * tempSize.x;
        float height = shape.getHeight() * tempSize.y;
        float softnessHalf = softness * 0.5f;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX() - softnessHalf, shape.getY() - softnessHalf, shape.getWidth() + softness, shape.getHeight() + softness);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        // ВАЖНО: Проверка на null перед использованием (предотвращает мигание черным)
        if (input != null) {
            RenderSystem.bindTexture(input.getColorAttachment());
        } else {
            // Если framebuffer все еще не готов, пропускаем этот render
            return;
        }

        ShaderProgram shader = RenderSystem.setShader(BLUR_GLASS_SHADER_KEY);
        
        // Оптимизация: группируем все uniform вызовы
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(tempPos.x, mc.getWindow().getFramebufferHeight() - height - tempPos.y);
        shader.getUniformOrDefault("radius").set(tempRound);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("Quality").set(quality);
        shader.getUniformOrDefault("InputResolution").set(resolution.x, resolution.y);

        float distortion = shape.getStart() != 0.0F ? shape.getStart() : DEFAULT_DISTORTION;
        shader.getUniformOrDefault("Distortion").set(distortion);

        // Оптимизация: вычисляем компоненты цвета один раз и переиспользуем
        int baseColor = shape.getColor().x;
        int finalBaseColor = ColorUtil.multAlpha(baseColor, alpha);
        colorCache[0] = ColorUtil.redf(finalBaseColor);
        colorCache[1] = ColorUtil.greenf(finalBaseColor);
        colorCache[2] = ColorUtil.bluef(finalBaseColor);
        colorCache[3] = ColorUtil.alphaf(finalBaseColor);

        shader.getUniformOrDefault("color1").set(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);
        shader.getUniformOrDefault("color2").set(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);
        shader.getUniformOrDefault("color3").set(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);
        shader.getUniformOrDefault("color4").set(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);
        
        // Outline color
        setColorUniform(shader, "outlineColor", shape.getOutlineColor(), alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    // Удаляем переопределение setup() - используем оптимизированную версию из родительского Blur
    // которая вызывается только ОДИН раз за кадр благодаря setupIfNeeded()
}
