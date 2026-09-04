package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableAudioClient;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Listener-only feedback for manual SCP-079 network requests. */
@Mixin(Scp079PlayableNetwork.class)
public abstract class Scp079PlayableNetworkAudioMixin {
    @Inject(method = "requestRoom", at = @At("HEAD"), remap = false)
    private static void scpclassifieddirective$roomSwitch(UUID roomId,
            CallbackInfo ci) {
        if (roomId != null) Scp079PlayableAudioClient.playRoomSwitch();
    }

    @Inject(method = "requestAction", at = @At("HEAD"), remap = false)
    private static void scpclassifieddirective$facilityAction(
            Scp079PlayableManager.ManualAction action, BlockPos aimedPos,
            CallbackInfo ci) {
        if (action == null || aimedPos == null) return;
        if (action == Scp079PlayableManager.ManualAction.LOCK) {
            Scp079PlayableAudioClient.playLockOrTesla();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (nearbyMatches(minecraft, aimedPos, 5,
                TeslaGateStructure::isController)) {
            Scp079PlayableAudioClient.playLockOrTesla();
        } else {
            Scp079PlayableAudioClient.playDoorToggle();
        }
    }

    private static boolean nearbyMatches(Minecraft minecraft, BlockPos origin,
            int radius, java.util.function.Predicate<BlockState> predicate) {
        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (predicate.test(minecraft.level.getBlockState(cursor))) return true;
        }
        return false;
    }
}
