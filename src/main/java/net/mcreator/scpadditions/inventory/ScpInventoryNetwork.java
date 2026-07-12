package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpInventoryNetwork {
    private static boolean registered;

    private ScpInventoryNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpAdditionsMod.addNetworkMessage(ScpInventorySyncPacket.class,
                ScpInventorySyncPacket::encode,
                ScpInventorySyncPacket::decode,
                ScpInventorySyncPacket::handle);
        ScpAdditionsMod.addNetworkMessage(ScpInventoryRequestSyncPacket.class,
                ScpInventoryRequestSyncPacket::encode,
                ScpInventoryRequestSyncPacket::decode,
                ScpInventoryRequestSyncPacket::handle);
        ScpAdditionsMod.addNetworkMessage(ScpInventoryFullPacket.class,
                ScpInventoryFullPacket::encode,
                ScpInventoryFullPacket::decode,
                ScpInventoryFullPacket::handle);
    }

    public static void sync(ServerPlayer player) {
        if (player == null) return;
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
                ScpAdditionsMod.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new ScpInventorySyncPacket(inventory.serializeNBT())));
    }

    public static void notifyFull(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator()) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ScpInventoryFullPacket());
    }
}
