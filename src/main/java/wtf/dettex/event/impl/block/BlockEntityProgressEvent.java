package wtf.dettex.event.impl.block;

import net.minecraft.block.entity.BlockEntity;
import wtf.dettex.event.events.Event;

public record BlockEntityProgressEvent(BlockEntity blockEntity, Type type) implements Event {
    public enum Type {
        ADD, REMOVE
    }
}

