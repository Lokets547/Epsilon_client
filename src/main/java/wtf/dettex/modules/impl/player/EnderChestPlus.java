package wtf.dettex.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BindSetting;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.event.impl.container.CloseScreenEvent;
import wtf.dettex.event.impl.container.SetScreenEvent;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.event.impl.packet.PacketEvent;
import wtf.dettex.implement.features.draggables.Notifications;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnderChestPlus extends Module {
    private HandledScreen<?> screen;
    private final BindSetting bindSetting = new BindSetting("Fold Items Button", "Puts all items in the ender chest");

    public EnderChestPlus() {
        super("EnderChestPlus", "Ender-Chest Plus", ModuleCategory.PLAYER);
        setup(bindSetting);
    }

    @Override
     
    public void deactivate() {
        if (screen != null) {
            screen = null;
            PlayerInventoryUtil.closeScreen(true);
            Notifications.getInstance().addList("Ender Chest - " + Formatting.RED + "закрыт", 5000);
        }
    }

    @EventHandler
     
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(bindSetting.getKey()) && screen != null) {
            List<Slot> slots = mc.player.currentScreenHandler.slots;
            slots.stream().filter(s -> s.id < slots.size() - 36 && s.getStack().isEmpty())
                    .findFirst().ifPresent(s -> PlayerInventoryUtil.swapHand(s, Hand.OFF_HAND, false));
            slots.stream().filter(s -> s.id >= slots.size() - 36 && !s.getStack().isEmpty())
                    .forEach(s -> PlayerInventoryUtil.clickSlot(s, 0, SlotActionType.QUICK_MOVE, false));
        }
    }

    @EventHandler
     
    public void onPacket(PacketEvent e) {
        if (screen != null && mc.player != null) switch (e.getPacket()) {
            case GameJoinS2CPacket join -> deactivate();
            case OpenScreenS2CPacket open -> deactivate();
            case CloseScreenS2CPacket close -> deactivate();
            case PlayerRespawnS2CPacket respawn -> deactivate();
            case CloseHandledScreenC2SPacket close -> e.cancel();
            case PlayerActionC2SPacket player when player.getAction().equals(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) -> {
                PlayerInventoryUtil.swapHand(PlayerInventoryUtil.mainHandSlot(), Hand.OFF_HAND, false);
                e.cancel();
            }
            default -> {}
        }
    }

    @EventHandler
     
    public void onSetScreen(SetScreenEvent e) {
        if (e.getScreen() instanceof InventoryScreen && screen != null) {
            e.setScreen(screen);
        }
    }
    
    @EventHandler
     
    public void onCloseScreen(CloseScreenEvent e) {
        if (e.getScreen() instanceof GenericContainerScreen scr && scr.getTitle().getString().contains(Text.translatable("container.enderchest").getString())) {
            screen = scr;
        }
        if (screen != null) {
            mc.setScreen(null);
            e.cancel();
        }
    }
}
