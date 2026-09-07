package com.bl4ues.scpclassifieddirective.facility.surveillance;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Optical pivot geometry for the ceiling-mounted dome camera. */
public final class CeilingCameraViewGeometry {
    /** The authored lens points straight down in its neutral pose. */
    public static final float DEFAULT_DOWN_PITCH = 90.0F;

    // The eye plane is authored 1.75 Blockbench units from the [0, 16, 0]
    // spherical pivot. A tiny clearance keeps the feed outside the rendered lens.
    private static final double PIVOT_TO_LENS = 1.75D / 16.0D + 0.018D;
    // At horizontal pitch the spherical lens reaches the ceiling plane itself.
    // Keep the optical pivot slightly below that plane so the near clip never
    // samples the support block above the camera.
    private static final double CEILING_VIEW_CLEARANCE = 1.5D / 16.0D;

    private CeilingCameraViewGeometry() {
    }

    /** Neutral physical eye point, directly below the lowered ceiling pivot. */
    public static Vec3 baseEye(BlockPos pos) {
        Vec3 pivot = new Vec3(pos.getX() + 0.5D,
                pos.getY() + 1.0D - CEILING_VIEW_CLEARANCE,
                pos.getZ() + 0.5D);
        return pivot.add(forward(180.0F, DEFAULT_DOWN_PITCH)
                .scale(PIVOT_TO_LENS));
    }

    /**
     * Reconstructs the lowered dome pivot from the persisted neutral eye point
     * and moves the feed along the same spherical arc as the physical lens.
     */
    public static Vec3 lensFromBaseEye(Vec3 baseEye, float baseYaw,
            float basePitch, float yaw, float pitch) {
        if (baseEye == null) return Vec3.ZERO;
        Vec3 pivot = baseEye.subtract(forward(baseYaw, basePitch)
                .scale(PIVOT_TO_LENS));
        return pivot.add(forward(yaw, pitch).scale(PIVOT_TO_LENS));
    }

    private static Vec3 forward(float yaw, float pitch) {
        return Vec3.directionFromRotation(pitch, yaw).normalize();
    }
}
