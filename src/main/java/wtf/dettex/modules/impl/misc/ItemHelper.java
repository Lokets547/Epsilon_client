package wtf.dettex.modules.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BindSetting;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.entity.PlayerInventoryComponent;
import wtf.dettex.common.util.item.ItemUsage;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemHelper extends Module {
    List<KeyBind> keyBindings = new ArrayList<>();
    Map<KeyBind, SwapState> states = new HashMap<>();

    public ItemHelper() {
        super("ItemHelper", "Item Helper", ModuleCategory.MISC);
        keyBindings.add(new KeyBind(Items.ENCHANTED_GOLDEN_APPLE, new BindSetting("Enchanted Golden Apple", "Use enchanted golden apple")));
        keyBindings.add(new KeyBind(Items.GOLDEN_APPLE, new BindSetting("Golden Apple", "Use golden apple")));
        keyBindings.add(new KeyBind(Items.CHORUS_FRUIT, new BindSetting("Chorus Fruit", "Use chorus fruit")));
        keyBindings.add(new KeyBind(Items.GOLDEN_CARROT, new BindSetting("Golden Carrot", "Use golden carrot")));
        keyBindings.forEach(bind -> setup(bind.setting));
        keyBindings.forEach(bind -> states.put(bind, new SwapState()));
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        keyBindings.stream().filter(bind -> e.isKeyReleased(bind.setting.getKey())).forEach(this::handleToggle);
    }

    private void handleToggle(KeyBind bind) {
        SwapState state = states.get(bind);
        if (state == null) return;

        if (!state.active) {
            // First press: bring item to main hand and start using; remember where to return
            Slot slot = PlayerInventoryUtil.getSlot(bind.item);
            if (slot == null) return;
            state.slotId = slot.id;
            state.button = mc.player.getInventory().selectedSlot;
            PlayerInventoryComponent.addTask(() -> {
                PlayerInventoryUtil.swap(slot, state.button, true);
                ItemUsage.INSTANCE.useHand(Hand.MAIN_HAND);
            });
            state.active = true;
        } else {
            // Second press: swap back using remembered slot/button
            Slot originalSlot = PlayerInventoryUtil.getSlot(s -> s.id == state.slotId);
            if (originalSlot != null) {
                PlayerInventoryComponent.addTask(() -> PlayerInventoryUtil.swap(originalSlot, state.button, true));
            }
            state.active = false;
        }
    }

    public record KeyBind(Item item, BindSetting setting) {}

    private static class SwapState {
        int slotId = -1;
        int button = 0;
        boolean active = false;
    }
}
