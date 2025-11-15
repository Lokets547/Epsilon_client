package wtf.dettex.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.player.TickEvent;

import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoDelay extends Module {
    public static NoDelay getInstance() {
        return Instance.get(NoDelay.class);
    }

    public MultiSelectSetting ignoreSetting = new MultiSelectSetting("Type", "Allows the actions you choose")
            .value("Jump", "Right Click", "Break CoolDown", "Exp Bottle");

    public NoDelay() {
        super("NoDelay", "No Delay", ModuleCategory.PLAYER);
        setup(ignoreSetting);
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        if (ignoreSetting.isSelected("Break CoolDown")) mc.interactionManager.blockBreakingCooldown = 0;
        if (ignoreSetting.isSelected("Jump")) mc.player.jumpingCooldown = 0;
        if (ignoreSetting.isSelected("Right Click")) {
            mc.itemUseCooldown = 0;
        } else if (ignoreSetting.isSelected("ExpBottle")) {
            if (mc.player != null) {
                ItemStack main = mc.player.getMainHandStack();
                ItemStack off = mc.player.getOffHandStack();
                boolean holdingExpBottle = (main != null && main.getItem() instanceof ExperienceBottleItem)
                        || (off != null && off.getItem() instanceof ExperienceBottleItem);
                if (holdingExpBottle) {
                    mc.itemUseCooldown = 0;
                }
            }
        }
    }
}