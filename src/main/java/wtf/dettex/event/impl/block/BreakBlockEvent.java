package wtf.dettex.event.impl.block;

import net.minecraft.util.math.BlockPos;
import wtf.dettex.event.events.Event;

public record BreakBlockEvent(BlockPos blockPos) implements Event {}
