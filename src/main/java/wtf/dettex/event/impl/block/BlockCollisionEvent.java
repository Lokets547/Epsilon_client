package wtf.dettex.event.impl.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.event.events.Event;

public record BlockCollisionEvent(BlockPos blockPos, BlockState state) implements Event {}

