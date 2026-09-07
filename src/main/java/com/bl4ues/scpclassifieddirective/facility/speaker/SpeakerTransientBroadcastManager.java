package com.bl4ues.scpclassifieddirective.facility.speaker;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived physical Speaker activation used by generated announcements.
 *
 * A lease is intentionally separate from the manual SCP-079 microphone
 * broadcast. This means a generated line can switch a room Speaker on, while a
 * player pressing the normal Speaker key during the sentence simply promotes
 * the endpoint to a normal broadcast and prevents the lease from switching it
 * back off afterwards.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpeakerTransientBroadcastManager {
    private static final float SWITCH_CUE_VOLUME = 1.75F;
    private static final Map<UUID, LeaseState> ACTIVE =
            new ConcurrentHashMap<>();

    private SpeakerTransientBroadcastManager() {
    }

    public static Lease begin(MinecraftServer server,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
        if (server == null || endpoints == null || endpoints.isEmpty()) {
            return null;
        }
        List<FacilitySpeakerRegistry.SpeakerEndpoint> valid = endpoints.stream()
                .filter(endpoint -> endpoint != null)
                .filter(endpoint -> {
                    ServerLevel level = server.getLevel(endpoint.dimension());
                    return level != null && level.getBlockState(endpoint.pos())
                            .is(SpeakerModule.BLOCK.get());
                }).distinct().toList();
        if (valid.isEmpty()) return null;

        UUID token = UUID.randomUUID();
        LeaseState state = new LeaseState(server, List.copyOf(valid));
        ACTIVE.put(token, state);
        valid.forEach(endpoint -> setEndpoint(server, endpoint, true));
        return new Lease(token, List.copyOf(valid));
    }

    public static void end(MinecraftServer server, Lease lease) {
        if (server == null || lease == null) return;
        LeaseState state = ACTIVE.get(lease.token());
        if (state == null || state.server != server
                || !ACTIVE.remove(lease.token(), state)) return;
        for (FacilitySpeakerRegistry.SpeakerEndpoint endpoint : state.endpoints) {
            if (!SpeakerBroadcastManager.isEndpointActive(
                    server.getLevel(endpoint.dimension()), endpoint.pos())
                    && !usedByOtherLease(server, endpoint)) {
                setEndpoint(server, endpoint, false);
            }
        }
    }

    private static boolean usedByOtherLease(MinecraftServer server,
            FacilitySpeakerRegistry.SpeakerEndpoint endpoint) {
        for (LeaseState state : ACTIVE.values()) {
            if (state.server == server && state.endpoints.contains(endpoint)) {
                return true;
            }
        }
        return false;
    }

    private static void setEndpoint(MinecraftServer server,
            FacilitySpeakerRegistry.SpeakerEndpoint endpoint, boolean active) {
        ServerLevel level = server.getLevel(endpoint.dimension());
        if (level == null) return;
        BlockState state = level.getBlockState(endpoint.pos());
        if (!state.is(SpeakerModule.BLOCK.get())
                || state.getValue(SpeakerModule.ACTIVE) == active) return;
        level.setBlock(endpoint.pos(), state.setValue(
                SpeakerModule.ACTIVE, active), Block.UPDATE_CLIENTS);
        level.playSound(null, endpoint.pos(),
                active ? SpeakerModule.ON.get() : SpeakerModule.OFF.get(),
                SoundSource.BLOCKS, SWITCH_CUE_VOLUME, 1.0F);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().server
                == event.getServer());
    }

    public record Lease(UUID token,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
    }

    private record LeaseState(MinecraftServer server,
            List<FacilitySpeakerRegistry.SpeakerEndpoint> endpoints) {
    }
}
