package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives SCP-173 a small collision-safe snap reach at true contact distance. */
@Mixin(value = Scp173Entity.class, remap = false)
public abstract class Scp173ContactFixMixin {
    @Unique private static final double scpClassifiedDirective$snapGap = 0.28D;

    @Inject(method = "isInSnapRange", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$allowCollisionSafeSnap(LivingEntity target,
            CallbackInfoReturnable<Boolean> callback) {
        Scp173Entity self = (Scp173Entity) (Object) this;
        if (target == null || !self.hasLineOfSight(target)) {
            callback.setReturnValue(false);
            return;
        }

        AABB statue = self.getBoundingBox();
        AABB victim = target.getBoundingBox();
        double verticalOverlap = Math.min(statue.maxY, victim.maxY)
                - Math.max(statue.minY, victim.minY);
        if (verticalOverlap <= 0.0D) {
            callback.setReturnValue(false);
            return;
        }

        double gapX = Math.max(0.0D, Math.max(victim.minX - statue.maxX,
                statue.minX - victim.maxX));
        double gapZ = Math.max(0.0D, Math.max(victim.minZ - statue.maxZ,
                statue.minZ - victim.maxZ));
        callback.setReturnValue(gapX * gapX + gapZ * gapZ
                <= scpClassifiedDirective$snapGap * scpClassifiedDirective$snapGap);
    }
}
