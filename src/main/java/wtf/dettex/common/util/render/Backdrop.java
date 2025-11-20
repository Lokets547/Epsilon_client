package wtf.dettex.common.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Vector2f;

public final class Backdrop {
    private static SimpleFramebuffer buffer;
    private static final Vector2f resolution = new Vector2f(1f, 1f);

    private Backdrop() {}

    public static void capture() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        Framebuffer main = mc.getFramebuffer();
        if (buffer == null) {
            buffer = new SimpleFramebuffer(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), false);
        }
        if (buffer.textureWidth != mc.getWindow().getFramebufferWidth() || buffer.textureHeight != mc.getWindow().getFramebufferHeight()) {
            buffer.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        }

        buffer.beginWrite(false);
        main.draw(buffer.textureWidth, buffer.textureHeight);
        main.beginWrite(false);

        resolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
    }

    public static Framebuffer getBuffer() {
        return buffer;
    }

    public static Vector2f getResolution() {
        return new Vector2f(resolution);
    }
}

