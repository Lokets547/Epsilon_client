package wtf.dettex.api.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dettex.common.QuickImports;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin implements QuickImports {
    @Shadow
    public abstract boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks);

    @Inject(method = "usageTick", at = @At(value = "RETURN"))
    public void usageTickHook(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (this.onStoppedUsing(stack, world, user, remainingUseTicks)) mc.options.useKey.setPressed(false);
    }
}