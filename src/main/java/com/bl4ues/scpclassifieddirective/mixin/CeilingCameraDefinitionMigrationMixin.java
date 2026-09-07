package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraViewGeometry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lazily upgrades ceiling-camera records created before the final 90-degree arc. */
@Mixin(value = FacilitySurveillanceRegistry.class, remap = false)
public abstract class CeilingCameraDefinitionMigrationMixin {
    @Inject(method = "normalizePlaceholder", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$refreshCeilingDefinition(
            ServerLevel level, FacilityCameraDefinition camera,
            CallbackInfoReturnable<FacilityCameraDefinition> cir) {
        if (level == null || camera == null) return;
        BlockState state = level.getBlockState(camera.anchorPos());
        if (!state.is(CeilingCameraModule.BLOCK.get())) return;

        Vec3 eye = CeilingCameraModule.eyePosition(camera.anchorPos(), state);
        float baseYaw = CeilingCameraModule.BASE_YAW;
        float basePitch = CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;
        float yawLimit = CeilingCameraModule.MANUAL_YAW_LIMIT;
        float minPitch = CeilingCameraModule.MANUAL_MIN_PITCH;
        float maxPitch = CeilingCameraModule.MANUAL_MAX_PITCH;

        boolean current = Float.compare(camera.baseYaw(), baseYaw) == 0
                && Float.compare(camera.basePitch(), basePitch) == 0
                && Float.compare(camera.yawLimit(), yawLimit) == 0
                && Float.compare(camera.minPitch(), minPitch) == 0
                && Float.compare(camera.maxPitch(), maxPitch) == 0
                && camera.eyePosition().distanceToSqr(eye) < 0.000001D;
        if (current) {
            cir.setReturnValue(camera);
            return;
        }

        FacilityCameraDefinition upgraded = new FacilityCameraDefinition(
                camera.id(), camera.dimension(), camera.anchorPos(), eye,
                camera.name(), baseYaw, basePitch, yawLimit,
                minPitch, maxPitch, camera.maxZoom());
        FacilitySurveillanceSavedData.get(level.getServer()).put(upgraded);
        cir.setReturnValue(upgraded);
    }
}
