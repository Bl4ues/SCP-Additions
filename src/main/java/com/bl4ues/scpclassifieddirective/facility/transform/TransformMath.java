package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
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

    /**
     * Rotation whose local X/Y/Z axes match the supplied world-space frame.
     * The frame is normalized/orthogonalized first so spline rounding cannot
     * leak scale or shear into GeckoLib/BlockEntity renderers.
     */
    public static Quaternionf frameQuaternion(Vec3 xAxis, Vec3 yAxis,
            Vec3 zAxis) {
        Vec3 x = safeNormalize(xAxis, new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 z = safeNormalize(zAxis, new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 y = safeNormalize(yAxis, z.cross(x));
        z = safeNormalize(x.cross(y), z);
        y = safeNormalize(z.cross(x), y);
        Matrix3f matrix = new Matrix3f(
                (float) x.x, (float) x.y, (float) x.z,
                (float) y.x, (float) y.y, (float) y.z,
                (float) z.x, (float) z.y, (float) z.z).transpose();
        return new Quaternionf().setFromNormalized(matrix);
    }

    public static Vec3 rotate(Vec3 vector, float xDegrees, float yDegrees,
            float zDegrees) {
        Vector3f transformed = new Vector3f((float) vector.x,
                (float) vector.y, (float) vector.z);
        quaternion(xDegrees, yDegrees, zDegrees).transform(transformed);
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    public static float[] composeAxisRotation(float xDegrees,
            float yDegrees, float zDegrees, Vec3 worldAxis,
            float deltaDegrees, boolean localSpace) {
        Vec3 axis = safeNormalize(worldAxis, new Vec3(0.0D, 1.0D, 0.0D));
        Quaternionf base = quaternion(xDegrees, yDegrees, zDegrees);
        Quaternionf delta = new Quaternionf().fromAxisAngleRad(
                (float) axis.x, (float) axis.y, (float) axis.z,
                (float) Math.toRadians(deltaDegrees));
        Quaternionf result = localSpace
                ? new Quaternionf(base).mul(delta)
                : new Quaternionf(delta).mul(base);
        Vector3f euler = result.getEulerAnglesXYZ(new Vector3f());
        return new float[]{
                (float) Math.toDegrees(euler.x),
                (float) Math.toDegrees(euler.y),
                (float) Math.toDegrees(euler.z)
        };
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
