package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * World-space rendering for off-grid cells and curved surface payloads. Static
 * surface models are deformed vertex-by-vertex; rigid attachments keep their
 * shape and inherit the local surface frame.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformConstructionClientRenderer {
    private static final double MAX_RENDER_DISTANCE_SQR = 192.0D * 192.0D;
    private static final Direction[] SIDES = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null
    };

    private TransformConstructionClientRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        var dimension = minecraft.level.dimension().location();
        var groups = TransformConstructionClientState.groups(dimension);
        var surfaces = TransformConstructionClientState.surfaces(dimension);
        if (groups.isEmpty() && surfaces.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (TransformGroup group : groups) {
            renderGroup(minecraft, pose, buffers, group, camera);
        }
        for (ConstructionSurface surface : surfaces) {
            renderSurfacePayloads(minecraft, pose, buffers, surface, camera);
        }

        boolean offGridTool = minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool());
        boolean surfaceTool = minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getSurfaceTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
        if (offGridTool || surfaceTool) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            if (offGridTool) {
                for (TransformGroup group : groups) {
                    renderGroupGrid(pose, lines, group, camera);
                }
            }
            if (surfaceTool) {
                for (ConstructionSurface surface : surfaces) {
                    renderSurfaceGrid(pose, lines, surface, camera);
                }
            }
            buffers.endBatch(RenderType.lines());
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static void renderGroup(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, TransformGroup group,
            Vec3 camera) {
        if (group.origin().distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) return;
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            BlockState state = entry.getValue();
            if (state == null || state.isAir()
                    || state.getRenderShape() == RenderShape.INVISIBLE) continue;
            TransformGroup.GridPos cell = entry.getKey();
            Vec3 center = group.cellCenter(cell);
            if (center.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) continue;
            BlockPos lightPos = BlockPos.containing(center);
            int light = LevelRenderer.getLightColor(minecraft.level, state,
                    lightPos);
            pose.pushPose();
            pose.translate(group.origin().x, group.origin().y, group.origin().z);
            pose.mulPose(TransformMath.quaternion(group.rotationX(),
                    group.rotationY(), group.rotationZ()));
            pose.translate(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D);
            minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                    light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    private static void renderSurfacePayloads(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, ConstructionSurface surface,
            Vec3 camera) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        if (center.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutoutMipped());
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            BlockState state = entry.getValue().state();
            if (state == null || state.isAir()
                    || state.getRenderShape() == RenderShape.INVISIBLE) continue;
            renderSurfaceBlock(minecraft, pose, consumer, surface,
                    entry.getKey(), entry.getValue());
        }
    }

    private static void renderSurfaceBlock(Minecraft minecraft, PoseStack pose,
            VertexConsumer consumer, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment) {
        BlockState state = attachment.state();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create(42L);
        BlockPos lightPos = BlockPos.containing(surface.gridPoint(
                (slot.column() + 0.5D) / surface.columns(),
                (slot.row() + 0.5D) / surface.rows()));
        int packedLight = LevelRenderer.getLightColor(minecraft.level, state,
                lightPos);
        for (Direction side : SIDES) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, side, random,
                    ModelData.EMPTY, null)) {
                emitQuad(minecraft, pose, consumer, surface, slot, attachment,
                        quad, lightPos, packedLight);
            }
        }
    }

    private static void emitQuad(Minecraft minecraft, PoseStack pose,
            VertexConsumer consumer, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, BakedQuad quad,
            BlockPos lightPos, int fallbackLight) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int tint = quad.isTinted() ? minecraft.getBlockColors().getColor(
                attachment.state(), minecraft.level, lightPos,
                quad.getTintIndex()) : 0xFFFFFF;
        int red = tint < 0 ? 255 : (tint >> 16) & 0xFF;
        int green = tint < 0 ? 255 : (tint >> 8) & 0xFF;
        int blue = tint < 0 ? 255 : tint & 0xFF;
        Vec3 quadNormal = new Vec3(quad.getDirection().getStepX(),
                quad.getDirection().getStepY(), quad.getDirection().getStepZ());

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float u = stride > 4 ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
            float v = stride > 5 ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
            VertexFrame frame = attachment.deform()
                    ? deformedFrame(surface, slot, x, y, z, quadNormal)
                    : rigidFrame(surface, slot, x, y, z, quadNormal);
            BlockPos sample = BlockPos.containing(frame.position());
            int light = minecraft.level.hasChunkAt(sample)
                    ? LevelRenderer.getLightColor(minecraft.level,
                            attachment.state(), sample) : fallbackLight;
            consumer.vertex(pose.last().pose(), (float) frame.position().x,
                            (float) frame.position().y,
                            (float) frame.position().z)
                    .color(red, green, blue, 255)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(pose.last().normal(), (float) frame.normal().x,
                            (float) frame.normal().y,
                            (float) frame.normal().z)
                    .endVertex();
        }
    }

    private static VertexFrame deformedFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal) {
        double u = (slot.column() + x) / surface.columns();
        double v = (slot.row() + y) / surface.rows();
        Vec3 tangent = surface.gridTangent(u, v);
        Vec3 normal = surface.gridNormal(u, v);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u));
        Vec3 position = surface.gridPoint(u, v).add(normal.scale(z - 0.5D));
        Vec3 transformedNormal = TransformMath.safeNormalize(
                tangent.scale(localNormal.x)
                        .add(vertical.scale(localNormal.y))
                        .add(normal.scale(localNormal.z)), normal);
        return new VertexFrame(position, transformedNormal);
    }

    private static VertexFrame rigidFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = surface.gridTangent(u, v);
        Vec3 normal = surface.gridNormal(u, v);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u));
        Vec3 position = surface.gridPoint(u, v)
                .add(tangent.scale(x - 0.5D))
                .add(vertical.scale(y - 0.5D))
                .add(normal.scale(z - 0.5D));
        Vec3 transformedNormal = TransformMath.safeNormalize(
                tangent.scale(localNormal.x)
                        .add(vertical.scale(localNormal.y))
                        .add(normal.scale(localNormal.z)), normal);
        return new VertexFrame(position, transformedNormal);
    }

    private static void renderGroupGrid(PoseStack pose, VertexConsumer lines,
            TransformGroup group, Vec3 camera) {
        if (group.origin().distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) return;
        Selection selected = TransformConstructionClientState.selection();
        boolean active = selected != null && selected.type() == SelectionType.GROUP
                && group.id().equals(selected.id());
        float r = active ? 1.0F : 0.12F;
        float g = active ? 0.82F : 1.0F;
        float b = active ? 0.12F : 0.28F;
        for (TransformGroup.GridPos cell : group.cells().keySet()) {
            Vec3[] corners = new Vec3[8];
            int index = 0;
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    for (int xi = 0; xi < 2; xi++) {
                        Vec3 local = new Vec3(cell.x() + (xi - 0.5D),
                                cell.y() + (yi - 0.5D),
                                cell.z() + (zi - 0.5D));
                        corners[index++] = TransformMath.localToWorld(
                                group.origin(), local, group.rotationX(),
                                group.rotationY(), group.rotationZ());
                    }
                }
            }
            int[][] edges = {
                    {0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},
                    {0,4},{1,5},{2,6},{3,7}
            };
            for (int[] edge : edges) {
                line(pose, lines, corners[edge[0]], corners[edge[1]],
                        r, g, b, 0.95F);
            }
        }
    }

    private static void renderSurfaceGrid(PoseStack pose, VertexConsumer lines,
            ConstructionSurface surface, Vec3 camera) {
        if (surface.gridPoint(0.5D, 0.5D).distanceToSqr(camera)
                > MAX_RENDER_DISTANCE_SQR) return;
        Selection selected = TransformConstructionClientState.selection();
        boolean active = selected != null
                && selected.type() == SelectionType.SURFACE
                && surface.id().equals(selected.id());
        float red = active ? 1.0F : 0.12F;
        float green = active ? 0.82F : 1.0F;
        float blue = active ? 0.12F : 0.28F;
        int columns = surface.columns();
        int rows = surface.rows();
        for (int column = 0; column <= columns; column++) {
            double u = column / (double) columns;
            Vec3 previous = surface.gridPoint(u, 0.0D);
            int samples = Math.max(4, rows * 2);
            for (int sample = 1; sample <= samples; sample++) {
                double v = sample / (double) samples;
                Vec3 current = surface.gridPoint(u, v);
                line(pose, lines, previous, current, red, green, blue, 0.84F);
                previous = current;
            }
        }
        for (int row = 0; row <= rows; row++) {
            double v = row / (double) rows;
            Vec3 previous = surface.gridPoint(0.0D, v);
            int samples = Math.max(8, columns * 3);
            for (int sample = 1; sample <= samples; sample++) {
                double u = sample / (double) samples;
                Vec3 current = surface.gridPoint(u, v);
                line(pose, lines, previous, current, red, green, blue, 0.84F);
                previous = current;
            }
        }
        if (active) renderHandles(pose, lines, surface, selected.handle());
    }

    private static void renderHandles(PoseStack pose, VertexConsumer lines,
            ConstructionSurface surface, SurfaceHandle selected) {
        for (SurfaceHandle handle : SurfaceHandle.values()) {
            Vec3 point = switch (handle) {
                case BOTTOM_START -> surface.bottomStart();
                case BOTTOM_END -> surface.bottomEnd();
                case TOP_START -> surface.topStart();
                case TOP_END -> surface.topEnd();
                case CENTER -> surface.gridPoint(0.5D, 0.5D);
            };
            double size = handle == selected ? 0.13D : 0.09D;
            AABB box = new AABB(point.x - size, point.y - size,
                    point.z - size, point.x + size, point.y + size,
                    point.z + size);
            LevelRenderer.renderLineBox(pose, lines, box,
                    handle == selected ? 1.0F : 0.22F,
                    handle == selected ? 0.35F : 1.0F,
                    0.18F, 1.0F);
        }
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 from,
            Vec3 to, float red, float green, float blue, float alpha) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-9D) return;
        direction = direction.normalize();
        PoseStack.Pose current = pose.last();
        int r = Math.round(red * 255.0F);
        int g = Math.round(green * 255.0F);
        int b = Math.round(blue * 255.0F);
        int a = Math.round(alpha * 255.0F);
        lines.vertex(current.pose(), (float) from.x, (float) from.y,
                        (float) from.z)
                .color(r, g, b, a)
                .normal(current.normal(), (float) direction.x,
                        (float) direction.y, (float) direction.z).endVertex();
        lines.vertex(current.pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(r, g, b, a)
                .normal(current.normal(), (float) direction.x,
                        (float) direction.y, (float) direction.z).endVertex();
    }

    private record VertexFrame(Vec3 position, Vec3 normal) {
    }
}
