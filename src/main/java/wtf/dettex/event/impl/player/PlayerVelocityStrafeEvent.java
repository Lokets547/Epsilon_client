package wtf.dettex.event.impl.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;
import wtf.dettex.event.events.Event;
@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerVelocityStrafeEvent implements Event {
    final Vec3d movementInput;
    final float speed;
    final float yaw;
    Vec3d velocity;
}
