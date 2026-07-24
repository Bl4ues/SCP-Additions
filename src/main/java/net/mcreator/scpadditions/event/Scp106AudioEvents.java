package net.mcreator.scpadditions.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.init.Scp106Sounds;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Coordinates positional SCP-106 sounds and per-target chase music. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106AudioEvents {
    private static final byte EMERGING_GROUND = 1;
    private static final byte EMERGING_WALL = 2;
    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();

    private Scp106AudioEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof Scp106Entity scp106) {
            TRACKED.put(scp106.getUUID(), new Tracked(scp106));
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof Scp106Entity scp106)) {
            return;
        }
        Tracked tracked = TRACKED.remove(scp106.getUUID());
        if (tracked != null) stopChase(event.getLevel().getServer(), tracked);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        Iterator<Tracked> iterator = TRACKED.values().iterator();
        while (iterator.hasNext()) {
            Tracked tracked = iterator.next();
            Scp106Entity scp106 = tracked.entity;
            if (scp106.isRemoved() || !scp106.isAlive()) {
                stopChase(server, tracked);
                iterator.remove();
                continue;
            }

            byte state = scp106.getEncounterState();
            if (state != tracked.previousState
                    && (state == EMERGING_GROUND
                    || state == EMERGING_WALL)) {
                playPhase(scp106, 1.0F);
            }
            tracked.previousState = state;

            boolean ranged = scp106.isRangedAttacking();
            if (ranged && !tracked.rangedPreviously) {
                playPhase(scp106, 0.38F);
            }
            tracked.rangedPreviously = ranged;

            ServerPlayer target = validTarget(scp106);
            UUID targetId = target == null ? null : target.getUUID();
            if (!java.util.Objects.equals(targetId, tracked.audioTarget)) {
                stopChase(server, tracked);
                if (target != null) {
                    ScpEntityNetwork.setScp106Chase(target, true);
                    tracked.audioTarget = targetId;
                }
            }
        }
    }

    public static void stopChaseFor(Scp106Entity scp106) {
        if (scp106 == null || scp106.level().isClientSide) return;
        Tracked tracked = TRACKED.get(scp106.getUUID());
        MinecraftServer server = scp106.getServer();
        if (tracked != null) stopChase(server, tracked);
    }

    private static ServerPlayer validTarget(Scp106Entity scp106) {
        if (!(scp106.getTarget() instanceof ServerPlayer player)
                || !player.isAlive() || player.isCreative()
                || player.isSpectator() || player.isRemoved()) {
            return null;
        }
        return player;
    }

    private static void stopChase(MinecraftServer server, Tracked tracked) {
        if (tracked.audioTarget == null || server == null) {
            tracked.audioTarget = null;
            return;
        }
        ServerPlayer oldTarget = server.getPlayerList()
                .getPlayer(tracked.audioTarget);
        if (oldTarget != null) {
            ScpEntityNetwork.setScp106Chase(oldTarget, false);
        }
        tracked.audioTarget = null;
    }

    public static void playPhase(Scp106Entity scp106, float volume) {
        if (!(scp106.level() instanceof ServerLevel level)) return;
        level.playSound(null, scp106.getX(), scp106.getY() + 0.8D,
                scp106.getZ(), Scp106Sounds.PHASE.get(),
                SoundSource.HOSTILE, volume,
                0.94F + scp106.getRandom().nextFloat() * 0.12F);
    }

    private static final class Tracked {
        private final Scp106Entity entity;
        private byte previousState = -1;
        private boolean rangedPreviously;
        private UUID audioTarget;

        private Tracked(Scp106Entity entity) {
            this.entity = entity;
        }
    }
}
