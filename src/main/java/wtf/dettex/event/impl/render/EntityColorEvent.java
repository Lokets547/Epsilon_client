package wtf.dettex.event.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import wtf.dettex.event.events.callables.EventCancellable;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EntityColorEvent extends EventCancellable {
    int color;
    LivingEntity entity;

    public EntityColorEvent(int color, LivingEntity entity) {
        this.color = color;
        this.entity = entity;
    }
}

