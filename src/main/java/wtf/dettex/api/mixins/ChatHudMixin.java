package wtf.dettex.api.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dettex.event.EventManager;
import wtf.dettex.event.impl.chat.ChatReceiveEvent;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("TAIL"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (client != null && client.world != null) {
            EventManager.callEvent(new ChatReceiveEvent(message));
        }
    }
}

