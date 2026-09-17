package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Shared local/world transform math for off-grid groups and editable surfaces. */
public final class TransformMath {
    private TransformMath() {
    }

    public static Quaternionf quaternion(float xDegrees, float yDegrees,
            float zDegrees) {
        return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(xDegrees),
                (float) Math.toRadians(yDegrees),
                (float) Math.toRadians(zDegrees));
    }

    public static Vec3 rotate(Vec3 vector, float xDegrees, float yDegrees,
            float zDegrees) {
        Vector3f transformed = new Vector3f((float) vector.x,
                (float) vector.y, (float) vector.z);
        quaternion(xDegrees, yDegrees, zDegrees).transform(transformed);
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    public static Vec3 inverseRotate(Vec3 vector, float xDegrees,
            float yDegrees, float zDegrees) {
        Vector3f transformed = new Vector3f((float) vector.x,
                (float) vector.y, (float) vector.z);
        quaternion(xDegrees, yDegrees, zDegrees).conjugate()
                .transform(transformed);
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    public static Vec3 localToWorld(Vec3 origin, Vec3 local,
            float xDegrees, float yDegrees, float zDegrees) {
        return origin.add(rotate(local, xDegrees, yDegrees, zDegrees));
    }

    public static Vec3 worldToLocal(Vec3 origin, Vec3 world,
            float xDegrees, float yDegrees, float zDegrees) {
        return inverseRotate(world.subtract(origin), xDegrees, yDegrees,
                zDegrees);
    }

    public static Vec3 quadratic(Vec3 start, Vec3 control, Vec3 end,
            double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        double inverse = 1.0D - clamped;
        return start.scale(inverse * inverse)
                .add(control.scale(2.0D * inverse * clamped))
                .add(end.scale(clamped * clamped));
    }

    public static Vec3 quadraticTangent(Vec3 start, Vec3 control, Vec3 end,
            double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        return control.subtract(start).scale(2.0D * (1.0D - clamped))
                .add(end.subtract(control).scale(2.0D * clamped));
    }

    public static Vec3 safeNormalize(Vec3 value, Vec3 fallback) {
        if (value == null || value.lengthSqr() < 1.0E-10D) return fallback;
        return value.normalize();
    }
}
