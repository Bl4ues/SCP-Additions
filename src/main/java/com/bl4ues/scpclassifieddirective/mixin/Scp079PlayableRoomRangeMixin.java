package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

/** Uses authored room geometry for playable SCP-079 device interaction. */
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

    /**
     * Door lockdown already scans the live room geometry, while manual camera
     * control historically depended on the saved facility-access door set. A
     * door missing from that bookkeeping could therefore be locked down but not
     * clicked by SCP-079. Resolve the first nearestTracked call directly from
     * loaded AnimatedDoor blocks around the HUD target; the original method's
     * interface checks and AP handling still remain authoritative afterwards.
     */
    @Redirect(method = "performAction",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/facility/Scp079PlayableManager;nearestTracked(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Set;Lnet/minecraft/core/BlockPos;D)Lnet/minecraft/core/BlockPos;",
                    ordinal = 0),
            remap = false)
    private static BlockPos scpclassifieddirective$resolveLiveDoor(
            ServerLevel level, Set<?> ignoredTracked, BlockPos aimedPos,
            double radius, ServerPlayer player,
            Scp079PlayableManager.ManualAction action, BlockPos originalAimedPos) {
        if (level == null || aimedPos == null) return null;
        double radiusSqr = radius * radius;
        int reach = Math.max(1, (int) Math.ceil(radius));
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = aimedPos.getX() - reach;
                x <= aimedPos.getX() + reach; x++) {
            for (int y = aimedPos.getY() - reach;
                    y <= aimedPos.getY() + reach; y++) {
                for (int z = aimedPos.getZ() - reach;
                        z <= aimedPos.getZ() + reach; z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    double distance = candidate.distSqr(aimedPos);
                    if (distance > radiusSqr || distance >= bestDistance
                            || !level.hasChunkAt(candidate)
                            || !FacilityModule.isFacilityDoor(
                                    level.getBlockState(candidate))
                            || !Scp079RoomInteractionPolicy.allows(
                                    player, candidate)) {
                        continue;
                    }
                    best = candidate.immutable();
                    bestDistance = distance;
                }
            }
        }
        return best;
    }
}
