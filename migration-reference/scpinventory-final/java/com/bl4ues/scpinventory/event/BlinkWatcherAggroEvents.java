package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.BlinkWatcherEntity;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import com.bl4ues.scpinventory.network.BlinkStatePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.ScareSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class BlinkWatcherAggroEvents {
    private static final Map<UUID, Boolean> LAST_ACTIVE = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_CLOSE_REVEAL = new HashMap<>();
    private static final Map<UUID, Integer> LAST_REVEAL_SOUND_TICK = new HashMap<>();
    private static final Map<UUID, Integer> LAST_THREAT_TICK = new HashMap<>();
    private static final Map<ObservationKey, Integer> LAST_OBSERVED_TICK = new HashMap<>();
    private static final Map<ObservationKey, Vec3> LAST_OBSERVED_POSITION = new HashMap<>();
    private static final Map<ObservationKey, Integer> LAST_CONFIRMED_OBSERVED_TICK = new HashMap<>();
    private static final Map<ObservationKey, Vec3> LAST_CONFIRMED_OBSERVED_POSITION = new HashMap<>();
    private static final Map<ObservationKey, Integer> LAST_CLOSE_OBSERVED_TICK = new HashMap<>();
    private static final Map<ObservationKey, Vec3> LAST_CLOSE_OBSERVED_POSITION = new HashMap<>();
    private static final double THREAT_MEMORY_DISTANCE = 48.0D;
    private static final double THREAT_MEMORY_DISTANCE_SQR = THREAT_MEMORY_DISTANCE * THREAT_MEMORY_DISTANCE;
    private static final double VISUAL_CONFIRM_DISTANCE = 20.0D;
    private static final double VISUAL_CONFIRM_DISTANCE_SQR = VISUAL_CONFIRM_DISTANCE * VISUAL_CONFIRM_DISTANCE;
    private static final double CLOSE_REVEAL_DISTANCE = 5.0D;
    private static final double CLOSE_REVEAL_DISTANCE_SQR = CLOSE_REVEAL_DISTANCE * CLOSE_REVEAL_DISTANCE;
    private static final double REVEAL_TRAVEL_DISTANCE = 2.25D;
    private static final double REVEAL_TRAVEL_DISTANCE_SQR = REVEAL_TRAVEL_DISTANCE * REVEAL_TRAVEL_DISTANCE;
    private static final float REPEAT_REVEAL_SOUND_CHANCE = 0.35F;
    private static final int REVEAL_UNOBSERVED_TICKS = 40;
    private static final int REVEAL_SOUND_COOLDOWN_TICKS = 160;
    private static final int HUD_DISENGAGE_DELAY_TICKS = 200;

    private BlinkWatcherAggroEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        ThreatState threat = findThreatState(player);
        boolean rawActive = threat.active();
        boolean closeReveal = threat.closeObservedReveal();
        UUID id = player.getUUID();
        boolean previousActive = LAST_ACTIVE.getOrDefault(id, false);
        boolean previousCloseReveal = LAST_CLOSE_REVEAL.getOrDefault(id, false);

        if (rawActive) {
            LAST_THREAT_TICK.put(id, player.tickCount);
        }
        boolean active = rawActive || (threat.hasWatcher() && shouldKeepHudForParanoia(player, id));
        if (!threat.hasWatcher() && !active) {
            LAST_THREAT_TICK.remove(id);
            LAST_CLOSE_REVEAL.remove(id);
        }

        if (closeReveal && !previousCloseReveal && !maybeSendRevealSound(player, id, threat.forceRevealSound())) {
            closeReveal = false;
        }

        LAST_CLOSE_REVEAL.put(id, closeReveal);
        if (active != previousActive || player.tickCount % 40 == 0) {
            LAST_ACTIVE.put(id, active);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlinkStatePacket(active));
        }
    }

    private static ThreatState findThreatState(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return ThreatState.INACTIVE;
        }

        AABB area = player.getBoundingBox().inflate(THREAT_MEMORY_DISTANCE);
        boolean active = false;
        boolean closeObservedReveal = false;
        boolean forceRevealSound = false;
        boolean hasWatcher = false;

        for (BlinkWatcherEntity watcher : player.serverLevel().getEntitiesOfClass(BlinkWatcherEntity.class, area,
                watcher -> watcher.isAlive() && shouldTrackWatcher(player, watcher))) {
            hasWatcher = true;
            if (shouldShowBlinkHud(player, watcher)) {
                active = true;
            }
            if (watcher instanceof Scp173Entity scp173) {
                RevealState reveal = revealState(player, scp173);
                if (reveal.shouldPlay()) {
                    closeObservedReveal = true;
                    forceRevealSound |= reveal.forceSound();
                }
                updateObservedMemory(player, scp173);
                updateConfirmedObservedMemory(player, scp173);
                updateCloseObservedMemory(player, scp173);
            }
        }

        return new ThreatState(active, closeObservedReveal, hasWatcher, forceRevealSound);
    }

    private static boolean shouldTrackWatcher(ServerPlayer player, BlinkWatcherEntity watcher) {
        if (watcher.getTarget() == player) {
            return true;
        }
        if (watcher instanceof Scp173Entity scp173) {
            return scp173.isObservedBy(player) || wasObservedBefore(player, scp173)
                    && scp173.distanceToSqr(player) <= THREAT_MEMORY_DISTANCE_SQR;
        }
        return false;
    }

    private static boolean shouldShowBlinkHud(ServerPlayer player, BlinkWatcherEntity watcher) {
        if (watcher instanceof Scp173Entity scp173) {
            return isConfirmedVisualThreat(player, scp173);
        }
        return watcher.getTarget() == player;
    }

    private static RevealState revealState(ServerPlayer player, Scp173Entity scp173) {
        if (!isConfirmedVisualThreat(player, scp173)) {
            return RevealState.NONE;
        }

        ObservationKey key = observationKey(player, scp173);
        double distanceSqr = scp173.distanceToSqr(player);
        boolean firstConfirmedObservation = !LAST_CONFIRMED_OBSERVED_TICK.containsKey(key);
        if (firstConfirmedObservation) {
            return new RevealState(true, true);
        }

        int lastObservedTick = LAST_OBSERVED_TICK.getOrDefault(key, player.tickCount);
        int lastCloseObservedTick = LAST_CLOSE_OBSERVED_TICK.getOrDefault(key, -REVEAL_UNOBSERVED_TICKS);
        Vec3 lastConfirmedPosition = LAST_CONFIRMED_OBSERVED_POSITION.getOrDefault(key, scp173.position());
        Vec3 lastClosePosition = LAST_CLOSE_OBSERVED_POSITION.getOrDefault(key, scp173.position());

        boolean wasUnobservedLongEnough = player.tickCount - lastObservedTick >= REVEAL_UNOBSERVED_TICKS;
        boolean movedSinceConfirmation = scp173.position().distanceToSqr(lastConfirmedPosition) >= REVEAL_TRAVEL_DISTANCE_SQR;
        boolean suddenCloseApproach = distanceSqr <= CLOSE_REVEAL_DISTANCE_SQR
                && (player.tickCount - lastCloseObservedTick >= REVEAL_UNOBSERVED_TICKS
                || scp173.position().distanceToSqr(lastClosePosition) >= REVEAL_TRAVEL_DISTANCE_SQR);
        boolean repeatRevealRoll = player.getRandom().nextFloat() < REPEAT_REVEAL_SOUND_CHANCE;

        return new RevealState((wasUnobservedLongEnough || suddenCloseApproach) && movedSinceConfirmation && repeatRevealRoll, false);
    }

    private static boolean isConfirmedVisualThreat(ServerPlayer player, Scp173Entity scp173) {
        return scp173.isActivated()
                && scp173.isObservedBy(player)
                && scp173.distanceToSqr(player) <= VISUAL_CONFIRM_DISTANCE_SQR;
    }

    private static void updateObservedMemory(ServerPlayer player, Scp173Entity scp173) {
        if (scp173.isObservedBy(player)) {
            ObservationKey key = observationKey(player, scp173);
            LAST_OBSERVED_TICK.put(key, player.tickCount);
            LAST_OBSERVED_POSITION.put(key, scp173.position());
        }
    }

    private static void updateConfirmedObservedMemory(ServerPlayer player, Scp173Entity scp173) {
        if (isConfirmedVisualThreat(player, scp173)) {
            ObservationKey key = observationKey(player, scp173);
            LAST_CONFIRMED_OBSERVED_TICK.put(key, player.tickCount);
            LAST_CONFIRMED_OBSERVED_POSITION.put(key, scp173.position());
        }
    }

    private static void updateCloseObservedMemory(ServerPlayer player, Scp173Entity scp173) {
        if (isConfirmedVisualThreat(player, scp173) && scp173.distanceToSqr(player) <= CLOSE_REVEAL_DISTANCE_SQR) {
            ObservationKey key = observationKey(player, scp173);
            LAST_CLOSE_OBSERVED_TICK.put(key, player.tickCount);
            LAST_CLOSE_OBSERVED_POSITION.put(key, scp173.position());
        }
    }

    private static boolean wasObservedBefore(ServerPlayer player, Scp173Entity scp173) {
        return LAST_OBSERVED_TICK.containsKey(observationKey(player, scp173));
    }

    private static ObservationKey observationKey(ServerPlayer player, Scp173Entity scp173) {
        return new ObservationKey(player.getUUID(), scp173.getUUID());
    }

    private static boolean shouldKeepHudForParanoia(ServerPlayer player, UUID id) {
        int lastThreatTick = LAST_THREAT_TICK.getOrDefault(id, -HUD_DISENGAGE_DELAY_TICKS);
        return player.tickCount - lastThreatTick <= HUD_DISENGAGE_DELAY_TICKS;
    }

    private static boolean maybeSendRevealSound(ServerPlayer player, UUID id, boolean ignoreCooldown) {
        int lastTick = LAST_REVEAL_SOUND_TICK.getOrDefault(id, -REVEAL_SOUND_COOLDOWN_TICKS);
        if (!ignoreCooldown && player.tickCount - lastTick < REVEAL_SOUND_COOLDOWN_TICKS) {
            return false;
        }
        LAST_REVEAL_SOUND_TICK.put(id, player.tickCount);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScareSoundPacket());
        return true;
    }

    private record ObservationKey(UUID playerId, UUID scpId) {
    }

    private record RevealState(boolean shouldPlay, boolean forceSound) {
        private static final RevealState NONE = new RevealState(false, false);
    }

    private record ThreatState(boolean active, boolean closeObservedReveal, boolean hasWatcher, boolean forceRevealSound) {
        private static final ThreatState INACTIVE = new ThreatState(false, false, false, false);
    }
}
