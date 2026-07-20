package net.neoforged.neoforge.server;
import net.minecraft.server.MinecraftServer;
public final class ServerLifecycleHooks {
    private static volatile MinecraftServer current;
    private ServerLifecycleHooks() {}
    public static MinecraftServer getCurrentServer() { return current; }
    public static void setCurrentServer(MinecraftServer server) { current = server; }
}
