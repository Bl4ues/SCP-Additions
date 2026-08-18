package net.mcreator.scpadditions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps cached repair-route nodes precise enough for one-block-wide passages. */
@Mixin(targets = "net.mcreator.scpadditions.entity.Scp173MovementController$MovementState",
        remap = false)
public abstract class Scp173MovementStateFixMixin {
    @Unique private static final double scpAdditions$narrowNodeTolerance = 0.08D;

    @ModifyConstant(method = "advanceReachedNodes",
            constant = @Constant(doubleValue = 0.38D * 0.38D), require = 1)
    private double scpAdditions$centerBeforeAdvancingCachedRoute(double original) {
        return scpAdditions$narrowNodeTolerance
                * scpAdditions$narrowNodeTolerance;
    }
}
