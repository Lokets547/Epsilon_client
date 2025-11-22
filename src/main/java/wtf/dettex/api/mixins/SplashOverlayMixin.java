package wtf.dettex.api.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    // Force the background fill color in the splash screen to black while preserving alpha/fade
    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"),
            index = 4
    )
    private int dettex$useBlackBackground(int color) {
        // Preserve alpha, force RGB to 0x000000 (black)
        return (color & 0xFF000000);
    }
}

