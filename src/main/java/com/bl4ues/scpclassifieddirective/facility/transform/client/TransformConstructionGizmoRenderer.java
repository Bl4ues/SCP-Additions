package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Axis;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.EditMode;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Axiom/Blockbench-like world-space transform axes for the selected object. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformConstructionGizmoRenderer {
    private TransformConstructionGizmoRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (!minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                && !minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getSurfaceTool())) return;
        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        Vec3 origin = origin(selection);
        if (origin == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        if (TransformConstructionClientState.mode() == EditMode.ROTATE
                && selection.type() == SelectionType.GROUP) {
            drawRotationRings(pose, lines, origin);
        } else {
            drawAxes(pose, lines, origin);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void drawAxes(PoseStack pose, VertexConsumer lines,
            Vec3 origin) {
        drawAxis(pose, lines, origin, new Vec3(1.05D, 0.0D, 0.0D),
                Axis.X, 1.0F, 0.20F, 0.18F);
        drawAxis(pose, lines, origin, new Vec3(0.0D, 1.05D, 0.0D),
                Axis.Y, 0.25F, 1.0F, 0.28F);
        drawAxis(pose, lines, origin, new Vec3(0.0D, 0.0D, 1.05D),
                Axis.Z, 0.20F, 0.48F, 1.0F);
    }

    private static void drawAxis(PoseStack pose, VertexConsumer lines,
            Vec3 origin, Vec3 direction, Axis axis, float red, float green,
            float blue) {
        boolean selected = TransformConstructionClientState.axis() == axis;
        float alpha = selected ? 1.0F : 0.78F;
        Vec3 end = origin.add(direction);
        line(pose, lines, origin, end, red, green, blue, alpha);
        Vec3 unit = direction.normalize();
        Vec3 sideA = Math.abs(unit.y) > 0.8D
                ? new Vec3(0.12D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.12D, 0.0D);
        Vec3 sideB = unit.cross(sideA).normalize().scale(0.12D);
        Vec3 back = end.subtract(unit.scale(selected ? 0.23D : 0.18D));
        line(pose, lines, end, back.add(sideA), red, green, blue, alpha);
        line(pose, lines, end, back.subtract(sideA), red, green, blue, alpha);
        line(pose, lines, end, back.add(sideB), red, green, blue, alpha);
        line(pose, lines, end, back.subtract(sideB), red, green, blue, alpha);
    }

    private static void drawRotationRings(PoseStack pose, VertexConsumer lines,
            Vec3 origin) {
        ring(pose, lines, origin, Axis.X, 1.0F, 0.20F, 0.18F);
        ring(pose, lines, origin, Axis.Y, 0.25F, 1.0F, 0.28F);
        ring(pose, lines, origin, Axis.Z, 0.20F, 0.48F, 1.0F);
    }

    private static void ring(PoseStack pose, VertexConsumer lines, Vec3 origin,
            Axis axis, float red, float green, float blue) {
        boolean selected = TransformConstructionClientState.axis() == axis;
        double radius = selected ? 0.72D : 0.64D;
        int segments = 48;
        Vec3 previous = ringPoint(origin, axis, radius, 0.0D);
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            Vec3 current = ringPoint(origin, axis, radius, angle);
            line(pose, lines, previous, current, red, green, blue,
                    selected ? 1.0F : 0.72F);
            previous = current;
        }
    }

    private static Vec3 ringPoint(Vec3 origin, Axis axis, double radius,
            double angle) {
        double a = Math.cos(angle) * radius;
        double b = Math.sin(angle) * radius;
        return switch (axis) {
            case X -> origin.add(0.0D, a, b);
            case Y -> origin.add(a, 0.0D, b);
            case Z -> origin.add(a, b, 0.0D);
        };
    }

    private static Vec3 origin(Selection selection) {
        if (selection.type() == SelectionType.GROUP) {
            TransformGroup group = TransformConstructionClientState.group(
                    selection.id());
            return group == null ? null : group.origin();
        }
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        if (surface == null) return null;
        SurfaceHandle handle = selection.handle();
        return switch (handle) {
            case BOTTOM_START -> surface.bottomStart();
            case BOTTOM_END -> surface.bottomEnd();
            case TOP_START -> surface.topStart();
            case TOP_END -> surface.topEnd();
            case CENTER -> surface.gridPoint(0.5D, 0.5D);
        };
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 a,
            Vec3 b, float red, float green, float blue, float alpha) {
        Vec3 normal = b.subtract(a);
        if (normal.lengthSqr() < 1.0E-9D) return;
        normal = normal.normalize();
        PoseStack.Pose current = pose.last();
        lines.vertex(current.pose(), (float) a.x, (float) a.y, (float) a.z)
                .color(red, green, blue, alpha)
                .normal(current.normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z).endVertex();
        lines.vertex(current.pose(), (float) b.x, (float) b.y, (float) b.z)
                .color(red, green, blue, alpha)
                .normal(current.normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z).endVertex();
    }
}
