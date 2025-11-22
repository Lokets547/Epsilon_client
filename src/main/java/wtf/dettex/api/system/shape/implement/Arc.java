package wtf.dettex.api.system.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import wtf.dettex.api.system.shape.Shape;
import wtf.dettex.api.system.shape.ShapeProperties;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.color.ColorUtil;

public class Arc implements Shape, QuickImports {
    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/arc"), VertexFormats.POSITION, Defines.EMPTY);
    
    // Кэшируем временные векторы для избежания аллокаций
    private final Vector3f tempPos = new Vector3f();
    private final Vector3f tempSize = new Vector3f();
    private final Vector4f tempRound = new Vector4f();
    private final float[] colorCache = new float[4];

    @Override
    public void render(ShapeProperties shape) {
        // Группируем GL state changes
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = 1.0F;

        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        
        // Используем кэшированные векторы
        matrix4f.transformPosition(shape.getX(), shape.getY(), 0, tempPos).mul(scale);
        matrix4f.getScale(tempSize).mul(scale);
        shape.getRound().mul(tempSize.y, tempRound);

        float width = shape.getWidth() * tempSize.x;
        float height = shape.getHeight() * tempSize.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        
        // Оптимизация: группируем все uniform вызовы
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(tempPos.x, window.getHeight() - height - tempPos.y);
        shader.getUniformOrDefault("radius").set(tempRound.x);
        shader.getUniformOrDefault("thickness").set(shape.getThickness());
        shader.getUniformOrDefault("start").set(shape.getStart());
        shader.getUniformOrDefault("end").set(shape.getEnd());
        
        // Оптимизация цветов: вычисляем компоненты один раз
        setColorUniform(shader, "color1", shape.getColor().x, alpha);
        setColorUniform(shader, "color2", shape.getColor().y, alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
    
    // Вспомогательный метод для установки цвета
    private void setColorUniform(ShaderProgram shader, String name, int color, float alpha) {
        int finalColor = ColorUtil.multAlpha(color, alpha);
        colorCache[0] = ColorUtil.redf(finalColor);
        colorCache[1] = ColorUtil.greenf(finalColor);
        colorCache[2] = ColorUtil.bluef(finalColor);
        colorCache[3] = ColorUtil.alphaf(finalColor);
        shader.getUniformOrDefault(name).set(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);
    }
}
