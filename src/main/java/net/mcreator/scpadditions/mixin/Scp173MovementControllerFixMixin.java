package net.mcreator.scpadditions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173MovementController;
import net.mcreator.scpadditions.facility.FacilityModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/** Keeps observed gravity, snap attacks and collision-route repair coherent for SCP-173. */
@Mixin(value = Scp173MovementController.class, remap = false)
public abstract class Scp173MovementControllerFixMixin {
    @Unique private static final Method scpAdditions$observationLocked =
            scpAdditions$method("isObservationLocked");
    @Unique private static final Method scpAdditions$trySnapAttack =
            scpAdditions$method("trySnapAttack", LivingEntity.class);
    @Unique private static final double scpAdditions$narrowNodeTolerance = 0.08D;

    @Inject(method = "validateAndRepair", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$preserveObservedFall(ServerLevel level,
            Scp173Entity statue, CallbackInfo callback) {
        if (statue == null || !scpAdditions$isObserved(statue)) return;

        // Scp173Entity already freezes horizontal movement and applies its own
        // heavy vertical motion while observed. The external repair controller
        // must never restore the tick-start snapshot here: doing so undoes a
        // mid-air fall, including the tick on which the statue reaches ground.
        statue.getNavigation().stop();
        statue.getMoveControl().setWantedPosition(statue.getX(),
                statue.getY(), statue.getZ(), 0.0D);
        statue.setDeltaMovement(0.0D, 0.0D, 0.0D);
        callback.cancel();
    }

    @Inject(method = "applyStep", at = @At("TAIL"))
    private static void scpAdditions$attackAfterRepairStep(ServerLevel level,
            Scp173Entity statue, LivingEntity target, Vec3 step,
            CallbackInfo callback) {
        if (statue == null || target == null || scpAdditions$trySnapAttack == null) {
            return;
        }
        try {
            scpAdditions$trySnapAttack.invoke(statue, target);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not perform SCP-173 snap attack after route repair",
                    exception);
        }
    }

    @Inject(method = "isSoftObstacle", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$ignoreOpenFacilityDoors(Level level,
            BlockPos pos, BlockState state, VoxelShape shape,
            CallbackInfoReturnable<Boolean> callback) {
        if (FacilityModule.isDoorPassable(state)) {
            callback.setReturnValue(true);
        }
    }

    @ModifyConstant(method = "findLocalCollisionRoute",
            constant = @Constant(intValue = 2600), require = 0)
    private static int scpAdditions$expandCornerSearch(int original) {
        return 7200;
    }

    @ModifyConstant(method = "findLocalCollisionRoute",
            constant = @Constant(doubleValue = 0.38D * 0.38D), require = 1)
    private static double scpAdditions$centerLocalRouteStart(double original) {
        return scpAdditions$narrowNodeTolerance
                * scpAdditions$narrowNodeTolerance;
    }

    @ModifyConstant(method = "firstVanillaPathStep",
            constant = @Constant(doubleValue = 0.38D * 0.38D), require = 1)
    private static double scpAdditions$centerVanillaFallbackNode(double original) {
        return scpAdditions$narrowNodeTolerance
                * scpAdditions$narrowNodeTolerance;
    }

    @Unique
    private static boolean scpAdditions$isObserved(Scp173Entity statue) {
        if (scpAdditions$observationLocked != null) {
            try {
                return (boolean) scpAdditions$observationLocked.invoke(statue);
            } catch (ReflectiveOperationException exception) {
                ScpAdditionsMod.LOGGER.warn(
                        "Could not query SCP-173 observation lock", exception);
            }
        }
        return statue.level().players().stream().anyMatch(statue::isObservedBy);
    }

    @Unique
    private static Method scpAdditions$method(String name,
            Class<?>... parameters) {
        try {
            Method method = Scp173Entity.class.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
