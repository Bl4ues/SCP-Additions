package net.mcreator.scpadditions.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.mcreator.scpadditions.entity.Scp173Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives SCP-173 collision-safe contact reach and precise narrow-route alignment. */
@Mixin(value = Scp173Entity.class, remap = false)
public abstract class Scp173ContactFixMixin {
    @Unique private static final double scpAdditions$snapGap = 0.28D;
    @Unique private static final double scpAdditions$narrowNodeTolerance = 0.08D;

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

    @ModifyConstant(method = "trimReachedNodes",
            constant = @Constant(doubleValue = 0.55D * 0.55D), require = 1)
    private double scpAdditions$centerBeforeSkippingTrimmedNode(double original) {
        return scpAdditions$narrowNodeTolerance
                * scpAdditions$narrowNodeTolerance;
    }

    @ModifyConstant(method = "stepAlongRememberedRoute",
            constant = @Constant(doubleValue = 0.55D * 0.55D), require = 1)
    private double scpAdditions$centerBeforeSkippingRememberedNode(double original) {
        // SCP-173 is 0.82 blocks wide. A one-block doorway leaves only about
        // 0.09 blocks of clearance on each side, so the previous 0.55-block
        // reached radius let it abandon the doorway-center node while still
        // physically wedged against the frame and aim diagonally at the next one.
        return scpAdditions$narrowNodeTolerance
                * scpAdditions$narrowNodeTolerance;
    }
}
