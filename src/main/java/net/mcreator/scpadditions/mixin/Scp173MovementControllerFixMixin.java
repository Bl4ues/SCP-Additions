package net.mcreator.scpadditions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

/** Keeps observed gravity and collision-route repair coherent for SCP-173. */
@Mixin(value = Scp173MovementController.class, remap = false)
public abstract class Scp173MovementControllerFixMixin {
    @Unique private static final Method scpAdditions$observationLocked =
            scpAdditions$method("isObservationLocked");

    @Inject(method = "validateAndRepair", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$preserveObservedFall(ServerLevel level,
            Scp173Entity statue, CallbackInfo callback) {
        if (statue == null || statue.onGround()
                || !scpAdditions$isObserved(statue)) {
            return;
        }

        // Scp173Entity already froze X/Z and applied its heavy vertical physics
        // during its own tick. Restoring the tick-start snapshot here used to
        // undo that Y movement and leave an observed statue suspended in air.
        statue.getNavigation().stop();
        statue.getMoveControl().setWantedPosition(statue.getX(),
                statue.getY(), statue.getZ(), 0.0D);
        statue.setDeltaMovement(0.0D, 0.0D, 0.0D);
        callback.cancel();
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
    private static Method scpAdditions$method(String name) {
        try {
            Method method = Scp173Entity.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
