package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public class InputEvent extends Event {
    public static final class Key extends InputEvent {}
    public static final class InteractionKeyMappingTriggered extends InputEvent {
        private final boolean useItem;
        public InteractionKeyMappingTriggered(boolean useItem) { this.useItem = useItem; }
        public boolean isUseItem() { return useItem; }
    }
    public static final class MouseScrollingEvent extends InputEvent {
        private final double scrollDeltaX;
        private final double scrollDeltaY;
        public MouseScrollingEvent(double x, double y) { scrollDeltaX=x; scrollDeltaY=y; }
        public double getScrollDeltaX() { return scrollDeltaX; }
        public double getScrollDeltaY() { return scrollDeltaY; }
    }
}
