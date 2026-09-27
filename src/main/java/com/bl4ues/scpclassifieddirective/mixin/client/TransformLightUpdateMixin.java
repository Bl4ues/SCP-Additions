package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Curved Surface meshes are rendered outside Minecraft's chunk mesh and cache
 * their packed lightmap UVs. Reconcile them from the exact vanilla light-data
 * application point so parent/child planes never freeze different propagation
 * generations after a light source changes.
 */
@Mixin(ClientPacketListener.class)
public abstract class TransformLightUpdateMixin {
    @Inject(method = "applyLightData", at = @At("TAIL"))
    private void scpClassifiedDirective$refreshCurvedSurfaceLightmaps(
            int chunkX, int chunkZ, ClientboundLightUpdatePacketData data,
            CallbackInfo ci) {
        TransformConstructionClientRenderer.invalidateLightingChunk(
                chunkX, chunkZ);
    }
}
