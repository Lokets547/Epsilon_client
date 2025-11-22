package wtf.dettex.event.impl.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import wtf.dettex.event.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}

