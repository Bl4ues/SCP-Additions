package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079CameraTravelRules;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.mixin.client.Scp079CameraEffectsClientInvoker;
import com.bl4ues.scpclassifieddirective.mixin.client.Scp079PlayableClientTravelAccessor;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Holds a camera hand-off on the current feed while the authored interference
 * plays. The server remains authoritative and spends AP immediately; only the
 * local surveillance feed waits for the network-travel interval to complete.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class Scp079CameraTravelDelayClient {
    private static final long SAME_FLOOR_NANOS = 500_000_000L;
    private static final long CROSS_FLOOR_NANOS = 1_000_000_000L;
    private static final long CROSS_ZONE_NANOS = 2_000_000_000L;
    private static final long ARRIVAL_SUPPRESSION_NANOS = 350_000_000L;

    private static Scp079PlayableNetwork.State pendingState;
    private static long arrivalAt;
    private static long suppressAutomaticUntil;
    private static boolean bypass;

    private Scp079CameraTravelDelayClient() { }

    /** @return true when the incoming state must be held instead of applied. */
    public static boolean intercept(Scp079PlayableNetwork.State state) {
        if (bypass || state == null) return false;
        if (!state.active() || state.cameraId() == null
                || !Scp079PlayableClient.cameraMode()) {
            clearPending();
            return false;
        }

        Vec3 current = Scp079PlayableClient.viewPosition();
        Vec3 target = new Vec3(state.cameraX(), state.cameraY(), state.cameraZ());
        if (current.distanceToSqr(target) <= 1.0E-6D) {
            clearPending();
            return false;
        }

        long now = System.nanoTime();
        if (pendingState != null
                && pendingState.cameraId().equals(state.cameraId())) {
            // Periodic authoritative state refreshes must not restart travel.
            pendingState = state;
            return true;
        }

        FacilityRoomSnapshot from = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(), BlockPos.containing(current));
        FacilityRoomSnapshot to = FacilityMappingClientState.roomAt(
                state.dimension(), BlockPos.containing(target));
        int tier = Scp079CameraTravelRules.multiplier(from, to);
        long duration = tier >= 3 ? CROSS_ZONE_NANOS
                : tier == 2 ? CROSS_FLOOR_NANOS : SAME_FLOOR_NANOS;

        pendingState = state;
        arrivalAt = now + duration;
        suppressAutomaticUntil = 0L;
        Scp079CameraEffectsClientInvoker.scpclassifieddirective$startTransition(
                duration);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Scp079FacilityMapScreen) {
            minecraft.setScreen(null);
        }
        return true;
    }

    public static boolean suppressAutomaticTransition() {
        return suppressAutomaticUntil > 0L
                && System.nanoTime() < suppressAutomaticUntil;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingState == null) return;
        if (!Scp079PlayableClient.active()) {
            clearPending();
            return;
        }
        long now = System.nanoTime();
        if (now < arrivalAt) return;

        Scp079PlayableNetwork.State state = pendingState;
        clearPending();
        suppressAutomaticUntil = now + ARRIVAL_SUPPRESSION_NANOS;
        bypass = true;
        try {
            Scp079PlayableClient.receive(state);
            // receive() owns a legacy 260 ms local glitch timer. The authored
            // travel effect has already completed, so do not append another
            // mini-transition after arrival.
            Scp079PlayableClientTravelAccessor
                    .scpclassifieddirective$setInterferenceUntil(now);
        } finally {
            bypass = false;
        }
    }

    private static void clearPending() {
        pendingState = null;
        arrivalAt = 0L;
    }
}
