package net.mcreator.scpadditions.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Synchronizes chase music only to the player currently hunted. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106ChaseAudioEvents {
    private static final Map<Scp106Entity, UUID> AUDIBLE_TARGETS =
            new WeakHashMap<>();

    private Scp106ChaseAudioEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Scp106Entity scp106)
                || scp106.level().isClientSide) {
            return;
        }

        UUID next = scp106.shouldPlayChaseMusic()
                ? scp106.getHuntedPlayerId() : null;
        UUID previous = AUDIBLE_TARGETS.get(scp106);
        if (java.util.Objects.equals(previous, next)) return;

        MinecraftServer server = scp106.getServer();
        if (server == null) return;
        if (previous != null) send(server, previous,
                scp106.getUUID(), false);
        if (next != null) {
            AUDIBLE_TARGETS.put(scp106, next);
            send(server, next, scp106.getUUID(), true);
        } else {
            AUDIBLE_TARGETS.remove(scp106);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Scp106Entity scp106)
                || event.getLevel().isClientSide) {
            return;
        }
        UUID previous = AUDIBLE_TARGETS.remove(scp106);
        MinecraftServer server = event.getLevel().getServer();
        if (previous != null && server != null) {
            send(server, previous, scp106.getUUID(), false);
        }
    }

    private static void send(MinecraftServer server, UUID playerId,
            UUID sourceId, boolean active) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            ScpEntityNetwork.setScp106ChaseMusic(
                    player, sourceId, active);
        }
    }
}
