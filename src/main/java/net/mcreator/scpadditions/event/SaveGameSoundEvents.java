package net.mcreator.scpadditions.event;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.mcreator.scpadditions.compat.MineZeroCompatibility;
import net.mcreator.scpadditions.compat.MineZeroSaveSafety;
import net.mcreator.scpadditions.network.SaveStatePacket;
import net.mcreator.scpadditions.save.SaveMethod;
import net.mcreator.scpadditions.sound.GameplaySounds;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Authoritative feedback and last-save identity for effective respawn points. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveGameSoundEvents {
    public static final String LAST_SAVE_METHOD_TAG =
            "scp_additions.last_save_method";

    private static final long DUPLICATE_FEEDBACK_WINDOW_MS = 7000L;
    private static final long RESPAWN_FEEDBACK_SILENCE_MS = 4500L;
    private static final Map<UUID, SpawnSnapshot> LAST_SPAWNS = new HashMap<>();
    private static final Map<UUID, FeedbackStamp> LAST_FEEDBACK = new HashMap<>();
    private static final Map<UUID, Long> FEEDBACK_SILENCED_UNTIL =
            new HashMap<>();

    private SaveGameSoundEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SPAWNS.put(player.getUUID(), snapshot(player));
            LAST_FEEDBACK.remove(player.getUUID());
            FEEDBACK_SILENCED_UNTIL.remove(player.getUUID());
            syncState(player, methodFor(player), false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SPAWNS.put(player.getUUID(), snapshot(player));
            silenceFeedback(player, RESPAWN_FEEDBACK_SILENCE_MS);
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
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            silenceFeedback(player, RESPAWN_FEEDBACK_SILENCE_MS);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_SPAWNS.remove(id);
        LAST_FEEDBACK.remove(id);
        FEEDBACK_SILENCED_UNTIL.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;
        SpawnSnapshot current = snapshot(player);
        SpawnSnapshot previous = LAST_SPAWNS.put(player.getUUID(), current);
        if (previous == null || previous.equals(current)
                || current.position() == null) return;
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

        SaveMethod storedMethod = sameEffectiveSave
                && resolved == SaveMethod.RESPAWN_POINT
                ? previousFeedback.method() : resolved;
        player.getPersistentData().putString(LAST_SAVE_METHOD_TAG,
                storedMethod.id());
        LAST_SPAWNS.put(playerId, current);

        if (sameEffectiveSave) return;
        LAST_FEEDBACK.put(playerId,
                new FeedbackStamp(current, storedMethod, now));

        if (player.connection == null || feedbackSilenced(player, now)
                || MineZeroCompatibility.restoring()) return;

        if (MineZeroCompatibility.enabled()) {
            if (!MineZeroSaveSafety.canSave(player.server)) {
                player.displayClientMessage(
                        Component.literal("You can't save right now"), true);
                return;
            }
            if (!MineZeroCompatibility.createCheckpoint(player)) {
                ScpAdditionsMod.LOGGER.warn(
                        "A respawn point was saved, but the MineZero checkpoint could not be updated for {}",
                        player.getGameProfile().getName());
                return;
            }
            broadcastGlobalMineZeroSave(player, storedMethod, now);
            return;
        }

        player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        syncState(player, storedMethod, true);
    }

    /** MineZero checkpoints are global, so their save identity and feedback are too. */
    private static void broadcastGlobalMineZeroSave(ServerPlayer source,
            SaveMethod method, long now) {
        for (ServerPlayer player : source.server.getPlayerList().getPlayers()) {
            player.getPersistentData().putString(LAST_SAVE_METHOD_TAG, method.id());
            LAST_FEEDBACK.put(player.getUUID(),
                    new FeedbackStamp(snapshot(player), method, now));
            if (player.connection == null || feedbackSilenced(player, now)) continue;
            player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            syncState(player, method, true);
        }
    }

    public static void silenceFeedback(ServerPlayer player, long durationMs) {
        if (player == null) return;
        FEEDBACK_SILENCED_UNTIL.put(player.getUUID(),
                Util.getMillis() + Math.max(0L, durationMs));
    }

    private static boolean feedbackSilenced(ServerPlayer player, long now) {
        Long until = FEEDBACK_SILENCED_UNTIL.get(player.getUUID());
        if (until == null) return false;
        if (now < until) return true;
        FEEDBACK_SILENCED_UNTIL.remove(player.getUUID());
        return false;
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
