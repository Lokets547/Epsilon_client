package wtf.dettex.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.player.TickEvent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BowSpam extends Module {
    ValueSetting delaySetting = new ValueSetting("Delay", "Ticks before releasing the bow")
            .setValue(3f)
            .range(0, 20);

    public BowSpam() {
        super("BowSpam", "Bow Spam", ModuleCategory.COMBAT);
        setup(delaySetting);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (fullNullCheck()) return;

        boolean mainHandBow = mc.player.getMainHandStack().isOf(Items.BOW);
        boolean offHandBow = mc.player.getOffHandStack().isOf(Items.BOW);

        if (!mc.player.isUsingItem() || (!mainHandBow && !offHandBow)) return;

        if (mc.player.getItemUseTime() >= delaySetting.getInt()) {
            Hand hand = mainHandBow ? Hand.MAIN_HAND : Hand.OFF_HAND;

            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, mc.player.getBlockPos(), mc.player.getHorizontalFacing()));
            PlayerIntersectionUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(hand, sequence, mc.player.getYaw(), mc.player.getPitch()));
            mc.player.stopUsingItem();
        }
    }
}

