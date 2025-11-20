package wtf.dettex.modules.impl.movement;

import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.event.impl.player.TickEvent;

public class WaterLeave extends Module {
    public final ValueSetting boostStrength = new ValueSetting("Сила выброса", "Сила выброса из воды").range(0.1f, 2.0f).setValue(0.8f);

    public WaterLeave() {
        super("WaterLeave",
                "Water Leave",
                ModuleCategory.MOVEMENT);
        setup(boostStrength);
    }

    @EventHandler
     
    public void onEventUpdate(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.isTouchingWater()) {
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos blockBelow = playerPos.down();

        boolean isSoulSandBelow = mc.world.getBlockState(blockBelow).getBlock() == Blocks.SOUL_SAND;

        boolean isInWaterBlock = mc.world.getFluidState(playerPos).getFluid() == Fluids.WATER
                || mc.world.getFluidState(playerPos.up()).getFluid() == Fluids.WATER;

        if (isSoulSandBelow && isInWaterBlock) {
            var vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x, boostStrength.getValue(), vel.z);
        }
    }
}

