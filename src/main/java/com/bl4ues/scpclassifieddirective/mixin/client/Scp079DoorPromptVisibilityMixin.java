package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps SCP-079 door prompts attached to the visible middle/upper door surface.
 * Ceiling cameras look down from a steep angle, so a controller-height anchor
 * can leave the viewport while most of the actual door is still plainly visible.
 */
@Mixin(value = Scp079PlayableVisualsV2.class, remap = false)
public abstract class Scp079DoorPromptVisibilityMixin {
    @ModifyConstant(method = "anchor",
            constant = @Constant(doubleValue = 0.22D), require = 1)
    private static double scpclassifieddirective$centerDoorPrompt(
            double original) {
        return 0.90D;
    }

    @Inject(method = "visibleDoorFromCamera", at = @At("HEAD"),
            cancellable = true)
    private static void scpclassifieddirective$sampleVisibleDoorFace(
            Minecraft minecraft, Vec3 camera, BlockPos targetPos, Vec3 target,
            CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.level == null || minecraft.player == null
                || targetPos == null) {
            cir.setReturnValue(false);
            return;
        }

        BlockState targetState = minecraft.level.getBlockState(targetPos);
        Vec3 center = Vec3.atCenterOf(targetPos);
        Direction sideways = Direction.EAST;
        if (targetState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            sideways = targetState.getValue(HorizontalDirectionalBlock.FACING)
                    .getClockWise();
        }
        Vec3 side = new Vec3(sideways.getStepX(), 0.0D,
                sideways.getStepZ()).scale(0.28D);

        Vec3[] samples = new Vec3[] {
                target,
                center.add(0.0D, 0.50D, 0.0D),
                center.add(0.0D, 0.90D, 0.0D),
                center.add(0.0D, 1.28D, 0.0D),
                center.add(0.0D, 0.90D, 0.0D).add(side),
                center.add(0.0D, 0.90D, 0.0D).subtract(side)
        };

        for (Vec3 sample : samples) {
            BlockHitResult hit = clip(minecraft, camera, sample);

            // When a Ceiling Camera tilts toward the horizon, its optical point
            // travels through the dome's own block volume. VISUAL raycasts then
            // immediately hit the camera housing and falsely hide every device
            // in an otherwise unobstructed room. Step the ray beyond only that
            // near self-hit, then perform the normal obstruction test.
            if (hit.getType() == HitResult.Type.BLOCK
                    && minecraft.level.getBlockState(hit.getBlockPos())
                            .is(CeilingCameraModule.BLOCK.get())
                    && camera.distanceToSqr(hit.getLocation()) <= 0.80D * 0.80D) {
                Vec3 toward = sample.subtract(camera);
                if (toward.lengthSqr() > 1.0E-6D) {
                    Vec3 restart = camera.add(toward.normalize().scale(0.55D));
                    hit = clip(minecraft, restart, sample);
                }
            }

            if (hit.getType() != HitResult.Type.BLOCK
                    || camera.distanceToSqr(hit.getLocation()) + 0.35D
                            >= camera.distanceToSqr(sample)) {
                cir.setReturnValue(true);
                return;
            }

            BlockPos hitPos = hit.getBlockPos();
            BlockState hitState = minecraft.level.getBlockState(hitPos);
            if (BlastDoorModule.isStructureState(hitState)) {
                BlockPos controller = BlastDoorModule.controllerPosition(
                        minecraft.level, hitPos, hitState);
                if (targetPos.equals(controller)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (FacilityModule.isFacilityDoor(hitState)
                    && hitPos.distSqr(targetPos) <= 9.0D) {
                cir.setReturnValue(true);
                return;
            }

            // Doorframes are frequently one block beside the controller while
            // the rendered heavy door spans the opening. Accept only a very
            // near frame hit; this does not turn mapped doors into wallhacks.
            if (Math.abs(hitPos.getY() - targetPos.getY()) <= 2
                    && Math.abs(hitPos.getX() - targetPos.getX()) <= 1
                    && Math.abs(hitPos.getZ() - targetPos.getZ()) <= 1
                    && hit.getLocation().distanceToSqr(sample) <= 0.90D) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    private static BlockHitResult clip(Minecraft minecraft, Vec3 from,
            Vec3 to) {
        return minecraft.level.clip(new ClipContext(from, to,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                minecraft.player));
    }
}
