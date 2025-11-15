package wtf.dettex.event.impl.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import wtf.dettex.event.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JumpEvent extends EventCancellable {
    PlayerEntity player;
}
