package net.mcreator.scpadditions.fabric;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
public final class FabricServerContext {
    private static volatile MinecraftServer server;
    private FabricServerContext() {}
    public static void set(MinecraftServer value) { server=value; net.neoforged.neoforge.server.ServerLifecycleHooks.setCurrentServer(value); }
    public static Collection<ServerPlayer> players() { return server == null ? List.of() : server.getPlayerList().getPlayers(); }
}
