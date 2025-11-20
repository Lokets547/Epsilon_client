package wtf.dettex.api.mixins.accessor;

import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackEntityRenderState.class)
public interface ItemStackEntityRenderStateAccessor {
    @Accessor("itemRenderState")
    ItemRenderState dettex$getItemRenderState();
}

