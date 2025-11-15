package wtf.dettex.modules.impl.player;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.RegistryKeys;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.SelectSetting;

public class PerfectDelay extends Module {
        public PerfectDelay() {
            super("PerfectDelay", ModuleCategory.PLAYER);
            setup(zov);
        }
        private final MultiSelectSetting zov = new MultiSelectSetting("Items", "Perfect delay for").value("Bow", "Crossbow", "Trident");

        private float getEnchantLevel(ItemStack stack) {
            return mc.world == null
                    ? 0.0F
                    : EnchantmentHelper.getLevel(mc.world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getEntry(Enchantments.QUICK_CHARGE.getValue())
                    .orElseThrow(), stack);
        }

        public void onUpdate() {
            if (mc.player == null || mc.player.getActiveItem().isEmpty()) return;

            ItemStack active = mc.player.getActiveItem();
            int useDuration = mc.player.getItemUseTime();

            if (active.getItem() instanceof TridentItem && zov.isSelected("Trident")) {
                if (useDuration >= 10) {
                    mc.interactionManager.stopUsingItem(mc.player);
                }
                return;
            }

            if (active.getItem() instanceof CrossbowItem && zov.isSelected("Crossbow")) {
                int pullTime = CrossbowItem.getPullTime(active, mc.player);
                int quickChargeLevel = (int) getEnchantLevel(active);
                int adjusted = Math.max(1, pullTime - quickChargeLevel * 5);
                if (useDuration >= adjusted) {
                    mc.interactionManager.stopUsingItem(mc.player);
                }
                return;
            }

            if (active.getItem() instanceof BowItem && zov.isSelected("Bow")) {
                if (useDuration >= 20) {
                    mc.interactionManager.stopUsingItem(mc.player);
                }
            }
        }
    }