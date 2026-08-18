package net.mcreator.scpadditions.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173UnityNavigator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Keeps SCP-173's authored/manual yaw pointed at its pursuit target while the
 * navigator follows detour waypoints. This matters most during blinks: the
 * client cannot visually correct the rotation while the player's eyes are
 * closed, so the synchronized MANUAL_YAW must already contain the pose that
 * should be revealed when observation resumes.
 */
@Mixin(value = Scp173UnityNavigator.class, remap = false)
public abstract class Scp173FacingSyncMixin {
    @Unique
    private static final Method scpAdditions$setManualYaw =
            scpAdditions$findSetManualYaw();
    @Unique
    private static boolean scpAdditions$warningLogged;

    @Inject(method = "faceMovement", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$facePursuitTarget(Scp173Entity statue,
            Vec3 movement, CallbackInfo callback) {
        if (statue == null) {
            callback.cancel();
            return;
        }

        // Compute from the position the statue will occupy after this snap so
        // the pose revealed at the end of a blink faces the target from the
        // new location, not from the previous path node.
        Vec3 origin = statue.position();
        if (movement != null) origin = origin.add(movement);

        LivingEntity target = statue.getTarget();
        Vec3 facing = target != null && target.isAlive() && !target.isRemoved()
                ? target.position().subtract(origin)
                : movement;
        if (facing == null
                || facing.x * facing.x + facing.z * facing.z <= 1.0E-8D) {
            callback.cancel();
            return;
        }

        float yaw = Mth.wrapDegrees((float) (Mth.atan2(facing.z, facing.x)
                * Mth.RAD_TO_DEG) - 90.0F);
        if (scpAdditions$setManualYaw != null) {
            try {
                scpAdditions$setManualYaw.invoke(statue, yaw);
                callback.cancel();
                return;
            } catch (ReflectiveOperationException exception) {
                if (!scpAdditions$warningLogged) {
                    scpAdditions$warningLogged = true;
                    ScpAdditionsMod.LOGGER.warn(
                            "Could not synchronize SCP-173 pursuit facing",
                            exception);
                }
            }
        }

        // Safe fallback if internal access ever changes. It cannot synchronize
        // MANUAL_YAW, but still preserves the correct server pose for this tick.
        statue.setYRot(yaw);
        statue.yRotO = yaw;
        statue.yBodyRot = yaw;
        statue.yBodyRotO = yaw;
        statue.yHeadRot = yaw;
        statue.yHeadRotO = yaw;
        callback.cancel();
    }

    @Unique
    private static Method scpAdditions$findSetManualYaw() {
        try {
            Method method = Scp173Entity.class.getDeclaredMethod(
                    "setManualYaw", float.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
