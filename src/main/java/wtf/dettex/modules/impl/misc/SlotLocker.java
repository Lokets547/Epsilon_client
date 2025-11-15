package wtf.dettex.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.common.QuickImports;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.item.ClickSlotEvent;
import wtf.dettex.event.impl.keyboard.KeyEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SlotLocker extends Module implements QuickImports {
    public static SlotLocker getInstance() { return Instance.get(SlotLocker.class); }
    MultiSelectSetting locked = new MultiSelectSetting("Locked Slots", "Prevent dropping from selected hotbar slots")
            .value("1","2","3","4","5","6","7","8","9");

    public SlotLocker() {
        super("SlotLocker", "Slot Locker", ModuleCategory.MISC);
        setup(locked);
    }

    public boolean isLocked(int hotbarIndex01) {
        return locked.isSelected(String.valueOf(hotbarIndex01));
    }

    @EventHandler
     
    public void onClickSlot(ClickSlotEvent e) {
        if (mc.player == null) return;
        if (e.getActionType() != SlotActionType.THROW) return;
        if (e.getSlotId() < 0 || e.getSlotId() >= mc.player.currentScreenHandler.slots.size()) return;

        Slot slot = mc.player.currentScreenHandler.getSlot(e.getSlotId());
        if (slot.inventory instanceof PlayerInventory) {
            int invIndex = slot.getIndex();
            if (invIndex >= 0 && invIndex < 9) {
                int hotbarPos01 = invIndex + 1;
                if (isLocked(hotbarPos01)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
     
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.currentScreen != null) return;
        int dropKeyCode = mc.options.dropKey.getDefaultKey().getCode();
        if (!e.isKeyDown(dropKeyCode)) return;

        int selected = mc.player.getInventory().selectedSlot;
        int pos01 = selected + 1;
        if (isLocked(pos01)) {
            mc.options.dropKey.setPressed(false);
        }
    }
}
