package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraViewGeometry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared physical frame for surveillance cameras authored on Surfaces. */
public final class TransformCameraGeometry {
    private TransformCameraGeometry() {
    }

    public static boolean isCamera(BlockState state) {
        return state != null && (state.is(
                SurveillanceCameraPlaceholderModule.BLOCK.get())
                || state.is(CeilingCameraModule.BLOCK.get()));
    }

    public static boolean isCeiling(BlockState state) {
        return state != null && state.is(CeilingCameraModule.BLOCK.get());
    }

    public static Frame frame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, BlockState state,
            int normalSign, boolean overlay) {
        if (surface == null || slot == null || !isCamera(state)) return null;
        int side = normalSign < 0 ? -1 : 1;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = TransformMath.safeNormalize(
                surface.gridFrameTangent(u, v).scale(side),
                new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 normal = TransformMath.safeNormalize(
                surface.gridNormal(u, v).scale(side),
                new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        Vec3 center = TransformSurfaceGeometry.cellCenter(surface, slot,
                normalSign, overlay);

        if (isCeiling(state)) {
            // Canonical ceiling camera: +Y points into its support and -Y
            // points through the lens into the room. Keep local +Z along the
            // Surface's long/tangent direction so dome yaw remains intuitive.
            Vec3 yAxis = normal.scale(-1.0D);
            Vec3 zAxis = tangent;
            Vec3 xAxis = TransformMath.safeNormalize(yAxis.cross(zAxis),
                    vertical);
            Vec3 localEye = CeilingCameraViewGeometry.baseEye(BlockPos.ZERO)
                    .subtract(Vec3.atCenterOf(BlockPos.ZERO));
            Vec3 eye = center.add(xAxis.scale(localEye.x))
                    .add(yAxis.scale(localEye.y))
                    .add(zAxis.scale(localEye.z));
            return new Frame(center, xAxis, yAxis, zAxis, eye, true);
        }

        // Wall camera is authored in the same canonical tangent/up/outward
        // basis used by rigid Surface fixtures.
        Vec3 eye = center.add(normal.scale(0.10D))
                .add(vertical.scale(0.03D));
        return new Frame(center, tangent, vertical, normal, eye, false);
    }

    public static Angles worldAngles(Vec3 direction) {
        Vec3 value = TransformMath.safeNormalize(direction,
                new Vec3(0.0D, 0.0D, 1.0D));
        double horizontal = Math.sqrt(value.x * value.x + value.z * value.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-value.x, value.z));
        float pitch = (float) -Math.toDegrees(
                Math.atan2(value.y, horizontal));
        return new Angles(yaw, pitch);
    }

    public record Frame(Vec3 center, Vec3 xAxis, Vec3 yAxis, Vec3 zAxis,
                        Vec3 eye, boolean ceiling) {
        private float baseLocalYaw() {
            // CeilingCameraBlockEntity stores yaw relative to BASE_YAW
            // (NORTH/180 degrees). Wall Surface cameras are authored SOUTH,
            // whose vanilla base yaw is zero. Keeping that same convention
            // here makes the physical dome, playable feed and autonomous
            // target all consume the exact same yaw value.
            return ceiling ? CeilingCameraModule.BASE_YAW : 0.0F;
        }

        public Vec3 worldDirection(float localYaw, float localPitch) {
            Vec3 local = Vec3.directionFromRotation(localPitch,
                    localYaw + baseLocalYaw());
            return TransformMath.safeNormalize(
                    xAxis.scale(local.x)
                            .add(yAxis.scale(local.y))
                            .add(zAxis.scale(local.z)),
                    zAxis.scale(-1.0D));
        }

        public Angles localAngles(Vec3 worldDirection) {
            Vec3 value = TransformMath.safeNormalize(worldDirection,
                    zAxis.scale(-1.0D));
            double x = value.dot(xAxis);
            double y = value.dot(yAxis);
            double z = value.dot(zAxis);
            double horizontal = Math.sqrt(x * x + z * z);
            float absoluteYaw = (float) Math.toDegrees(Math.atan2(-x, z));
            return new Angles(
                    net.minecraft.util.Mth.wrapDegrees(
                            absoluteYaw - baseLocalYaw()),
                    (float) -Math.toDegrees(Math.atan2(y, horizontal)));
        }

        public Angles worldAngles(float localYaw, float localPitch) {
            return TransformCameraGeometry.worldAngles(
                    worldDirection(localYaw, localPitch));
        }
    }

    public record Angles(float yaw, float pitch) {
    }
}
