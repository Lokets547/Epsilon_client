package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;
import wtf.dettex.common.util.render.Render3DUtil;
import wtf.dettex.event.impl.block.BlockUpdateEvent;
import wtf.dettex.event.impl.render.WorldLoadEvent;
import wtf.dettex.event.impl.render.WorldRenderEvent;

import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XRay extends Module {
    Map<BlockPos, BlockState> map = new HashMap<>();

    SelectSetting modeSetting = new SelectSetting("Mode", "Operating mode for XRay").value("Block Update");
    MultiSelectSetting blockTypeSetting = new MultiSelectSetting("Blocks", "Blocks that will be displayed")
            .value("Ancient Debris", "Diamond", "Emerald", "Iron", "Gold");

    public XRay() {
        super("XRay", ModuleCategory.RENDER);
        setup(modeSetting, blockTypeSetting);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        map.forEach((key, value) -> {
            if (blockTypeSetting.getSelected().toString().toLowerCase().contains(getBlockName(value))) {
                Render3DUtil.drawBox(new Box(key), getColorByBlock(value), 1);
            }
        });
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        map.clear();
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent e) {
        BlockState state = e.state();
        BlockPos pos = e.pos();
        switch (e.type()) {
            case BlockUpdateEvent.Type.UPDATE -> {
                if (getColorByBlock(state) != -1 && !map.containsKey(pos)) map.put(pos, state);
                if (map.containsKey(pos) && !map.get(pos).equals(state)) map.remove(pos);
            }
            case BlockUpdateEvent.Type.UNLOAD -> map.remove(pos);
        }
    }
    
    private int getColorByBlock(BlockState block) {
        return switch (getBlockName(block)) {
            case "ancient debris" -> 0xFFA67554;
            case "diamond" -> 0xFF197B81;
            case "emerald" -> 0xFF41871B;
            case "iron" -> 0xFF754C1F;
            case "gold" -> 0xFFC5B938;
            default -> -1;
        };
    }

    private String getBlockName(BlockState state) {
        return state.getBlock().asItem().toString().replace("minecraft:", "").replace("_ore", "").replace("_", " ");
    }
}

