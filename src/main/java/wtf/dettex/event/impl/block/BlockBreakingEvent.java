package wtf.dettex.event.impl.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import wtf.dettex.event.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {}
