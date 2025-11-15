package wtf.dettex.modules.impl.render;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import wtf.dettex.api.system.animation.Animation;
import wtf.dettex.api.system.animation.Direction;
import wtf.dettex.api.system.animation.implement.DecelerateAnimation;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.EventHandler;
import wtf.dettex.event.impl.chat.ChatReceiveEvent;
import wtf.dettex.event.impl.container.CloseScreenEvent;
import wtf.dettex.event.impl.container.SetScreenEvent;
import wtf.dettex.event.impl.tab.TabToggleEvent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.modules.setting.implement.MultiSelectSetting;

import java.util.Arrays;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetterMinecraft extends Module {
    MultiSelectSetting animationTargets = new MultiSelectSetting("Animations", "Select UI animations to play")
            .value("Inventory", "Chat", "Tab");

    Animation inventoryAnimation = new DecelerateAnimation().setMs(250).setValue(1);
    Animation chatAnimation = new DecelerateAnimation().setMs(200).setValue(1);
    Animation tabAnimation = new DecelerateAnimation().setMs(200).setValue(1);

    public static BetterMinecraft getInstance() {
        return Instance.get(BetterMinecraft.class);
    }

    public BetterMinecraft() {
        super("BetterMinecraft", "Better Minecraft", ModuleCategory.RENDER);
        animationTargets.setSelected(Arrays.asList("Inventory", "Chat", "Tab"));
        setup(animationTargets);
    }

    @Override
     
    public void deactivate() {
        resetAnimation(inventoryAnimation, false);
        resetAnimation(chatAnimation, false);
        resetAnimation(tabAnimation, false);
        super.deactivate();
    }

    @EventHandler
     
    public void onSetScreen(SetScreenEvent event) {
        if (!animationTargets.isSelected("Inventory")) return;
        if (event.getScreen() != null) {
            trigger(inventoryAnimation, true);
        }
    }

    @EventHandler
     
    public void onCloseScreen(CloseScreenEvent event) {
        if (!animationTargets.isSelected("Inventory")) return;
        trigger(inventoryAnimation, false);
    }

    @EventHandler
     
    public void onChatReceive(ChatReceiveEvent event) {
        if (!animationTargets.isSelected("Chat")) return;
        trigger(chatAnimation, true);
    }

    @EventHandler
     
    public void onTabToggle(TabToggleEvent event) {
        if (!animationTargets.isSelected("Tab")) return;
        trigger(tabAnimation, event.isOpen());
    }

    public float getInventoryAnimationValue() {
        return inventoryAnimation.getOutput().floatValue();
    }

    public float getChatAnimationValue() {
        return chatAnimation.getOutput().floatValue();
    }

    public float getTabAnimationValue() {
        return tabAnimation.getOutput().floatValue();
    }

    private void trigger(Animation animation, boolean forwards) {
        resetAnimation(animation, forwards);
    }

    private void resetAnimation(Animation animation, boolean forwards) {
        animation.reset();
        animation.setDirection(forwards ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    public List<String> getSelectedTargets() {
        return animationTargets.getSelected();
    }
}
