package wtf.dettex.event.impl.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import wtf.dettex.event.events.Event;
import wtf.dettex.api.system.draw.DrawEngine;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrawEvent implements Event {
    DrawContext drawContext;
    DrawEngine drawEngine;
    float partialTicks;
}

