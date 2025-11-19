package wtf.dettex.api.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dettex.event.EventManager;
import wtf.dettex.event.impl.chat.ChatEvent;
import wtf.dettex.implement.screen.menu.MenuScreen;
import wtf.dettex.implement.features.altmanager.AltManagerScreen;
import wtf.dettex.implement.screen.mainmenu.CustomMainMenu;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", remap = false, ordinal = 1), method = "handleTextClick", cancellable = true)
    public void handleCustomClickEvent(Style style, CallbackInfoReturnable<Boolean> cir) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return;
        }
        EventManager.callEvent(new ChatEvent(clickEvent.getValue()));
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void disableBackgroundBlurAndDimmingForMenu(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object self = this;
        if (self instanceof MenuScreen || self instanceof CustomMainMenu || self instanceof AltManagerScreen) {
            ci.cancel();
            return;
        }
        String n = self.getClass().getName();
        boolean isIngame = n.contains(".screen.ingame.");
        boolean isChat = n.endsWith("ChatScreen") || n.contains(".screen.ChatScreen");
        boolean isPause = n.endsWith("GameMenuScreen");
        if (isIngame || isChat || isPause) {
            // No background for chat/inventory and other ingame screens
            ci.cancel();
            return;
        }
        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
        CustomMainMenu.renderTitleBackground(context, w, h);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void drawMenuBackgroundOnRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object self = this;
        if (self instanceof MenuScreen || self instanceof CustomMainMenu || self instanceof AltManagerScreen) {
            return;
        }
        String n = self.getClass().getName();
        boolean isIngame = n.contains(".screen.ingame.");
        boolean isChat = n.endsWith("ChatScreen") || n.contains(".screen.ChatScreen");
        boolean isPause = n.endsWith("GameMenuScreen");
        if (isIngame || isChat || isPause) {
            return;
        }
        int w = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = MinecraftClient.getInstance().getWindow().getScaledHeight();
        CustomMainMenu.renderTitleBackground(context, w, h);
    }
}
