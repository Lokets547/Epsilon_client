package wtf.dettex.modules.impl.misc;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import wtf.dettex.api.mixins.accessor.IPlayerPosition;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.packet.EventPacketNew;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;

public class ServerUtilities extends Module {

   MultiSelectSetting utils = new MultiSelectSetting("Utilities", "Which utilities is going to work").value("NoServerSlot", "NoServerRotate");


    public ServerUtilities() {
        super("ServerUtilities", ModuleCategory.MISC);
        setup(utils);
    }

    @EventHandler
    public void onPacketReceive(EventPacketNew.Receive event) {
        if (utils.isSelected("NoServerSlot") && event.getPacket() instanceof UpdateSelectedSlotS2CPacket) {
            event.cancel();
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }

        if (utils.isSelected("NoServerRotate") && event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
            if (fullNullCheck()) return;

            IPlayerPosition accessor = (IPlayerPosition) (Object) packet;
            accessor.setYaw(mc.player.getYaw());
            accessor.setPitch(mc.player.getPitch());
        }
    }
}


