package net.mcreator.scpadditions.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.mcreator.scpadditions.entity.Scp173Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents contact shoving and gives SCP-173 a small collision-safe snap reach. */
@Mixin(value = Scp173Entity.class, remap = false)
public abstract class Scp173ContactFixMixin {
    @Unique private static final double scpAdditions$snapGap = 0.28D;

    /** SCP-173 is effectively a concrete statue, not a multiplayer beach ball. */
    public boolean isPushable() {
        return false;
    }

    @Inject(method = "isInSnapRange", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$allowCollisionSafeSnap(LivingEntity target,
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
                <= scpAdditions$snapGap * scpAdditions$snapGap);
    }
}
