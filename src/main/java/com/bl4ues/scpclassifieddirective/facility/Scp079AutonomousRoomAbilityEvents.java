package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

/** Tactical use of mapped-room Blackout and Lockdown by autonomous SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079AutonomousRoomAbilityEvents {
    private static final int EVALUATION_INTERVAL_TICKS = 40;
    private static final float UNPROVOKED_BLACKOUT_CHANCE = 0.035F;
    private static final float UNPROVOKED_MIN_POWER = 78.0F;
    private static final double THREAT_RADIUS = 24.0D;
    private static final Map<MinecraftServer, Integer> NEXT_EVALUATION =
            new WeakHashMap<>();

    private Scp079AutonomousRoomAbilityEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (Scp079PlayableManager.hasController(server)) return;
        int now = server.getTickCount();
        synchronized (NEXT_EVALUATION) {
            int next = NEXT_EVALUATION.getOrDefault(server, 0);
            if (now < next) return;
            NEXT_EVALUATION.put(server, now + EVALUATION_INTERVAL_TICKS);
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (!Scp079ProcessingManager.isActive(level)
                    || !Scp079FacilityAccessManager.hasFacilityAccess(level)) {
                continue;
            }
            if (evaluateLevel(level)) return;
        }
    }

    private static boolean evaluateLevel(ServerLevel level) {
        return level.players().stream()
                .filter(player -> player.isAlive()
                        && !player.isCreative() && !player.isSpectator())
                .map(player -> new Candidate(player, roomAt(level, player),
                        threatScore(level, player)))
                .filter(candidate -> candidate.room != null)
                .sorted(Comparator.comparingDouble(Candidate::threatScore)
                        .reversed())
                .anyMatch(candidate -> tryAbility(level, candidate));
    }

    private static boolean tryAbility(ServerLevel level, Candidate candidate) {
        float power = Scp079ProcessingManager.getPower(level);
        if (candidate.threatScore > 0.0D
                && power + 0.001F >= Scp079RoomAbilityManager.LOCKDOWN_COST
                && Scp079RoomAbilityManager.useAutonomous(level, candidate.room,
                Scp079RoomAbilityManager.Ability.LOCKDOWN)) {
            return true;
        }

        if (candidate.threatScore > 0.0D
                && Scp079RoomAbilityManager.useAutonomous(level, candidate.room,
                Scp079RoomAbilityManager.Ability.BLACKOUT)) {
            return true;
        }

        return power >= UNPROVOKED_MIN_POWER
                && level.getRandom().nextFloat() < UNPROVOKED_BLACKOUT_CHANCE
                && Scp079RoomAbilityManager.useAutonomous(level, candidate.room,
                Scp079RoomAbilityManager.Ability.BLACKOUT);
    }

    private static FacilityRoomSnapshot roomAt(ServerLevel level,
            ServerPlayer player) {
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (room.containsColumn(player.blockPosition())) return room;
        }
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (Scp079RoomInteractionPolicy.withinExpandedFloor(room,
                    player.blockPosition(), 1)) return room;
        }
        return null;
    }

    private static double threatScore(ServerLevel level, ServerPlayer player) {
        double best = 0.0D;
        for (Mob mob : level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(THREAT_RADIUS),
                mob -> mob.isAlive() && mob.getTarget() == player)) {
            double distance = Math.sqrt(player.distanceToSqr(mob));
            best = Math.max(best, THREAT_RADIUS - distance);
        }
        for (Scp106Entity scp106 : level.getEntitiesOfClass(Scp106Entity.class,
                player.getBoundingBox().inflate(THREAT_RADIUS + 12.0D),
                entity -> entity.isAlive()
                        && (entity.getTarget() == player
                        || entity.isHuntingPlayer(player)))) {
            double distance = Math.sqrt(player.distanceToSqr(scp106));
            best = Math.max(best, THREAT_RADIUS + 12.0D - distance);
        }
        return best;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (NEXT_EVALUATION) {
            NEXT_EVALUATION.remove(event.getServer());
        }
    }

    private record Candidate(ServerPlayer player, FacilityRoomSnapshot room,
            double threatScore) {
    }
}
