package wtf.dettex.event.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import wtf.dettex.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}

