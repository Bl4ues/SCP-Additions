package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Client half of the reusable obstructed-structure placement overlay. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StructurePlacementFeedbackClient {
    private static final long DISPLAY_TICKS = 60L;
    private static List<BlockPos> blockers = List.of();
    private static ResourceKey<Level> dimension;
    private static long expiresAtGameTime;

    private StructurePlacementFeedbackClient() {
    }

    public static void show(List<BlockPos> positions, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || positions == null || positions.isEmpty()) return;

        blockers = positions.stream().map(BlockPos::immutable).distinct().toList();
        dimension = minecraft.level.dimension();
        expiresAtGameTime = minecraft.level.getGameTime() + DISPLAY_TICKS;
        minecraft.player.displayClientMessage(message, true);
    }

    @SubscribeEvent
    public static void renderBlockedCells(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS
                || blockers.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || dimension == null
                || !minecraft.level.dimension().equals(dimension)
                || minecraft.level.getGameTime() > expiresAtGameTime) {
            clear();
            return;
        }

        List<BlockPos> active = blockers.stream()
                .filter(pos -> minecraft.level.getWorldBorder().isWithinBounds(pos)
                        && !minecraft.level.getBlockState(pos).canBeReplaced())
                .toList();
        if (active.isEmpty()) {
            clear();
            return;
        }
        blockers = active;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : active) {
            AABB box = new AABB(pos).inflate(0.006D);
            LevelRenderer.renderLineBox(poseStack, lines, box,
                    1.0F, 0.18F, 0.08F, 0.95F);
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void clear() {
        blockers = List.of();
        dimension = null;
        expiresAtGameTime = 0L;
    }
}
