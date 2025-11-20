package wtf.dettex.api.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dettex.modules.impl.movement.Timer;

@Environment(EnvType.CLIENT)
@Mixin(RenderTickCounter.Dynamic.class)
public abstract class RenderTickCounterMixin {

    @Inject(method = "beginRenderTick", at = @At("RETURN"), cancellable = true)
    private void dettex$scaleDelta(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        Timer t = Timer.getInstance();
        if (t == null || !t.state) return;

        if (!t.isGrim()) {
            float mul = Math.max(0.1F, t.getTimerAmountValue());
            if (mul <= 1.0f) return;
            int boosted = Math.max(original, (int) Math.ceil(original * mul));
            cir.setReturnValue(boosted);
            return;
        }

        float target = Math.max(1.0F, t.getTimerAmountValue());
        float budget = Math.max(0.0F, t.getMaxViolation() - t.getViolation()) / t.getMaxViolation();
        float mul = 1.0F + (target - 1.0F) * budget;
        if (mul <= 1.0f || original <= 0) return;

        int extra = (int) Math.floor((mul - 1.0F) * original + 0.0001F);
        extra = Math.max(1, extra);
        int capped = Math.min(10, original + extra);
        cir.setReturnValue(capped);
    }
}

