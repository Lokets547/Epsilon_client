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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;
import wtf.dettex.api.system.shape.Shape;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;

public class Blur implements Shape, QuickImports {
    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur"), VertexFormats.POSITION, Defines.EMPTY);
    
    // КРИТИЧЕСКИ ВАЖНО: Общий framebuffer для всех экземпляров Blur/BlurGlass
    // Иначе один экземпляр может иметь null framebuffer когда другой вызвал setup()
    protected static Framebuffer input;
    protected static Vector2f resolution = new Vector2f();
    
    // Кэшируем временные векторы для избежания аллокаций
    protected final Vector3f tempPos = new Vector3f();
    protected final Vector3f tempSize = new Vector3f();
    protected final Vector4f tempRound = new Vector4f();
    protected final float[] colorCache = new float[4];
    
    // Отслеживание последнего размера для оптимизации setup()
    private static int lastFramebufferWidth = -1;
    private static int lastFramebufferHeight = -1;
    
    // КРИТИЧЕСКАЯ ОПТИМИЗАЦИЯ: кэш для предотвращения множественных setup() вызовов за кадр
    private static long lastSetupFrame = -1;
    private static int currentFrameSetupCount = 0;

    @Override
    public void render(ShapeProperties shape) {
        // Группируем GL state changes
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        // Оптимизация: вызываем setup только когда необходимо
        setupIfNeeded();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = 1.0F;
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        
        // Используем кэшированные векторы
        matrix4f.transformPosition(shape.getX(), shape.getY(), 0, tempPos).mul(scale);
        matrix4f.getScale(tempSize).mul(scale);
        shape.getRound().mul(tempSize.y, tempRound);
        
        float quality = shape.getQuality();
        float softness = shape.getSoftness();
        float thickness = shape.getThickness();
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
        
        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        
        // Оптимизация: группируем все uniform вызовы
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(tempPos.x, mc.getWindow().getFramebufferHeight() - height - tempPos.y);
        shader.getUniformOrDefault("radius").set(tempRound);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("Quality").set(quality);
        shader.getUniformOrDefault("InputResolution").set(resolution.x, resolution.y);
        
        // Оптимизация цветов
        setColorUniform(shader, "color1", shape.getColor().x, alpha);
        setColorUniform(shader, "color2", shape.getColor().y, alpha);
        setColorUniform(shader, "color3", shape.getColor().z, alpha);
        setColorUniform(shader, "color4", shape.getColor().w, alpha);
        setColorUniform(shader, "outlineColor", shape.getOutlineColor(), alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
    
    // Вспомогательный метод для установки цвета
    protected void setColorUniform(ShaderProgram shader, String name, int color, float alpha) {
        int finalColor = ColorUtil.multAlpha(color, alpha);
        colorCache[0] = ColorUtil.redf(finalColor);
        colorCache[1] = ColorUtil.greenf(finalColor);
        colorCache[2] = ColorUtil.bluef(finalColor);
        colorCache[3] = ColorUtil.alphaf(finalColor);
        shader.getUniformOrDefault(name).set(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);
    }
    
    // КРИТИЧЕСКАЯ ОПТИМИЗАЦИЯ: вызываем setup только ОДИН раз за кадр
    protected void setupIfNeeded() {
        long currentFrame = System.nanoTime() / 16_666_666; // ~60fps frames
        int currentWidth = mc.getWindow().getFramebufferWidth();
        int currentHeight = mc.getWindow().getFramebufferHeight();
        
        // Новый кадр - сбрасываем счетчик
        if (lastSetupFrame != currentFrame) {
            lastSetupFrame = currentFrame;
            currentFrameSetupCount = 0;
        }
        
        // ВАЖНО: Всегда вызываем setup если framebuffer не инициализирован (исправляет мигание)
        boolean needsSetup = input == null 
            || currentFrameSetupCount == 0 
            || lastFramebufferWidth != currentWidth 
            || lastFramebufferHeight != currentHeight;
        
        if (needsSetup) {
            setup();
            currentFrameSetupCount++;
            lastFramebufferWidth = currentWidth;
            lastFramebufferHeight = currentHeight;
        }
    }

    public void setup() {
        try {
            Framebuffer buffer = mc.getFramebuffer();
            if (buffer == null) return; // Защита от NPE
            
            int fbWidth = mc.getWindow().getFramebufferWidth();
            int fbHeight = mc.getWindow().getFramebufferHeight();
            
            // Создаем или изменяем размер framebuffer
            if (input == null) {
                input = new SimpleFramebuffer(fbWidth, fbHeight, false);
            } else if (input.textureWidth != fbWidth || input.textureHeight != fbHeight) {
                input.resize(fbWidth, fbHeight);
            }
            
            // Копируем framebuffer (оптимизировано - только 1 раз за кадр)
            input.beginWrite(false);
            buffer.draw(input.textureWidth, input.textureHeight);
            buffer.beginWrite(false);
            
            resolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
        } catch (Exception e) {
            // Предотвращаем краш если что-то пошло не так
            e.printStackTrace();
        }
    }
}

