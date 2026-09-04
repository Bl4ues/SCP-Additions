package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneTrack;
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

import java.util.ArrayList;
import java.util.List;

/** White selection volume with automatic soundtrack sources shown in green. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SafeZoneSelectionRenderer {
    private static final double COMPLETED_ZONE_RENDER_DISTANCE = 96.0D;
    private static final int MAX_COMPLETED_ZONES_RENDERED = 32;
    private static BlockPos cachedStart;
    private static BlockPos cachedEnd;
    private static List<BlockPos> cachedSources = List.of();

    private SafeZoneSelectionRenderer() {
    }

    @SubscribeEvent
    public static void renderSelection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos start = SafeZoneClientState.selectionStart();
        if (minecraft.level == null || minecraft.player == null
                || !isHoldingTool(minecraft)) {
            clearCache();
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (SafeZone zone : SafeZoneClientState.nearbyZones(
                minecraft.level.dimension().location(),
                minecraft.player.position(), COMPLETED_ZONE_RENDER_DISTANCE,
                MAX_COMPLETED_ZONES_RENDERED)) {
            LevelRenderer.renderLineBox(poseStack, lines,
                    zone.bounds().inflate(0.009D),
                    1.0F, 1.0F, 1.0F, 0.72F);
        }

        if (start != null) {
            renderActiveSelection(minecraft, poseStack, lines, start);
        } else {
            clearCache();
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderActiveSelection(Minecraft minecraft,
            PoseStack poseStack, VertexConsumer lines, BlockPos start) {
        BlockPos end = start;
        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult blockHit
                && hit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getBlockPos();
        }
        BlockPos min = minimum(start, end);
        BlockPos max = maximum(start, end);
        long volume = volume(min, max);

        if (!start.equals(cachedStart) || !end.equals(cachedEnd)) {
            cachedStart = start.immutable();
            cachedEnd = end.immutable();
            cachedSources = volume <= SafeZoneManager.MAX_SELECTION_VOLUME
                    ? findSources(minecraft, min, max) : List.of();
        }

        AABB selection = new AABB(min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0D, max.getY() + 1.0D,
                max.getZ() + 1.0D).inflate(0.006D);
        float green = volume <= SafeZoneManager.MAX_SELECTION_VOLUME
                ? 1.0F : 0.18F;
        float blue = volume <= SafeZoneManager.MAX_SELECTION_VOLUME
                ? 1.0F : 0.08F;
        LevelRenderer.renderLineBox(poseStack, lines, selection,
                1.0F, green, blue, 0.95F);
        for (BlockPos source : cachedSources) {
            LevelRenderer.renderLineBox(poseStack, lines,
                    new AABB(source).inflate(0.012D),
                    0.15F, 1.0F, 0.28F, 1.0F);
        }
    }

    private static boolean isHoldingTool(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(
                ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get())
                || minecraft.player.getOffhandItem().is(
                        ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get());
    }

    private static List<BlockPos> findSources(Minecraft minecraft,
            BlockPos min, BlockPos max) {
        List<BlockPos> sources = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            if (SafeZoneTrack.automaticTrackFor(
                    minecraft.level.getBlockState(cursor)) != null) {
                sources.add(cursor.immutable());
                if (sources.size() >= 256) break;
            }
        }
        return List.copyOf(sources);
    }

    private static BlockPos minimum(BlockPos first, BlockPos second) {
        return new BlockPos(Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
    }

    private static BlockPos maximum(BlockPos first, BlockPos second) {
        return new BlockPos(Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
    }

    private static long volume(BlockPos min, BlockPos max) {
        return ((long) max.getX() - min.getX() + 1L)
                * ((long) max.getY() - min.getY() + 1L)
                * ((long) max.getZ() - min.getZ() + 1L);
    }

    private static void clearCache() {
        cachedStart = null;
        cachedEnd = null;
        cachedSources = List.of();
    }
}
