package com.bl4ues.scpclassifieddirective.facility.surveillance;

import net.minecraft.world.phys.Vec3;

/**
 * Shared geometry for the authored surveillance-camera head.
 *
 * <p>The camera feed must originate at the physical lens, not rotate around a
 * fixed point in the middle of the block. The constants below correspond to
 * the authored {@code camera_pitch} pivot and lens offset in the GeckoLib
 * model, expressed in block units.</p>
 */
public final class SurveillanceCameraViewGeometry {
    /** Neutral mounting angle. Wall cameras look slightly down by default. */
    public static final float DEFAULT_DOWN_PITCH = 15.0F;

    private static final double PIVOT_TO_LENS_FORWARD = 4.75D / 16.0D;
    private static final double PIVOT_TO_LENS_UP = 1.0D / 16.0D;
    private static final double OPTICAL_CLEARANCE = 0.018D;

    private SurveillanceCameraViewGeometry() {
    }

    /**
     * Reconstruct the mechanical pivot from the persisted neutral eye point,
     * then move the eye along the same arc as the physical camera head.
     */
    public static Vec3 lensFromBaseEye(Vec3 baseEye, float baseYaw,
            float basePitch, float yaw, float pitch) {
        if (baseEye == null) return Vec3.ZERO;
        Vec3 baseForward = forward(baseYaw, basePitch);
        Vec3 baseUp = up(baseYaw, basePitch, baseForward);
        Vec3 pivot = baseEye
                .subtract(baseForward.scale(PIVOT_TO_LENS_FORWARD
                        + OPTICAL_CLEARANCE))
                .subtract(baseUp.scale(PIVOT_TO_LENS_UP));
        return lensFromPivot(pivot, yaw, pitch);
    }

    public static Vec3 lensFromPivot(Vec3 pivot, float yaw, float pitch) {
        Vec3 forward = forward(yaw, pitch);
        Vec3 up = up(yaw, pitch, forward);
        return pivot
                .add(forward.scale(PIVOT_TO_LENS_FORWARD
                        + OPTICAL_CLEARANCE))
                .add(up.scale(PIVOT_TO_LENS_UP));
    }

    private static Vec3 forward(float yaw, float pitch) {
        return Vec3.directionFromRotation(pitch, yaw).normalize();
    }

    private static Vec3 up(float yaw, float pitch, Vec3 forward) {
        Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F)
                .normalize();
        Vec3 up = right.cross(forward);
        return up.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 1.0D, 0.0D) : up.normalize();
    }
}
