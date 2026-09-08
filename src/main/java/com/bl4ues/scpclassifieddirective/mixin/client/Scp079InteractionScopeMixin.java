package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079InteractionScopeClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.network.Scp079AdjacentCameraSwitchNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stabilizes room ownership while the physical camera lens moves and prevents
 * cross-room device prompts from leaking into neighbouring rooms.
 */
@Mixin(value = Scp079PlayableVisualsV2.class, remap = false)
public abstract class Scp079InteractionScopeMixin {
    @Shadow private static List<?> cachedTargets;

    @ModifyArg(method = "refreshPrompts",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079PlayableVisualsV2;refreshTargetCache(Lnet/minecraft/client/Minecraft;Lnet/minecraft/core/BlockPos;)V"),
            index = 1)
    private static BlockPos scpclassifieddirective$useStableCameraRoom(
            BlockPos movingLensBlock) {
        return BlockPos.containing(Scp079PlayableClient.viewPosition());
    }

    @Inject(method = "refreshTargetCache", at = @At("RETURN"))
    private static void scpclassifieddirective$filterInteractionRooms(
            Minecraft minecraft, BlockPos viewpoint, CallbackInfo ci) {
        if (cachedTargets == null || cachedTargets.isEmpty()) return;
        List<Object> filtered = new ArrayList<>(cachedTargets.size());
        for (Object raw : cachedTargets) {
            if (!(raw instanceof Scp079WorldTargetAccessor target)) continue;
            Object kind = target.scpclassifieddirective$kind();
            if (Scp079InteractionScopeClient.allow(String.valueOf(kind),
                    target.scpclassifieddirective$pos())) {
                filtered.add(raw);
            }
        }
        cachedTargets = List.copyOf(filtered);
    }

    @Redirect(method = "handleInteraction",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/network/Scp079PlayableNetwork;requestRoom(Ljava/util/UUID;)V"))
    private static void scpclassifieddirective$requestAdjacentCameraOnly(
            UUID roomId) {
        Scp079AdjacentCameraSwitchNetwork.request(roomId);
    }
}
