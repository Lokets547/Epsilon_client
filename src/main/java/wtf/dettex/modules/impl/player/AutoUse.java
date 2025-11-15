package wtf.dettex.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryComponent;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.item.ItemUsage;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.impl.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoUse extends Module {
    Script script = new Script();

    MultiSelectSetting multiSetting = new MultiSelectSetting("Mode", "Choose what will be used").value("Eat", "Invisibility");

    public AutoUse() {
        super("AutoUse", "Auto Use", ModuleCategory.PLAYER);
        setup(multiSetting);
    }

    @Override
     
    public void deactivate() {
        script.update();
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        for (String string : multiSetting.getSelected())
            switch (string) {
                case "Eat" -> {
                    Slot slot = PlayerInventoryUtil.getFoodMaxSaturationSlot();
                    if (slot != null && mc.player.getHungerManager().isNotFull() && swapAndEat(slot)) {
                        return;
                    }
                }
                case "Invisibility" -> {
                    Slot slot = PlayerInventoryUtil.getPotion(StatusEffects.INVISIBILITY);
                    if (slot != null && !PlayerIntersectionUtil.isPotionActive(StatusEffects.INVISIBILITY) && swapAndEat(slot)) {
                        return;
                    }
                }
            }
        script.update();
    }

    public boolean swapAndEat(Slot slot) {
        ItemStack stack = slot.getStack();
        if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            if (!mc.player.getOffHandStack().equals(stack)) {
                if (PlayerInventoryComponent.script.isFinished()) {
                    PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true, true);
                    script.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true, true));
                }
            } else {
                ItemUsage.INSTANCE.useHand(Hand.OFF_HAND);
            }
            return true;
        }
        return false;
    }
}
