package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders authored floor patches and the flat selection being added. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityMappingSelectionRenderer {
    private static final double MAX_RENDER_DISTANCE_SQR = 128.0D * 128.0D;

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

        for (FacilityRoomSnapshot room : FacilityMappingClientState.rooms(
                minecraft.level.dimension().location())) {
            for (FacilityFloorPatch patch : room.patches()) {
                double centerX = (patch.minX() + patch.maxX() + 1.0D) * 0.5D;
                double centerZ = (patch.minZ() + patch.maxZ() + 1.0D) * 0.5D;
                double dx = centerX - minecraft.player.getX();
                double dz = centerZ - minecraft.player.getZ();
                if (dx * dx + dz * dz > MAX_RENDER_DISTANCE_SQR) continue;
                LevelRenderer.renderLineBox(poseStack, lines,
                        bounds(patch).inflate(0.005D),
                        0.18F, 0.82F, 1.0F, 0.78F);
            }
        }

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
}
