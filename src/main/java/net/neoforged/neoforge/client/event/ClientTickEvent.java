package net.neoforged.neoforge.client.event;
import net.neoforged.bus.api.Event;
public class ClientTickEvent extends Event {
    public static final class Pre extends ClientTickEvent {}
    public static final class Post extends ClientTickEvent {}
}
