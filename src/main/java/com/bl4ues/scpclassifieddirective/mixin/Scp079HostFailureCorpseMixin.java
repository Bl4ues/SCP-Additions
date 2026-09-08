package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.death.PlayerCorpseManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079HostFailureDeathGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SCP-079 has no humanoid body to leave behind when its host is destroyed. */
@Mixin(value = PlayerCorpseManager.class, remap = false)
public abstract class Scp079HostFailureCorpseMixin {
    @Inject(method = "onPlayerDeath", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$skip079Corpse(
            LivingDeathEvent event, CallbackInfo ci) {
        if (event.getEntity() instanceof ServerPlayer player
                && Scp079HostFailureDeathGuard.active(player)) {
            ci.cancel();
        }
    }
}
