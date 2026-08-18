package net.mcreator.scpadditions.event;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.SaveStatePacket;
import net.mcreator.scpadditions.save.SaveMethod;
import net.mcreator.scpadditions.sound.GameplaySounds;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authoritative save feedback for every effective player respawn point.
 *
 * Normal vanilla/modded calls are intercepted immediately by the ServerPlayer
 * mixin. The tick snapshot remains as a compatibility fallback for mods that
 * mutate respawn data without going through setRespawnPosition.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveGameSoundEvents {
    public static final String LAST_SAVE_METHOD_TAG =
            "scp_additions.last_save_method";

    private static final long DUPLICATE_FEEDBACK_WINDOW_MS = 1200L;
    private static final Map<UUID, SpawnSnapshot> LAST_SPAWNS = new HashMap<>();
    private static final Map<UUID, FeedbackStamp> LAST_FEEDBACK = new HashMap<>();

    private SaveGameSoundEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SPAWNS.put(player.getUUID(), snapshot(player));
            LAST_FEEDBACK.remove(player.getUUID());
            syncState(player, methodFor(player), false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SPAWNS.put(player.getUUID(), snapshot(player));
            syncState(player, methodFor(player), false);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        String method = event.getOriginal().getPersistentData()
                .getString(LAST_SAVE_METHOD_TAG);
        if (!method.isBlank()) {
            event.getEntity().getPersistentData()
                    .putString(LAST_SAVE_METHOD_TAG, method);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_SPAWNS.remove(id);
        LAST_FEEDBACK.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;

        SpawnSnapshot current = snapshot(player);
        SpawnSnapshot previous = LAST_SPAWNS.put(player.getUUID(), current);
        if (previous == null || previous.equals(current)
                || current.position() == null) return;

        // Fallback for compatibility mods that alter the effective respawn data
        // without calling ServerPlayer#setRespawnPosition.
        recordRespawnPointSet(player, SaveMethod.RESPAWN_POINT);
    }

    /** Called by the mapping-safe ServerPlayer mixin for every explicit save. */
    public static void recordRespawnPointSet(ServerPlayer player,
            SaveMethod method) {
        if (player == null || player.getRespawnPosition() == null) return;

        SaveMethod resolved = method == null
                ? SaveMethod.RESPAWN_POINT : method;
        SpawnSnapshot current = snapshot(player);
        long now = Util.getMillis();
        UUID playerId = player.getUUID();
        FeedbackStamp previousFeedback = LAST_FEEDBACK.get(playerId);

        boolean sameEffectiveSave = previousFeedback != null
                && previousFeedback.snapshot().equals(current)
                && now - previousFeedback.atMillis()
                < DUPLICATE_FEEDBACK_WINDOW_MS
                && (resolved == previousFeedback.method()
                || resolved == SaveMethod.RESPAWN_POINT);

        // A generic compatibility/fallback write immediately following a named
        // save must not erase the more useful Quicksave/Checkpoint identity.
        SaveMethod storedMethod = sameEffectiveSave
                && resolved == SaveMethod.RESPAWN_POINT
                ? previousFeedback.method() : resolved;
        player.getPersistentData().putString(LAST_SAVE_METHOD_TAG,
                storedMethod.id());
        LAST_SPAWNS.put(playerId, current);

        if (sameEffectiveSave) return;
        LAST_FEEDBACK.put(playerId,
                new FeedbackStamp(current, storedMethod, now));

        // Some compatibility mods set respawn data while constructing/logging
        // in a ServerPlayer. Persist the method immediately, but defer visible
        // feedback until a real network connection exists.
        if (player.connection == null) return;
        player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        syncState(player, storedMethod, true);
    }

    public static SaveMethod methodFor(ServerPlayer player) {
        if (player == null) return SaveMethod.WORLD_SPAWN;
        String stored = player.getPersistentData()
                .getString(LAST_SAVE_METHOD_TAG);
        if (!stored.isBlank()) return SaveMethod.fromId(stored);
        return player.getRespawnPosition() == null
                ? SaveMethod.WORLD_SPAWN : SaveMethod.RESPAWN_POINT;
    }

    private static void syncState(ServerPlayer player, SaveMethod method,
            boolean showOverlay) {
        if (player == null || player.connection == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SaveStatePacket(method.id(), showOverlay));
    }

    private static SpawnSnapshot snapshot(ServerPlayer player) {
        BlockPos position = player.getRespawnPosition();
        return new SpawnSnapshot(player.getRespawnDimension(),
                position == null ? null : position.immutable(),
                player.isRespawnForced());
    }

    private record SpawnSnapshot(ResourceKey<Level> dimension,
            @Nullable BlockPos position, boolean forced) {
    }

    private record FeedbackStamp(SpawnSnapshot snapshot, SaveMethod method,
            long atMillis) {
    }
}
