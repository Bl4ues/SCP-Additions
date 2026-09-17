package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Live world-space feedback for the three-click surface authoring workflow. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformSurfaceAuthoringPreviewRenderer {
    private TransformSurfaceAuthoringPreviewRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.isCreative()
                || !holdingSurfaceTool(minecraft)
                || TransformSurfaceAuthoringState.start() == null) return;

        Vec3 start = TransformSurfaceAuthoringState.start();
        Vec3 end = TransformSurfaceAuthoringState.end();
        Vec3 hit = hit(minecraft);
        if (hit == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        if (end == null) {
            Vec3 candidate = new Vec3(hit.x, start.y, hit.z);
            boolean valid = candidate.distanceToSqr(start) >= 0.04D;
            marker(pose, lines, start, 0.12D, 0.20F, 1.0F, 0.35F);
            marker(pose, lines, candidate, 0.10D,
                    valid ? 0.20F : 1.0F,
                    valid ? 1.0F : 0.18F, 0.26F);
            line(pose, lines, start, candidate,
                    valid ? 0.20F : 1.0F,
                    valid ? 1.0F : 0.18F, 0.30F, 1.0F);
        } else {
            double height = hit.y - start.y;
            Vec3 topStart = start.add(0.0D, height, 0.0D);
            Vec3 topEnd = end.add(0.0D, height, 0.0D);
            boolean valid = Math.abs(height) >= 0.125D;
            float red = valid ? 0.20F : 1.0F;
            float green = valid ? 1.0F : 0.18F;
            float blue = valid ? 0.34F : 0.20F;
            line(pose, lines, start, end, red, green, blue, 1.0F);
            line(pose, lines, topStart, topEnd, red, green, blue, 1.0F);
            line(pose, lines, start, topStart, red, green, blue, 1.0F);
            line(pose, lines, end, topEnd, red, green, blue, 1.0F);
            marker(pose, lines, start, 0.09D, red, green, blue);
            marker(pose, lines, end, 0.09D, red, green, blue);
            marker(pose, lines, topStart, 0.09D, red, green, blue);
            marker(pose, lines, topEnd, 0.09D, red, green, blue);

            int widthGuides = Math.min(16, Math.max(1,
                    (int) Math.round(start.distanceTo(end))));
            int heightGuides = Math.min(12, Math.max(1,
                    (int) Math.round(Math.abs(height))));
            for (int index = 1; index < widthGuides; index++) {
                double t = index / (double) widthGuides;
                Vec3 bottom = start.lerp(end, t);
                Vec3 top = topStart.lerp(topEnd, t);
                line(pose, lines, bottom, top, red, green, blue, 0.42F);
            }
            for (int index = 1; index < heightGuides; index++) {
                double t = index / (double) heightGuides;
                Vec3 a = start.lerp(topStart, t);
                Vec3 b = end.lerp(topEnd, t);
                line(pose, lines, a, b, red, green, blue, 0.42F);
            }
        }

        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static Vec3 hit(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        return hit instanceof BlockHitResult blockHit
                && hit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation() : null;
    }

    private static boolean holdingSurfaceTool(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getSurfaceTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
    }

    private static void marker(PoseStack pose, VertexConsumer lines, Vec3 point,
            double size, float red, float green, float blue) {
        line(pose, lines, point.add(-size, 0.0D, 0.0D),
                point.add(size, 0.0D, 0.0D), red, green, blue, 1.0F);
        line(pose, lines, point.add(0.0D, -size, 0.0D),
                point.add(0.0D, size, 0.0D), red, green, blue, 1.0F);
        line(pose, lines, point.add(0.0D, 0.0D, -size),
                point.add(0.0D, 0.0D, size), red, green, blue, 1.0F);
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 from,
            Vec3 to, float red, float green, float blue, float alpha) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-9D) return;
        direction = direction.normalize();
        PoseStack.Pose current = pose.last();
        lines.vertex(current.pose(), (float) from.x, (float) from.y,
                        (float) from.z)
                .color(red, green, blue, alpha)
                .normal(current.normal(), (float) direction.x,
                        (float) direction.y, (float) direction.z).endVertex();
        lines.vertex(current.pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(red, green, blue, alpha)
                .normal(current.normal(), (float) direction.x,
                        (float) direction.y, (float) direction.z).endVertex();
    }
}
