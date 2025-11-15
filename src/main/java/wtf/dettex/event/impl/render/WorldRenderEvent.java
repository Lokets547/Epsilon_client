package wtf.dettex.event.impl.render;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import wtf.dettex.event.events.Event;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class WorldRenderEvent implements Event {
    MatrixStack stack;
    float partialTicks;
}
