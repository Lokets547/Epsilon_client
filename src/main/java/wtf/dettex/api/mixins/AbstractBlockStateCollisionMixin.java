package wtf.dettex.api.mixins;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dettex.modules.impl.movement.Phase;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateCollisionMixin {

    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void zenith$phaseRemoveXZ(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (Phase.isShapeModeActiveStatic()) {
            VoxelShape original = cir.getReturnValue();
            double minY = original.getMin(Direction.Axis.Y);
            double maxY = original.getMax(Direction.Axis.Y);
            if (minY >= maxY) maxY = minY + 0.001;
            // Zero-thickness XZ shape to minimize horizontal collision while keeping some vertical extent
            VoxelShape finalShape = VoxelShapes.cuboid(0.0, minY, 0.0, 0.0, maxY, 0.0);
            cir.setReturnValue(finalShape);
        }
    }
}
