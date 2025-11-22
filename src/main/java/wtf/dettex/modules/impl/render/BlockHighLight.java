package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.color.ColorUtil;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.render.WorldRenderEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockHighLight extends Module {
    public static BlockHighLight getInstance() {
        return Instance.get(BlockHighLight.class);
    }

    public BlockHighLight() {
        super("BlockHighLight", "Block HighLight", ModuleCategory.RENDER);
    }

    @EventHandler
     
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(HitResult.Type.BLOCK)) {
            BlockPos pos = result.getBlockPos();
            Render3DUtil.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), ColorUtil.getClientColor(), 2, true, true);
        }
    }
}
