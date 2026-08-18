package net.mcreator.scpadditions.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.event.SaveGameSoundEvents;
import net.mcreator.scpadditions.network.MineZeroDeathStatePacket;
import net.mcreator.scpadditions.network.MineZeroRestoreTransitionPacket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Cooperative logical-death and rollback voting for MineZero compatibility. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MineZeroDeathCoordinator {
    private static final Set<UUID> DEAD = new HashSet<>();
    private static final Set<UUID> VOTES = new HashSet<>();
    private static final Map<UUID, String> CAUSES = new HashMap<>();
    private static boolean restorePending;

    private MineZeroDeathCoordinator() {
    }

    public static boolean sessionActive() {
        return restorePending || !DEAD.isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!MineZeroCompatibility.enabled()
                || MineZeroCompatibility.restoring()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !MineZeroCompatibility.hasCheckpoint(player)) {
            return;
        }

        event.setCanceled(true);
        UUID id = player.getUUID();
        if (!DEAD.add(id)) return;

        String cause = event.getSource().getLocalizedDeathMessage(player)
                .getString();
        CAUSES.put(id, cause == null || cause.isBlank()
                ? player.getName().getString() + " died." : cause);
        VOTES.remove(id);

        player.setHealth(1.0F);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.setInvulnerable(true);
        player.stopRiding();
        player.setGameMode(GameType.SPECTATOR);

        broadcast(player.server, id);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        if (!DEAD.remove(id)) return;
        VOTES.remove(id);
        CAUSES.remove(id);
        if (event.getEntity() instanceof ServerPlayer player) {
            broadcast(player.server, null);
        }
    }

    public static boolean isLogicallyDead(ServerPlayer player) {
        return player != null && DEAD.contains(player.getUUID());
    }

    public static void voteToRestore(ServerPlayer player) {
        if (!MineZeroCompatibility.enabled() || player == null
                || restorePending || !DEAD.contains(player.getUUID())) return;

        MinecraftServer server = player.server;
        SessionCounts counts = counts(server);
        if (counts.living() > 0 || counts.dead() == 0) return;

        VOTES.add(player.getUUID());
        counts = counts(server);
        broadcast(server, null);
        if (VOTES.size() < counts.requiredVotes()) return;

        restorePending = true;
        for (ServerPlayer dead : server.getPlayerList().getPlayers()) {
            if (DEAD.contains(dead.getUUID())) {
                ScpAdditionsMod.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> dead),
                        new MineZeroRestoreTransitionPacket(true));
            }
        }
        ScpAdditionsMod.queueServerWork(12, () -> restore(server));
    }

    private static void restore(MinecraftServer server) {
        ServerPlayer anchor = server.getPlayerList().getPlayers().stream()
                .findFirst().orElse(null);
        if (anchor == null) {
            restorePending = false;
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SaveGameSoundEvents.silenceFeedback(player, 6500L);
        }

        boolean restored = MineZeroCompatibility.restoreCheckpoint(anchor);
        if (!restored) {
            restorePending = false;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (DEAD.contains(player.getUUID())) {
                    player.sendSystemMessage(Component.literal(
                            "MineZero checkpoint restore failed; the death session was kept open."));
                }
            }
            broadcast(server, null);
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.setInvulnerable(false);
            ScpAdditionsMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new MineZeroRestoreTransitionPacket(false));
        }
        DEAD.clear();
        VOTES.clear();
        CAUSES.clear();
        restorePending = false;
    }

    private static void broadcast(MinecraftServer server, UUID newlyDead) {
        if (server == null) return;
        SessionCounts counts = counts(server);
        VOTES.removeIf(id -> !DEAD.contains(id));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!DEAD.contains(player.getUUID())) continue;
            boolean open = player.getUUID().equals(newlyDead);
            String cause = CAUSES.getOrDefault(player.getUUID(), "");
            ScpAdditionsMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new MineZeroDeathStatePacket(open, cause,
                            counts.living(), counts.dead(), VOTES.size(),
                            counts.requiredVotes()));
        }
    }

    private static SessionCounts counts(MinecraftServer server) {
        int dead = 0;
        int living = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (DEAD.contains(player.getUUID())) dead++;
            else if (!player.isSpectator()) living++;
        }
        int required = living == 0 ? dead : 0;
        return new SessionCounts(living, dead, required);
    }

    private record SessionCounts(int living, int dead, int requiredVotes) {
    }
}
