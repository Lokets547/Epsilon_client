package wtf.dettex.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.event.impl.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AirPlace extends Module {

    ValueSetting range = new ValueSetting("Range", "Max blocks ahead to place")
            .range(1f, 5f).setValue(3f);
    BooleanSetting onlyBlocks = new BooleanSetting("Only Blocks", "Only when holding a block").setValue(true);
    BooleanSetting useMainHand = new BooleanSetting("Main Hand", "Use main hand for placement").setValue(true);

    public AirPlace() {
        super("AirPlace", ModuleCategory.PLAYER);
        setup(range, onlyBlocks, useMainHand);
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!PlayerIntersectionUtil.isKey(mc.options.useKey)) return;

        Hand hand = useMainHand.isValue() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack held = mc.player.getStackInHand(hand);
        if (onlyBlocks.isValue() && !(held.getItem() instanceof BlockItem)) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVector();
        Vec3d target = eye.add(look.multiply(range.getValue()));

        Direction facing = Direction.getFacing(look.x, 0, look.z);
        BlockPos neighbor = BlockPos.ofFloored(eye).offset(facing);
        BlockPos placePos = neighbor.offset(facing);

        if (mc.world.getBlockState(neighbor).isAir()) {
            neighbor = mc.player.getBlockPos();
            placePos = neighbor.up();
            facing = Direction.UP;
        }

        if (!mc.world.getBlockState(placePos).isReplaceable()) return;
        Vec3d hit = Vec3d.ofCenter(placePos);
        BlockHitResult bhr = new BlockHitResult(hit, facing.getOpposite(), neighbor, false);

        mc.interactionManager.interactBlock(mc.player, hand, bhr);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, neighbor, facing));
    }
}
