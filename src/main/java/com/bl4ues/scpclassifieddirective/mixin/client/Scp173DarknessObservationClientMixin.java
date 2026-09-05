package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173ObservationLighting;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the client observation heartbeat from freezing SCP-173 in darkness. */
@Mixin(value = Scp173Entity.class, remap = false)
public abstract class Scp173DarknessObservationClientMixin {
    @Inject(method = "isClientObservedByLocalPlayer()Z", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void scpclassifieddirective$requireVisibleClientLight(
            CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (Scp079PlayableClient.active()) {
            cir.setReturnValue(false);
            return;
        }
        if (!Scp173ObservationLighting.canObserve(
                (Scp173Entity) (Object) this, minecraft.player)) {
            cir.setReturnValue(false);
        }
    }
}
