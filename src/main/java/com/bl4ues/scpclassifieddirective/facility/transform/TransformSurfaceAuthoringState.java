package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.world.phys.Vec3;

/**
 * Lightweight client authoring mirror for the three-click surface workflow.
 * The dedicated server has its own JVM and never consumes this state; keeping
 * the data free of client-only classes lets the item update the preview without
 * another packet for every authoring click.
 */
public final class TransformSurfaceAuthoringState {
    private static Vec3 start;
    private static Vec3 end;

    private TransformSurfaceAuthoringState() {
    }

    public static Vec3 start() {
        return start;
    }

    public static Vec3 end() {
        return end;
    }

    public static boolean active() {
        return start != null;
    }

    public static int step() {
        if (start == null) return 0;
        return end == null ? 1 : 2;
    }

    public static void select(Vec3 hit) {
        if (hit == null) return;
        if (start == null) {
            start = hit;
            end = null;
            return;
        }
        if (end == null) {
            Vec3 candidate = new Vec3(hit.x, start.y, hit.z);
            if (candidate.distanceToSqr(start) >= 0.04D) end = candidate;
            return;
        }
        clear();
    }

    public static void clear() {
        start = null;
        end = null;
    }
}
