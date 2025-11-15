package wtf.dettex.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BindSetting;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.common.util.entity.PlayerIntersectionUtil;
import wtf.dettex.common.util.entity.PlayerInventoryUtil;
import wtf.dettex.common.util.task.scripts.Script;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraHelper extends Module {
    BindSetting elytraSetting = new BindSetting("Elytra Swap", "Changes place chest-plate with elytra");
    BindSetting fireworkSetting = new BindSetting("Use FireWork", "Swaps and uses fireworks");
    BooleanSetting startSetting = new BooleanSetting("Quick Start", "When swapping to elytra, it takes off and uses fireworks").setValue(false);
    Script script = new Script();

    public ElytraHelper() {
        super("ElytraHelper", "Elytra Helper", ModuleCategory.MISC);
        setup(elytraSetting, fireworkSetting, startSetting);
    }

    
    @EventHandler
     
    public void onKey(KeyEvent e) {
        if (!script.isFinished()) return;

        if (e.isKeyDown(elytraSetting.getKey())) {
            Slot slot = chestPlate();
            if (slot != null) {
                Slot fireWork = PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET);
                boolean elytra = slot.getStack().getItem().equals(Items.ELYTRA);
                PlayerInventoryUtil.moveItem(slot, 6, true, true);
                if (startSetting.isValue() && fireWork != null && elytra) script.cleanup().addTickStep(4, () -> {
                    if (mc.player.isOnGround()) mc.player.jump();
                }).addTickStep(3, () -> {
                    PlayerIntersectionUtil.startFallFlying();
                    PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
                });
            }
        } else if (e.isKeyDown(fireworkSetting.getKey()) && mc.player.isGliding()) {
            PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
        }
    }

    @EventHandler
     
    public void onTick(TickEvent e) {
        script.update();
    }

    
    private Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA))
            return PlayerInventoryUtil.getSlot(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE));
        else return PlayerInventoryUtil.getSlot(Items.ELYTRA);
    }
}
