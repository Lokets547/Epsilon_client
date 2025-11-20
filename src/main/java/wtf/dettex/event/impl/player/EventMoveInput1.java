package wtf.dettex.event.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.PlayerInput;
import wtf.dettex.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class EventMoveInput1  extends EventCancellable {

        private PlayerInput input;
        private float forward, strafe;
    }



