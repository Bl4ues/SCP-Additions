package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppresses only the vanilla hit-feedback particle when the local preference is enabled. */
@Mixin(ParticleEngine.class)
public abstract class DamageIndicatorParticleMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$hideDamageIndicator(ParticleOptions options,
            double x, double y, double z, double xSpeed, double ySpeed,
            double zSpeed, CallbackInfoReturnable<Particle> callback) {
        if (ClientModulePreferences.hideDamageIndicatorParticles()
                && options != null
                && options.getType() == ParticleTypes.DAMAGE_INDICATOR) {
            callback.setReturnValue(null);
        }
    }
}
