package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomAbilityManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mirrors SCP:SL's pre-Tier-5 lockdown rule: no AP regeneration while active. */
@Mixin(value = Scp079ProcessingManager.class, remap = false)
public abstract class Scp079LockdownPowerRegenMixin {
    @Redirect(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/facility/Scp079FacilityAccessManager;activeAuxiliaryGenerators(Lnet/minecraft/server/MinecraftServer;)I"),
            remap = false)
    private static int scpclassifieddirective$pauseRegenDuringLockdown(
            MinecraftServer server) {
        return Scp079RoomAbilityManager.blocksPowerRegeneration(server)
                ? 0
                : Scp079FacilityAccessManager.activeAuxiliaryGenerators(server);
    }
}
