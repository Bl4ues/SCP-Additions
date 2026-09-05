package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.HeavyDoorControlPanelAccess;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessSavedData;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.network.Scp079ActionAudioNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Set;

/** Sends SCP-079 device feedback only after the authoritative action succeeds. */
@Mixin(Scp079PlayableManager.class)
public abstract class Scp079PlayableActionAudioMixin {
    @Inject(method = "performAction", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$confirmedAction(
            ServerPlayer player, Scp079PlayableManager.ManualAction action,
            BlockPos aimedPos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || player == null || action == null
                || aimedPos == null || player.getServer() == null) {
            return;
        }

        if (action == Scp079PlayableManager.ManualAction.LOCK) {
            Scp079ActionAudioNetwork.send(player,
                    Scp079ActionAudioNetwork.Cue.LOCK_OR_TESLA);
            return;
        }

        // performAction resolves a controllable door before considering a Tesla.
        // Mirror that order so the success cue describes the action the server
        // actually performed rather than whichever device happens to be nearer
        // on the client's slightly older frame.
        ServerLevel level = player.serverLevel();
        Scp079FacilityAccessSavedData data =
                Scp079FacilityAccessSavedData.get(player.getServer());
        BlockPos door = nearestTracked(level, data.doors(), aimedPos, 3.5D);
        if (door != null && HeavyDoorControlPanelAccess
                .hasControllableInterface(level, door)) {
            Scp079ActionAudioNetwork.send(player,
                    Scp079ActionAudioNetwork.Cue.DOOR_TOGGLE);
            return;
        }

        BlockPos tesla = nearestTracked(level, data.teslaGates(), aimedPos, 5.0D);
        if (tesla != null) {
            Scp079ActionAudioNetwork.send(player,
                    Scp079ActionAudioNetwork.Cue.LOCK_OR_TESLA);
        }
    }

    private static BlockPos nearestTracked(ServerLevel level,
            Set<Scp079FacilityAccessSavedData.TrackedPosition> tracked,
            BlockPos aimedPos, double radius) {
        String dimension = level.dimension().location().toString();
        double radiusSqr = radius * radius;
        return tracked.stream()
                .filter(position -> position.dimension().equals(dimension))
                .map(position -> BlockPos.of(position.packedPos()))
                .filter(pos -> pos.distSqr(aimedPos) <= radiusSqr)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(aimedPos)))
                .orElse(null);
    }
}
