package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmMountStructure;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Projects only onto actual backing wall faces. Empty logical cells and open
 * doorways cannot receive a free-floating cone. The vanilla Alarm's separate
 * scene projector continues to handle ordinary untransformed world geometry.
 */
final class TransformAlarmPhysicalProjection {
    // The broad, feathered wash is the visible cone. Emissive is only a
    // secondary, low-alpha pass, matching the vanilla Alarm projector.
    private static final RenderType WASH = RenderType.entityTranslucent(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/effect/alarm_light_splash.png"), true);
    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "textures/effect/alarm_light_emissive.png"));
    private static final double FACE_OFFSET = 0.008D;
    private static final double HALF_WIDTH = 1.72D;
    private static final double HALF_HEIGHT = 3.58D;
    private static final double MAX_DISTANCE_SQR = 24.0D * 24.0D;

    private TransformAlarmPhysicalProjection() {
    }

    static void renderGroup(TransformGroup group, TransformGroup.GridPos cell,
            AlarmModule.AlarmBlockEntity alarm, float partialTick,
            PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 camera) {
        BlockState state = alarm.getBlockState();
        if (!state.getValue(AlarmModule.ACTIVE)) return;
        Direction facing = state.getValue(AlarmModule.FACING);
        if (!facing.getAxis().isHorizontal()) return;
        Direction localRight = facing.getClockWise();
        Vec3 outward = worldAxis(group, facing);
        Vec3 right = worldAxis(group, localRight);
        Vec3 up = worldAxis(group, Direction.UP);
        Vec3 mount = AlarmMountStructure.visualOffset(state);
        Vec3 lamp = group.cellCenter(cell)
                .subtract(outward.scale(0.4975D))
                .add(right.scale(mount.dot(axis(localRight))))
                .add(up.scale(mount.y));
        if (lamp.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;
        Projector projector = new Projector(lamp, right, up,
                alarm.projectionPhase(partialTick));
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (int pass = 0; pass < 2; pass++) {
            RenderType layer = pass == 0 ? WASH : GLOW;
            VertexConsumer consumer = buffers.getBuffer(layer);
            boolean emitted = false;
            for (int horizontal = -3; horizontal <= 3; horizontal++) {
                for (int vertical = -4; vertical <= 4; vertical++) {
                TransformGroup.GridPos support = cell.offset(
                        -facing.getStepX() + localRight.getStepX() * horizontal,
                        vertical,
                        -facing.getStepZ() + localRight.getStepZ() * horizontal);
                if (!fullReceiver(group.cells().get(support))) continue;
                Vec3 center = group.cellCenter(support)
                        .add(outward.scale(0.5D + FACE_OFFSET));
                Vec3 a = center.subtract(right.scale(0.5D))
                        .subtract(up.scale(0.5D));
                Vec3 b = center.add(right.scale(0.5D))
                        .subtract(up.scale(0.5D));
                Vec3 c = center.add(right.scale(0.5D))
                        .add(up.scale(0.5D));
                Vec3 d = center.subtract(right.scale(0.5D))
                        .add(up.scale(0.5D));
                emitted |= emit(consumer, pose, projector, outward, a, b, c, d,
                        pass == 0 ? 255 : 87);
            }
        }
            if (emitted) buffers.endBatch(layer);
        }
        pose.popPose();
    }

    static void renderSurface(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay, AlarmModule.AlarmBlockEntity alarm,
            float partialTick, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera) {
        BlockState state = alarm.getBlockState();
        if (!state.getValue(AlarmModule.ACTIVE)
                || state.getValue(AlarmModule.FACING) != Direction.SOUTH) return;
        int alarmSide = normalSign < 0 ? -1 : 1;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 normal = surface.gridNormal(u, v).scale(alarmSide);
        Vec3 tangent = surface.gridFrameTangent(u, v).scale(alarmSide);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        Vec3 right = tangent.scale(-1.0D); // Local SOUTH.getClockWise() is WEST.
        Vec3 mount = AlarmMountStructure.visualOffset(state);
        Vec3 lamp = TransformSurfaceGeometry.cellCenter(surface, slot,
                        alarmSide, overlay)
                .subtract(normal.scale(0.4975D))
                .add(right.scale(-mount.x))
                .add(vertical.scale(mount.y));
        if (lamp.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;
        // The main-layer Alarm rests on the inner overlay. Both overlay
        // Alarms face a real structural wall, at its inner or outer face.
        int backingSign = overlay ? 0 : -1;
        boolean backingOverlay = !overlay;
        double z = overlay && alarmSide > 0 ? 1.0D : 0.0D;
        Projector projector = new Projector(lamp, right, vertical,
                alarm.projectionPhase(partialTick));
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (int pass = 0; pass < 2; pass++) {
            RenderType layer = pass == 0 ? WASH : GLOW;
            VertexConsumer consumer = buffers.getBuffer(layer);
            boolean emitted = false;
            for (int dc = -3; dc <= 3; dc++) {
                for (int dr = -4; dr <= 4; dr++) {
                ConstructionSurface.SurfaceSlot receiver =
                        new ConstructionSurface.SurfaceSlot(
                                slot.column() + dc, slot.row() + dr);
                if (receiver.column() < 0 || receiver.column() >= surface.columns()
                        || receiver.row() < 0 || receiver.row() >= surface.rows()) {
                    continue;
                }
                ConstructionSurface.SurfaceAttachment backing = backingOverlay
                        ? surface.overlay(receiver, backingSign)
                        : surface.attachments().get(receiver);
                if (backing == null || !fullReceiver(backing.state())) continue;
                boolean deform = TransformSurfaceGeometry.effectiveDeform(backing);
                // Four patches per cell follow the real parametric face. A flat
                // world-space rectangle would cut through a strongly bent wall.
                // Match the payload mesh tessellation exactly. The previous
                // 2x2 receiver was a different polygonal approximation to the
                // same curved wall, so its triangles crossed behind the wall
                // and appeared as short luminous slits under depth testing.
                int horizontalSteps = deform ? 4 : 1;
                int verticalSteps = deform
                        && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                        ? 2 : 1;
                for (int x = 0; x < horizontalSteps; x++) {
                    for (int y = 0; y < verticalSteps; y++) {
                        double x0 = x / (double) horizontalSteps;
                        double x1 = (x + 1.0D) / horizontalSteps;
                        double y0 = y / (double) verticalSteps;
                        double y1 = (y + 1.0D) / verticalSteps;
                        Vec3 a = receiverPoint(surface, receiver, deform,
                                backingSign, backingOverlay, z, alarmSide, x0, y0);
                        Vec3 b = receiverPoint(surface, receiver, deform,
                                backingSign, backingOverlay, z, alarmSide, x1, y0);
                        Vec3 c = receiverPoint(surface, receiver, deform,
                                backingSign, backingOverlay, z, alarmSide, x1, y1);
                        Vec3 d = receiverPoint(surface, receiver, deform,
                                backingSign, backingOverlay, z, alarmSide, x0, y1);
                        emitted |= emit(consumer, pose, projector, normal,
                                a, b, c, d, pass == 0 ? 255 : 87);
                    }
                }
            }
        }
            if (emitted) buffers.endBatch(layer);
        }
        pose.popPose();
    }

    private static Vec3 receiverPoint(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, boolean deform,
            int backingSign, boolean backingOverlay, double z,
            int faceSign, double x, double y) {
        double u = (slot.column() + x) / surface.columns();
        double v = (slot.row() + y) / surface.rows();
        return TransformSurfaceGeometry.logicalPoint(surface, slot, deform,
                        backingSign == 0 ? 1 : backingSign,
                        backingOverlay, x, y, z)
                .add(surface.gridNormal(u, v).scale(faceSign * FACE_OFFSET));
    }

    private static boolean fullReceiver(BlockState state) {
        if (state == null || state.isAir()
                || FacilityModule.isDoorPassable(state)
                || state.getBlock() instanceof AbstractGlassBlock
                || state.is(Blocks.GLASS_PANE)) return false;
        // Facility walls commonly render as full opaque-looking cubes with
        // noOcclusion for custom textures. canOcclude() would silently reject
        // their physical faces even when they completely fill the cell.
        List<AABB> boxes = state.getCollisionShape(EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO, CollisionContext.empty()).toAabbs();
        if (boxes.size() != 1) return false;
        AABB box = boxes.get(0);
        return box.minX < 1.0E-4D && box.minY < 1.0E-4D
                && box.minZ < 1.0E-4D && box.maxX > 0.9999D
                && box.maxY > 0.9999D && box.maxZ > 0.9999D;
    }

    private static Vec3 axis(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(),
                direction.getStepZ());
    }

    private static Vec3 worldAxis(TransformGroup group, Direction direction) {
        return TransformMath.rotate(axis(direction), group.rotationX(),
                group.rotationY(), group.rotationZ());
    }

    private static boolean emit(VertexConsumer output, PoseStack pose,
            Projector projector, Vec3 normal,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d, int alpha) {
        // Match the vertex winding to the visible side before UV clipping.
        if (b.subtract(a).cross(d.subtract(a)).dot(normal) < 0.0D) {
            Vec3 swap = b;
            b = d;
            d = swap;
        }
        List<Sample> polygon = List.of(projector.sample(a),
                projector.sample(b), projector.sample(c), projector.sample(d));
        polygon = clip(polygon, 0, 0.0F, true);
        polygon = clip(polygon, 0, 1.0F, false);
        polygon = clip(polygon, 1, 0.0F, true);
        polygon = clip(polygon, 1, 1.0F, false);
        if (polygon.size() < 3) return false;
        Sample first = polygon.get(0);
        for (int i = 1; i + 1 < polygon.size(); i++) {
            Sample previous = polygon.get(i);
            Sample next = polygon.get(i + 1);
            // The RenderType consumes QUADS, so emit a clipped triangle as a
            // degenerate quad without a second translucent coplanar layer.
            vertex(output, pose, first, normal, alpha);
            vertex(output, pose, previous, normal, alpha);
            vertex(output, pose, next, normal, alpha);
            vertex(output, pose, next, normal, alpha);
        }
        return true;
    }

    private static List<Sample> clip(List<Sample> input, int coordinate,
            float bound, boolean keepGreater) {
        if (input.isEmpty()) return input;
        List<Sample> output = new ArrayList<>(input.size() + 1);
        Sample previous = input.get(input.size() - 1);
        boolean previousInside = inside(previous, coordinate, bound, keepGreater);
        for (Sample current : input) {
            boolean currentInside = inside(current, coordinate, bound, keepGreater);
            if (currentInside != previousInside) {
                float before = coordinate == 0 ? previous.u() : previous.v();
                float after = coordinate == 0 ? current.u() : current.v();
                float denominator = after - before;
                if (Math.abs(denominator) > 1.0E-7F) {
                    double t = (bound - before) / denominator;
                    output.add(new Sample(previous.world().lerp(current.world(), t),
                            (float) (previous.u() + (current.u() - previous.u()) * t),
                            (float) (previous.v() + (current.v() - previous.v()) * t)));
                }
            }
            if (currentInside) output.add(current);
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean inside(Sample sample, int coordinate,
            float bound, boolean keepGreater) {
        float value = coordinate == 0 ? sample.u() : sample.v();
        return keepGreater ? value >= bound : value <= bound;
    }

    private static void vertex(VertexConsumer consumer, PoseStack pose,
            Sample sample, Vec3 normal, int alpha) {
        consumer.vertex(pose.last().pose(), (float) sample.world().x,
                        (float) sample.world().y, (float) sample.world().z)
                .color(255, 255, 255, alpha)
                .uv(sample.u(), sample.v())
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.last().normal(),
                        (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private record Sample(Vec3 world, float u, float v) {
    }

    private static final class Projector {
        private final Vec3 lamp;
        private final Vec3 axisX;
        private final Vec3 axisY;

        private Projector(Vec3 lamp, Vec3 right, Vec3 up, double phase) {
            this.lamp = lamp;
            double angle = -phase * Math.PI * 2.0D;
            this.axisX = right.scale(Math.cos(angle))
                    .add(up.scale(Math.sin(angle)));
            this.axisY = up.scale(Math.cos(angle))
                    .subtract(right.scale(Math.sin(angle)));
        }

        private Sample sample(Vec3 world) {
            Vec3 delta = world.subtract(lamp);
            return new Sample(world,
                    (float) (0.5D + delta.dot(axisX) / (2.0D * HALF_WIDTH)),
                    (float) (0.5D - delta.dot(axisY) / (2.0D * HALF_HEIGHT)));
        }
    }
}
