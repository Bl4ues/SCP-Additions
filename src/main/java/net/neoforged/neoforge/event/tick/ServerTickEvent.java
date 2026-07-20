package net.neoforged.neoforge.event.tick;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
public abstract class ServerTickEvent extends Event {
    private final MinecraftServer server;
    protected ServerTickEvent(MinecraftServer server) { this.server = server; }
    public MinecraftServer getServer() { return server; }
    public static final class Pre extends ServerTickEvent { public Pre(MinecraftServer server) { super(server); } }
    public static final class Post extends ServerTickEvent { public Post(MinecraftServer server) { super(server); } }
}
