package wtf.dettex.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Hand;
import net.minecraft.registry.entry.RegistryEntry;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.BooleanSetting;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;
import wtf.dettex.modules.setting.implement.ValueSetting;
import wtf.dettex.common.util.potion.PotionUtils;
import wtf.dettex.common.util.time.TimerUtil;
import wtf.dettex.event.impl.player.TickEvent;

import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoPotion extends Module {

    final MultiSelectSetting potions = new MultiSelectSetting("Бафать", "Какие зелья использовать")
            .value("Силу", "Скорость", "Огнестойкость", "Исцеление");

    final ValueSetting healthThreshold = new ValueSetting("Здоровье", "Порог здоровья для исцеления")
            .setValue(8.0F).range(1.0F, 20.0F).visible(() -> potions.isSelected("Исцеление"));

    final BooleanSetting autoDisable = new BooleanSetting("Авто выключение", "Выключать после использования")
            .setValue(false);

    final BooleanSetting onlyPvP = new BooleanSetting("Только в PVP", "Использовать только в PVP зонах")
            .setValue(false);

    boolean isActive;
    int selectedSlot;
    float previousPitch;
    final TimerUtil time = new TimerUtil();
    final PotionUtils potionUtil = new PotionUtils();
    boolean isActivePotion;

    public AutoPotion() {
        super("AutoPotion", "Auto Potion", ModuleCategory.MISC);
        setup(potions, healthThreshold, onlyPvP, autoDisable);
    }

    @EventHandler

    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem() && !mc.player.isBlocking()) {
            return;
        }

        if (isActive() && shouldUsePotion()) {
            for (PotionType potionType : PotionType.values()) {
                isActivePotion = potionType.isEnabled();
            }
        } else {
            isActivePotion = false;
        }

        if (isActive() && shouldUsePotion()) {
            if (previousPitch == mc.player.getPitch()) {
                int oldItem = mc.player.getInventory().selectedSlot;
                selectedSlot = -1;

                for (PotionType potionType : PotionType.values()) {
                    if (potionType.isEnabled()) {
                        int slot = findPotionSlot(potionType);
                        if (selectedSlot == -1) {
                            selectedSlot = slot;
                        }
                        isActive = true;
                    }
                }

                // ensure we switch back to previous slot after use
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldItem));
            }
        }

        if (time.hasTimeElapsed(500L)) {
            try {
                reset();
                selectedSlot = -2;
            } catch (Exception ex) {
                // Handle exception
            }
        }

        potionUtil.changeItemSlot(selectedSlot == -2);
        if (autoDisable.isValue() && isActive && selectedSlot == -2) {
            deactivate();
            isActive = false;
        }
    }

    // Rotation adjustments removed to match current event API

    private boolean shouldUsePotion() {
        return !onlyPvP.isValue(); // Simplified PvP check
    }

    private void reset() {
        for (PotionType potionType : PotionType.values()) {
            if (potions.isSelected(potionType.getSettingName())) {
                potionType.setEnabled(isPotionActive(potionType));
            }
        }
    }

    private int findPotionSlot(PotionType type) {
        int hbSlot = getPotionIndexHb(type.getPotion());
        if (hbSlot != -1) {
            potionUtil.setPreviousSlot(mc.player.getInventory().selectedSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hbSlot));
            PotionUtils.useItem(Hand.MAIN_HAND);
            type.setEnabled(false);
            time.reset();
            return hbSlot;
        }
        return -1;
    }

    public boolean isActive() {
        for (PotionType potionType : PotionType.values()) {
            if (potions.isSelected(potionType.getSettingName()) && potionType.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private boolean isPotionActive(PotionType type) {
        if (type == PotionType.INSTANT_HEALTH) {
            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() >= healthThreshold.getValue()) {
                isActive = false;
                return false;
            } else {
                return getPotionIndexHb(type.getPotion()) != -1;
            }
        } else {
            if (mc.player.hasStatusEffect(type.getPotion())) {
                isActive = false;
                return false;
            } else {
                return getPotionIndexHb(type.getPotion()) != -1;
            }
        }
    }

    private int getPotionIndexHb(RegistryEntry<StatusEffect> effectType) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
            Iterable<StatusEffectInstance> effects = potionContents != null ? potionContents.getEffects() : java.util.Collections.emptyList();

            for (StatusEffectInstance effect : effects) {
                if (effect.getEffectType().equals(effectType)) {
                    if (stack.getItem() == Items.SPLASH_POTION) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }


    public void deactivate() {
        isActive = false;
        super.deactivate();
    }

    enum PotionType {
        STRENGTH(StatusEffects.STRENGTH, "Силу"),
        SPEED(StatusEffects.SPEED, "Скорость"),
        FIRE_RESIST(StatusEffects.FIRE_RESISTANCE, "Огнестойкость"),
        INSTANT_HEALTH(StatusEffects.INSTANT_HEALTH, "Исцеление");

        private final RegistryEntry<StatusEffect> potion;
        private final String settingName;
        private boolean enabled;

        PotionType(RegistryEntry<StatusEffect> potion, String settingName) {
            this.potion = potion;
            this.settingName = settingName;
        }

        public RegistryEntry<StatusEffect> getPotion() {
            return potion;
        }

        public Supplier<Boolean> isPotionSettingEnabled() {
            return () -> false; // not used
        }

        public String getSettingName() {
            return settingName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

