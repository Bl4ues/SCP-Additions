package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient.Phase;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry.Attachment;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.hacking.HackingDevicePuzzle;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Set;

/** Renders the device physically seated on readers and its character-only CRT. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class HackingDeviceAttachedRenderer {
    private static final float LOGICAL_WIDTH = 256.0F;
    private static final float LOGICAL_HEIGHT = 154.0F;
    private static final float SCREEN_SCALE = (float)
            (HackingDeviceAttachmentGeometry.SCREEN_WIDTH / LOGICAL_WIDTH);
    private static final int GREEN = 0xFF49F06F;
    private static final int GREEN_BRIGHT = 0xFF78FF94;
    private static final int GREEN_DIM = 0xFF238A42;
    private static final double SCREEN_EPSILON = 0.0015D;
    private static final ResourceLocation SCREEN_MASK = new ResourceLocation(
            "minecraft", "textures/block/white_concrete.png");

    private HackingDeviceAttachedRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        Set<BlockPos> visibleDevices = HackingDeviceClientState.snapshot();

        for (BlockPos pos : visibleDevices) {
            if (!minecraft.level.hasChunkAt(pos)
                    || camera.distanceToSqr(Vec3.atCenterOf(pos)) > 4096.0D) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(pos);
            Attachment attachment = HackingDeviceAttachmentGeometry.resolve(pos, state);
            if (attachment == null) continue;
            renderDevice(minecraft, poseStack, buffers, camera, pos,
                    attachment);
        }
        buffers.endBatch();

        for (BlockPos pos : visibleDevices) {
            if (!minecraft.level.hasChunkAt(pos)
                    || camera.distanceToSqr(Vec3.atCenterOf(pos)) > 4096.0D) {
                continue;
            }
            Attachment attachment = HackingDeviceAttachmentGeometry.resolve(pos,
                    minecraft.level.getBlockState(pos));
            if (attachment != null) {
                renderScreen(minecraft, poseStack, buffers, camera, pos,
                        attachment);
            }
        }
        buffers.endBatch();
    }

    private static void renderDevice(Minecraft minecraft, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, Vec3 camera, BlockPos pos,
            Attachment attachment) {
        double seating = HackingDeviceClientState.seatingOffset(pos);
        Vec3 origin = attachment.modelOrigin()
                .add(attachment.screen().outward().scale(seating))
                .subtract(camera);
        poseStack.pushPose();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                HackingDeviceAttachmentGeometry.modelYaw(attachment.facing())));
        int light = LevelRenderer.getLightColor(minecraft.level, pos);
        ItemStack deviceStack = new ItemStack(
                ScpClassifiedDirectiveModItems.HACKING_DEVICE.get());
        minecraft.getItemRenderer().renderStatic(deviceStack,
                ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, minecraft.level, 0);
        poseStack.popPose();
    }

    private static void renderScreen(Minecraft minecraft, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, Vec3 camera, BlockPos pos,
            Attachment attachment) {
        double seating = HackingDeviceClientState.seatingOffset(pos);
        Frame original = attachment.screen();
        Vec3 animatedCenter = original.center()
                .add(original.outward().scale(seating));
        Frame frame = new Frame(animatedCenter, original.right(), original.up(),
                original.outward(), original.width(), original.height());
        RenderType panelType = RenderType.entityCutoutNoCull(SCREEN_MASK);
        VertexConsumer panel = buffers.getBuffer(panelType);
        emitBlackPanel(panel, poseStack.last(), frame, camera);
        buffers.endBatch(panelType);

        Vec3 center = frame.center().subtract(camera)
                .add(frame.outward().scale(SCREEN_EPSILON * 2.0D));
        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                HackingDeviceAttachmentGeometry.modelYaw(attachment.facing())
                        + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        poseStack.scale(-SCREEN_SCALE, -SCREEN_SCALE, SCREEN_SCALE);
        poseStack.translate(-LOGICAL_WIDTH * 0.5F,
                -LOGICAL_HEIGHT * 0.5F, 0.0F);

        if (HackingDeviceMinigameClient.active()
                && pos.equals(HackingDeviceMinigameClient.pos())) {
            renderSession(minecraft.font, poseStack, buffers);
        } else {
            draw(minecraft.font, poseStack, buffers,
                    "CI FIELD UNIT // STANDBY", 10.0F, 68.0F, GREEN_DIM);
        }
        poseStack.popPose();
    }

    private static void renderSession(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        Phase phase = HackingDeviceMinigameClient.phase();
        switch (phase) {
            case LOADING -> renderLoading(font, poseStack, buffers);
            case BOOT -> renderBoot(font, poseStack, buffers);
            case PUZZLE -> renderPuzzle(font, poseStack, buffers);
            case ROUND_OK -> renderRoundOk(font, poseStack, buffers);
            case DENIED -> renderDenied(font, poseStack, buffers);
            case LOCKED -> renderLocked(font, poseStack, buffers);
            case SUCCESS -> renderSuccess(font, poseStack, buffers);
        }
    }

    private static void renderLoading(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        double progress = HackingDeviceMinigameClient.phaseProgress();
        int filled = (int) Math.round(progress * 24.0D);
        String bar = "[" + "#".repeat(Math.max(0, Math.min(24, filled)))
                + ".".repeat(Math.max(0, 24 - filled)) + "]";
        draw(font, poseStack, buffers, "CI FIELD NODE // HDEV", 10, 18,
                GREEN_DIM);
        draw(font, poseStack, buffers, "> PHY TAP: READER BUS", 10, 42,
                GREEN);
        draw(font, poseStack, buffers, bar, 10, 67, GREEN_BRIGHT);
        draw(font, poseStack, buffers,
                String.format("LINK %3d%%", (int) Math.round(progress * 100.0D)),
                10, 82, GREEN);
        if (progress > 0.35D) {
            draw(font, poseStack, buffers, "> CLOCK........SYNC", 10, 108,
                    GREEN_DIM);
        }
        if (progress > 0.70D) {
            draw(font, poseStack, buffers, "> BUS..........ONLINE", 10, 121,
                    GREEN_DIM);
        }
    }

    private static void renderBoot(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        int count = HackingDeviceMinigameClient.bootLineCount();
        draw(font, poseStack, buffers,
                String.format("TARGET: KCR-L%d", HackingDeviceMinigameClient.accessLevel()),
                10, 15, GREEN_DIM);
        String[] lines = {
                "> SNIFF AUTH BUS........OK",
                "> CAPTURE FRAME.........OK",
                "> CRC/XOR TABLE......LOADED",
                "> BREACH CHANNEL......READY"
        };
        for (int index = 0; index < count; index++) {
            draw(font, poseStack, buffers, lines[index], 10,
                    42 + index * 22, index == count - 1 ? GREEN_BRIGHT : GREEN);
        }
        if ((System.nanoTime() / 80_000_000L & 1L) == 0L) {
            draw(font, poseStack, buffers, "7A:4C:FF/03  0xA91E", 104, 132,
                    GREEN_DIM);
        }
    }

    private static void renderPuzzle(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        HackingDevicePuzzle puzzle = HackingDeviceMinigameClient.puzzle();
        if (puzzle == null) return;
        draw(font, poseStack, buffers,
                String.format("CRC BREACH %d/3   ERR %d/3",
                        HackingDeviceMinigameClient.round(),
                        HackingDeviceMinigameClient.failures()),
                8, 8, GREEN_DIM);
        draw(font, poseStack, buffers,
                "RULE: B0 ^ B1 ^ B2 ^ B3 = CRC", 8, 26, GREEN);

        int[] bytes = puzzle.bytes();
        StringBuilder frame = new StringBuilder("FRAME: ");
        for (int index = 0; index < 4; index++) {
            if (index > 0) frame.append(' ');
            frame.append(index == puzzle.missingIndex()
                    ? "??" : hex(bytes[index]));
        }
        draw(font, poseStack, buffers, frame.toString(), 8, 48,
                GREEN_BRIGHT);
        draw(font, poseStack, buffers,
                "TARGET CRC: " + hex(puzzle.checksum()), 8, 64, GREEN);

        int selected = HackingDeviceMinigameClient.selectedIndex();
        draw(font, poseStack, buffers,
                "REPAIR:  < [" + hex(puzzle.candidate(selected)) + "] >",
                8, 86, GREEN_BRIGHT);
        int probe = HackingDeviceMinigameClient.probeChecksum();
        String probeText = probe >= 0
                ? "PROBE CRC: " + hex(probe)
                        + (probe == puzzle.checksum() ? "  MATCH" : "  MISMATCH")
                : String.format("PROBES: %d/%d",
                        HackingDeviceMinigameClient.probesUsed(),
                        HackingDeviceMinigameClient.maxProbes());
        draw(font, poseStack, buffers, probeText, 8, 103,
                probe == puzzle.checksum() ? GREEN_BRIGHT : GREEN);
        draw(font, poseStack, buffers, "A/D SELECT   SPACE PROBE", 8, 128,
                GREEN_DIM);
        draw(font, poseStack, buffers,
                HackingDeviceMinigameClient.waitingForServer()
                        ? "ENTER INJECT   [WAIT]" : "ENTER INJECT",
                8, 140, GREEN_DIM);
    }

    private static void renderRoundOk(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        centered(font, poseStack, buffers, "CRC VALID", 51, GREEN_BRIGHT);
        centered(font, poseStack, buffers, "FRAME REPAIRED", 70, GREEN);
        centered(font, poseStack, buffers, "> ADVANCING BREACH...", 96,
                GREEN_DIM);
    }

    private static void renderDenied(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        centered(font, poseStack, buffers, "!! CRC MISMATCH !!", 46,
                GREEN_BRIGHT);
        centered(font, poseStack, buffers, "ACCESS DENIED", 67, GREEN);
        centered(font, poseStack, buffers,
                String.format("BREACH ERROR %d/3",
                        HackingDeviceMinigameClient.failures()),
                88, GREEN);
        centered(font, poseStack, buffers, "> RECALIBRATING...", 111,
                GREEN_DIM);
    }

    private static void renderLocked(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        centered(font, poseStack, buffers, "BREACH LIMIT REACHED", 50,
                GREEN_BRIGHT);
        centered(font, poseStack, buffers, "ACCESS DENIED", 72, GREEN);
        centered(font, poseStack, buffers, "> SESSION ABORT", 101,
                GREEN_DIM);
    }

    private static void renderSuccess(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        double progress = HackingDeviceMinigameClient.phaseProgress();
        centered(font, poseStack, buffers, "AUTH BYPASS COMMITTED", 9,
                GREEN_DIM);
        if (progress < 0.40D) {
            asciiLock(font, poseStack, buffers, false, false);
            centered(font, poseStack, buffers, "> RELEASING LOCK...", 128,
                    GREEN);
        } else if (progress < 0.58D) {
            asciiLock(font, poseStack, buffers, true, false);
            centered(font, poseStack, buffers, "> LATCH OVERRIDE...", 128,
                    GREEN_BRIGHT);
        } else {
            asciiLock(font, poseStack, buffers, true, true);
            centered(font, poseStack, buffers, "ACCESS GRANTED", 128,
                    GREEN_BRIGHT);
        }
    }

    private static void asciiLock(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, boolean opening,
            boolean open) {
        String[] closed = {
                "     .------.     ",
                "    /        \\    ",
                "    |        |    ",
                "  .------------.  ",
                "  |    [##]    |  ",
                "  |     ||     |  ",
                "  '------------'  "
        };
        String[] half = {
                "       .----.      ",
                "      /            ",
                "     /             ",
                "  .------------.  ",
                "  |    [##]    |  ",
                "  |     ||     |  ",
                "  '------------'  "
        };
        String[] opened = {
                "    .----.         ",
                "   /               ",
                "   |               ",
                "  .------------.  ",
                "  |    [  ]    |  ",
                "  |            |  ",
                "  '------------'  "
        };
        String[] lines = open ? opened : opening ? half : closed;
        for (int index = 0; index < lines.length; index++) {
            centered(font, poseStack, buffers, lines[index],
                    30 + index * 12, GREEN_BRIGHT);
        }
    }

    private static String hex(int value) {
        return String.format("%02X", value & 0xFF);
    }

    private static void centered(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float y,
            int color) {
        var sequence = ScpFonts.anonymousPro(text).getVisualOrderText();
        float x = (LOGICAL_WIDTH - font.width(sequence)) * 0.5F;
        font.drawInBatch(sequence, x, y, color, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
    }

    private static void draw(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float x,
            float y, int color) {
        var sequence = ScpFonts.anonymousPro(text).getVisualOrderText();
        font.drawInBatch(sequence, x, y, color, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
    }

    private static void emitBlackPanel(VertexConsumer consumer,
            PoseStack.Pose pose, Frame frame, Vec3 camera) {
        Vec3 offset = frame.outward().scale(SCREEN_EPSILON);
        Vec3 topLeft = frame.point(-0.5D, 0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 topRight = frame.point(0.5D, 0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 bottomRight = frame.point(0.5D, -0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 bottomLeft = frame.point(-0.5D, -0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 normalVector = frame.outward();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, topLeft, 0.0F, 0.0F, normalVector);
        vertex(consumer, matrix, normal, topRight, 1.0F, 0.0F, normalVector);
        vertex(consumer, matrix, normal, bottomRight, 1.0F, 1.0F, normalVector);
        vertex(consumer, matrix, normal, bottomLeft, 0.0F, 1.0F, normalVector);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, Vec3 point, float u, float v, Vec3 normalVector) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(0, 0, 0, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, (float) normalVector.x,
                        (float) normalVector.y, (float) normalVector.z)
                .endVertex();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HackingDeviceClientState.clear();
        HackingDeviceFocusClient.forceClear();
    }
}
