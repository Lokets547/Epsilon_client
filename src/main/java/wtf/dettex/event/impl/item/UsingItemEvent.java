package wtf.dettex.event.impl.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wtf.dettex.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class UsingItemEvent extends EventCancellable {
    byte type;
}
