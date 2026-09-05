package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces the arbitrary camera-distance gate with the authored room floor. */
@Mixin(Scp079PlayableManager.class)
public abstract class Scp079PlayableRoomRangeMixin {
    @Redirect(method = "performAction",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"),
            remap = false)
    private static double scpclassifieddirective$mappedRoomRange(
            Vec3 cameraEye, Vec3 targetCenter, ServerPlayer player,
            Scp079PlayableManager.ManualAction action, BlockPos aimedPos) {
        if (Scp079RoomInteractionPolicy.allows(player, aimedPos)) return 0.0D;
        return cameraEye.distanceToSqr(targetCenter);
    }
}
