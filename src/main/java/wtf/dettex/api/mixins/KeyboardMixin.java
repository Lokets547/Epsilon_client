package wtf.dettex.api.mixins;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dettex.event.EventManager;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.event.impl.tab.TabToggleEvent;
import wtf.dettex.implement.screen.menu.MenuScreen;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (key != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            if (action == GLFW.GLFW_RELEASE && key == GLFW.GLFW_KEY_RIGHT_SHIFT && client.currentScreen == null) {
                MenuScreen.INSTANCE.openGui();
            }

            if (key == GLFW.GLFW_KEY_TAB) {
                if (action == GLFW.GLFW_PRESS) {
                    EventManager.callEvent(new TabToggleEvent(true));
                } else if (action == GLFW.GLFW_RELEASE) {
                    EventManager.callEvent(new TabToggleEvent(false));
                }
            }

            EventManager.callEvent(new KeyEvent(client.currentScreen, InputUtil.Type.KEYSYM, key, action));
        }
    }
}
