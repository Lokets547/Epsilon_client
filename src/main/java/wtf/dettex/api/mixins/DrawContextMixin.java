package wtf.dettex.api.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public class DrawContextMixin {
    private boolean shouldBlockFullscreen() {
        Screen s = MinecraftClient.getInstance().currentScreen;
        if (s == null) return false;
        // Allow our own screens to manage their background freely
        if (s instanceof wtf.dettex.implement.screen.mainmenu.CustomMainMenu
                || s instanceof wtf.dettex.implement.screen.menu.MenuScreen
                || s instanceof wtf.dettex.implement.features.altmanager.AltManagerScreen
                || s.getClass().getName().startsWith("wtf.dettex")) {
            return false;
        }
        // Block fullscreen fills for all other (vanilla/third-party) screens
        return true;
    }

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true)
    private void hookFill(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        if (!shouldBlockFullscreen()) return;
        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
        if (x1 <= 0 && y1 <= 0 && x2 >= w && y2 >= h) {
            ci.cancel();
        }
    }

    @Inject(method = "fillGradient(IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void hookFillGradient(int x1, int y1, int x2, int y2, int c1, int c2, CallbackInfo ci) {
        if (!shouldBlockFullscreen()) return;
        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
        if (x1 <= 0 && y1 <= 0 && x2 >= w && y2 >= h) {
            ci.cancel();
        }
    }
}

