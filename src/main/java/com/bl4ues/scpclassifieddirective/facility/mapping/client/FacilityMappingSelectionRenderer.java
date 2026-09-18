package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityCameraMappingSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityRoomOutlineGeometry.Layer;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Renders authored floor patches and the flat selection being added. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityMappingSelectionRenderer {
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0D * 128.0D;
    private static final Map<UUID, CachedRoomGeometry> ROOM_GEOMETRY =
            new HashMap<>();

    private FacilityMappingSelectionRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !holdingTool(minecraft)) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        var rooms = FacilityMappingClientState.rooms(
                minecraft.level.dimension().location());
        Set<UUID> liveRooms = new HashSet<>();
        for (FacilityRoomSnapshot room : rooms) {
            liveRooms.add(room.id());
            CachedRoomGeometry cached = ROOM_GEOMETRY.get(room.id());
            if (cached == null || !cached.room().equals(room)) {
                cached = new CachedRoomGeometry(room,
                        FacilityRoomOutlineGeometry.of(room));
                ROOM_GEOMETRY.put(room.id(), cached);
            }
            renderRoomOutline(minecraft, poseStack, lines,
                    cached.geometry());
        }
        ROOM_GEOMETRY.keySet().removeIf(id -> !liveRooms.contains(id));

        renderCameraAssociations(minecraft, poseStack, lines);

        BlockPos start = FacilityMappingClientState.selectionStart();
        if (start != null) {
            BlockPos end = start;
            HitResult hit = minecraft.hitResult;
            if (hit instanceof BlockHitResult blockHit
                    && hit.getType() == HitResult.Type.BLOCK) {
                end = blockHit.getBlockPos();
            }
            FacilityFloorPatch patch = FacilityFloorPatch.between(start, end);
            int xSpan = patch.maxX() - patch.minX() + 1;
            int zSpan = patch.maxZ() - patch.minZ() + 1;
            boolean valid = patch.area() <= FacilityMappingManager.MAX_PATCH_AREA
                    && xSpan <= FacilityMappingManager.MAX_PATCH_SPAN
                    && zSpan <= FacilityMappingManager.MAX_PATCH_SPAN;
            LevelRenderer.renderLineBox(poseStack, lines,
                    bounds(patch).inflate(0.011D),
                    1.0F, valid ? 1.0F : 0.12F,
                    valid ? 1.0F : 0.08F, 0.96F);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderRoomOutline(Minecraft minecraft,
            PoseStack poseStack, VertexConsumer lines,
            FacilityRoomOutlineGeometry geometry) {
        for (Layer layer : geometry.layers()) {
            if (layer.empty()) continue;
            Rectangle2D bounds = layer.bounds();
            double centerX = bounds.getCenterX();
            double centerZ = bounds.getCenterY();
            double dx = centerX - minecraft.player.getX();
            double dz = centerZ - minecraft.player.getZ();
            double radius = Math.hypot(bounds.getWidth(),
                    bounds.getHeight()) * 0.5D;
            double maximum = 128.0D + radius;
            if (dx * dx + dz * dz > maximum * maximum) continue;

            double y = layer.y() + 1.015D;
            for (var contour : layer.contours()) {
                for (int index = 0; index < contour.size(); index++) {
                    FacilityFloorPatch.Vertex a = contour.get(index);
                    FacilityFloorPatch.Vertex b = contour.get(
                            (index + 1) % contour.size());
                    renderLine(poseStack, lines,
                            new Vec3(a.x(), y, a.z()),
                            new Vec3(b.x(), y, b.z()),
                            0.18F, 0.82F, 1.0F, 0.90F);
                }
            }
        }
    }

    private static void renderCameraAssociations(Minecraft minecraft,
            PoseStack poseStack, VertexConsumer lines) {
        var dimension = minecraft.level.dimension().location();
        java.util.UUID selected =
                FacilityMappingClientState.cameraLinkSelection();
        for (FacilityCameraMappingSnapshot camera
                : FacilityMappingClientState.cameras(dimension)) {
            BlockPos pos = camera.anchorPos();
            double dx = pos.getX() + 0.5D - minecraft.player.getX();
            double dy = pos.getY() + 0.5D - minecraft.player.getY();
            double dz = pos.getZ() + 0.5D - minecraft.player.getZ();
            if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQR
                    || !minecraft.level.hasChunkAt(pos)) continue;

            BlockState state = minecraft.level.getBlockState(pos);
            VoxelShape shape = state.getShape(minecraft.level, pos);
            AABB cameraBox = shape.isEmpty()
                    ? new AABB(pos).deflate(0.22D)
                    : shape.bounds().move(pos).inflate(0.035D);
            boolean active = selected != null
                    && selected.equals(camera.cameraId());
            float red = active ? 1.0F : camera.detached() ? 1.0F : 0.20F;
            float green = active ? 0.86F : camera.detached() ? 0.34F : 0.95F;
            float blue = active ? 0.18F : camera.detached() ? 0.16F : 0.36F;
            LevelRenderer.renderLineBox(poseStack, lines, cameraBox,
                    red, green, blue, 0.96F);

            if (!camera.associated()) continue;
            FacilityRoomSnapshot room = FacilityMappingClientState.roomById(
                    dimension, camera.roomId());
            Vec3 target = nearestRoomAnchor(room, Vec3.atCenterOf(pos));
            if (target != null) {
                renderLine(poseStack, lines, Vec3.atCenterOf(pos), target,
                        0.18F, 1.0F, 0.34F, 0.90F);
            }
        }
    }

    private static Vec3 nearestRoomAnchor(FacilityRoomSnapshot room,
            Vec3 camera) {
        if (room == null || camera == null) return null;
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (FacilityFloorPatch patch : room.patches()) {
            Vec3 candidate = patchCenter(patch).add(0.0D, 0.025D, 0.0D);
            double distance = candidate.distanceToSqr(camera);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static Vec3 patchCenter(FacilityFloorPatch patch) {
        if (patch.isPolygon()) {
            double x = 0.0D;
            double z = 0.0D;
            for (FacilityFloorPatch.Vertex vertex : patch.vertices()) {
                x += vertex.x();
                z += vertex.z();
            }
            double count = patch.vertices().size();
            return new Vec3(x / count, patch.y() + 1.0D, z / count);
        }
        return new Vec3((patch.minX() + patch.maxX() + 1.0D) * 0.5D,
                patch.y() + 1.0D,
                (patch.minZ() + patch.maxZ() + 1.0D) * 0.5D);
    }

    private static void renderLine(PoseStack poseStack, VertexConsumer lines,
            Vec3 from, Vec3 to, float red, float green, float blue,
            float alpha) {
        Vec3 normal = to.subtract(from);
        if (normal.lengthSqr() < 1.0E-8D) return;
        normal = normal.normalize();
        PoseStack.Pose pose = poseStack.last();
        int r = Math.round(red * 255.0F);
        int g = Math.round(green * 255.0F);
        int b = Math.round(blue * 255.0F);
        int a = Math.round(alpha * 255.0F);
        lines.vertex(pose.pose(), (float) from.x, (float) from.y,
                        (float) from.z)
                .color(r, g, b, a)
                .normal(pose.normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z)
                .endVertex();
        lines.vertex(pose.pose(), (float) to.x, (float) to.y,
                        (float) to.z)
                .color(r, g, b, a)
                .normal(pose.normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static AABB bounds(FacilityFloorPatch patch) {
        return new AABB(patch.minX(), patch.y() + 0.985D, patch.minZ(),
                patch.maxX() + 1.0D, patch.y() + 1.015D,
                patch.maxZ() + 1.0D);
    }

    private static boolean holdingTool(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(FacilityMappingItems.getTool())
                || minecraft.player.getOffhandItem().is(
                FacilityMappingItems.getTool());
    }
    private record CachedRoomGeometry(FacilityRoomSnapshot room,
            FacilityRoomOutlineGeometry geometry) {
    }

}
