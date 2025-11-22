package wtf.dettex.event.impl.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.event.events.Event;

public record BlockUpdateEvent(BlockState state, BlockPos pos, Type type) implements Event {
    public enum Type {
        LOAD, UNLOAD, UPDATE
    }
}

