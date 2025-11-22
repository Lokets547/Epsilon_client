package wtf.dettex.api.mixins;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wtf.dettex.api.interfaces.ItemPhysicState;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemPhysicState {
    @Unique
    private boolean dettex$itemOnGround;

    @Override
    public boolean dettex$isItemOnGround() {
        return dettex$itemOnGround;
    }

    @Override
    public void dettex$setItemOnGround(boolean value) {
        this.dettex$itemOnGround = value;
    }
}

