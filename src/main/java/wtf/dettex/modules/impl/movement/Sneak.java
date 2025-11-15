package wtf.dettex.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.PlayerInput;
import wtf.dettex.event.EventHandler;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.other.Instance;
import wtf.dettex.event.impl.player.InputEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Sneak extends Module {

    public static Sneak getInstance() {
        return Instance.get(Sneak.class);
    }

    public Sneak() {
        super("Sneak", "Sneak", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.sneakKey.isPressed()) {
            PlayerInput in = event.getInput();
            event.setInput(new PlayerInput(in.forward(), in.backward(), in.left(), in.right(), in.jump(), false, in.sprint()));
        }
    }
}
