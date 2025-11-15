package wtf.dettex.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.other.StopWatch;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.event.impl.container.HandledScreenEvent;
import wtf.dettex.event.impl.item.ClickSlotEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemTweaks extends Module {
    StopWatch stopWatch = new StopWatch();

    ValueSetting scrollerSetting = new ValueSetting("Item Scroller delay", "Select Item Scroller delay").setValue(100).range(0, 200);

    public ItemTweaks() {
        super("ItemTweaks","Item Scroller", ModuleCategory.MISC);
        setup(scrollerSetting);
    }

    @EventHandler
    
    public void onHandledScreen(HandledScreenEvent e) {
        Slot hoverSlot = e.getSlotHover();
        SlotActionType actionType = PlayerIntersectionUtil.isKey(mc.options.dropKey) ? SlotActionType.THROW : PlayerIntersectionUtil.isKey(mc.options.attackKey) ? SlotActionType.QUICK_MOVE : null;

        if (PlayerIntersectionUtil.isKey(mc.options.sneakKey) && !PlayerIntersectionUtil.isKey(mc.options.sprintKey) && hoverSlot != null && hoverSlot.hasStack() && actionType != null && stopWatch.every(scrollerSetting.getValue())) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoverSlot.id, actionType.equals(SlotActionType.THROW) ? 1 : 0, actionType, mc.player);
        }
    }

    @EventHandler
     
    public void onClickSlot(ClickSlotEvent e) {
        int slotId = e.getSlotId();
        if (slotId < 0 || slotId > mc.player.currentScreenHandler.slots.size()) return;
        Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
        Item item = slot.getStack().getItem();

        if (item != null && PlayerIntersectionUtil.isKey(mc.options.sneakKey) && PlayerIntersectionUtil.isKey(mc.options.sprintKey) && stopWatch.every(50)) {
            PlayerInventoryUtil.slots().filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
                        .forEach(s -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, s.id, 1, e.getActionType(), mc.player));
        }
    }
}

