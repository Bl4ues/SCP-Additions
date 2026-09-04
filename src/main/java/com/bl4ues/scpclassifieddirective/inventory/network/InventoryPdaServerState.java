package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Authoritative, transient presentation state used only by nearby clients. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class InventoryPdaServerState {
    private static final Set<UUID> OPEN_PLAYERS =
            ConcurrentHashMap.newKeySet();

    private InventoryPdaServerState() {
    }

    public static void setOpen(ServerPlayer player, boolean open) {
        if (player == null) return;
        if (open) OPEN_PLAYERS.add(player.getUUID());
        else OPEN_PLAYERS.remove(player.getUUID());

        InventoryPdaStatePacket update = new InventoryPdaStatePacket(
                player.getUUID(), open);
        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                update);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer observer)
                || !(event.getTarget() instanceof ServerPlayer target)
                || !OPEN_PLAYERS.contains(target.getUUID())) return;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer),
                new InventoryPdaStatePacket(target.getUUID(), true));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OPEN_PLAYERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        OPEN_PLAYERS.clear();
    }
}
