package wtf.dettex.event.impl.keyboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wtf.dettex.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class MouseRotationEvent extends EventCancellable {
    float cursorDeltaX, cursorDeltaY;
}
