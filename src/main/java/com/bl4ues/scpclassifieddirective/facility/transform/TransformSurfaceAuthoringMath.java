package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Shared ray math for surface authoring. Block hits are preferred when they
 * exist, while air clicks project onto the active authoring plane so baseline
 * and height selection never depend on a convenient vanilla block behind them.
 */
public final class TransformSurfaceAuthoringMath {
    private static final double DEFAULT_DISTANCE = 5.0D;
    private static final double MAX_DISTANCE = 32.0D;

    private TransformSurfaceAuthoringMath() {
    }

    public static Vec3 resolve(Player player, Vec3 blockHit, Vec3 start,
            Vec3 end, boolean snap) {
        if (player == null) return blockHit;
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 fallback = blockHit != null
                ? blockHit : eye.add(view.scale(DEFAULT_DISTANCE));
        Vec3 result;

        if (start == null) {
            result = fallback;
        } else if (end == null) {
            result = rayHorizontalPlane(eye, view, start.y);
            if (result == null) result = new Vec3(fallback.x, start.y, fallback.z);
            else result = new Vec3(result.x, start.y, result.z);
        } else {
            Vec3 baseline = end.subtract(start);
            Vec3 planeNormal = new Vec3(-baseline.z, 0.0D, baseline.x);
            result = rayPlane(eye, view, start, planeNormal);
            if (result == null) result = fallback;
        }

        return snap ? snap16(result) : result;
    }

    public static Vec3 snap16(Vec3 point) {
        if (point == null) return null;
        return new Vec3(snap16(point.x), snap16(point.y), snap16(point.z));
    }

    private static double snap16(double value) {
        return Math.rint(value * 16.0D) / 16.0D;
    }

    private static Vec3 rayHorizontalPlane(Vec3 eye, Vec3 view, double y) {
        if (Math.abs(view.y) < 1.0E-6D) return null;
        double t = (y - eye.y) / view.y;
        return valid(t) ? eye.add(view.scale(t)) : null;
    }

    private static Vec3 rayPlane(Vec3 eye, Vec3 view, Vec3 point,
            Vec3 planeNormal) {
        if (planeNormal.lengthSqr() < 1.0E-8D) return null;
        Vec3 normal = planeNormal.normalize();
        double denominator = view.dot(normal);
        if (Math.abs(denominator) < 1.0E-6D) return null;
        double t = point.subtract(eye).dot(normal) / denominator;
        return valid(t) ? eye.add(view.scale(t)) : null;
    }

    private static boolean valid(double distance) {
        return Double.isFinite(distance) && distance >= 0.0D
                && distance <= MAX_DISTANCE;
    }
}
