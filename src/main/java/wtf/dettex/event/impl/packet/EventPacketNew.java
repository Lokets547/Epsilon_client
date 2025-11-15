package wtf.dettex.event.impl.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.packet.Packet;
import wtf.dettex.event.events.callables.EventCancellable;

@AllArgsConstructor @Getter
public class EventPacketNew extends EventCancellable {
    public final Packet<?> packet;

    public static class Receive extends EventPacketNew {
        public Receive(Packet<?> packet) {
            super(packet);
        }
    }

    public static class Send extends EventPacketNew {
        public Send(Packet<?> packet) {
            super(packet);
        }
    }

    public static class All extends EventPacketNew {
        public All(Packet<?> packet) {
            super(packet);
        }
    }
}
