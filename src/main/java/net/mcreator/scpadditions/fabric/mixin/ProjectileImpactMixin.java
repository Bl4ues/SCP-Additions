package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
abstract class ProjectileImpactMixin {
    @Inject(
            method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At("HEAD"))
    private void scpAdditions$projectileImpact(
            HitResult hit,
            CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(
                (Projectile) (Object) this, hit));
    }
}
