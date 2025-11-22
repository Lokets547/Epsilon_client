package wtf.dettex.common;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import wtf.dettex.api.system.draw.DrawEngine;
import wtf.dettex.api.system.draw.DrawEngineImpl;
import wtf.dettex.api.system.shape.implement.*;
import wtf.dettex.api.system.shape.implement.*;
import wtf.dettex.implement.screen.menu.components.implement.window.WindowManager;

public interface QuickImports extends QuickLogger {
    MinecraftClient mc = MinecraftClient.getInstance();
    RenderTickCounter tickCounter = mc.getRenderTickCounter();
    Window window = mc.getWindow();

    Tessellator tessellator = Tessellator.getInstance();
    DrawEngine drawEngine = new DrawEngineImpl();

    Rectangle rectangle = new Rectangle();
    Blur blur = new Blur();
    BlurGlass blurGlass = new BlurGlass();
    Arc arc = new Arc();
    Image image = new Image();
    LiquidGlass liquidGlass = new LiquidGlass();

    Gson gson = new Gson();

    WindowManager windowManager = new WindowManager();
}

