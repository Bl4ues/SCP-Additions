package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public class ClientPlayerNetworkEvent extends Event {
    public static final class LoggingIn extends ClientPlayerNetworkEvent {}
    public static final class LoggingOut extends ClientPlayerNetworkEvent {}
}
