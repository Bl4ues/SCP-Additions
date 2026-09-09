package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient.Phase;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry.Attachment;
import com.bl4ues.scpclassifieddirective.hacking.HackingDevicePuzzle;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Renders the device physically seated on readers. The CRT background is the
 * actual `screen` plane in the GeckoLib model. Its characters are injected by
 * HackingDeviceItemRenderer from the very same model pose, so there is no
 * second world-space screen that can clip, jump or disagree with the model.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class HackingDeviceAttachedRenderer {
    private static final float LOGICAL_WIDTH =
            HackingDeviceScreenTextClient.LOGICAL_WIDTH;
    private static final int GREEN = HackingDeviceScreenTextClient.GREEN;
    private static final int GREEN_BRIGHT =
            HackingDeviceScreenTextClient.GREEN_BRIGHT;
    private static final int GREEN_DIM = HackingDeviceScreenTextClient.GREEN_DIM;

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
            Attachment attachment = HackingDeviceAttachmentGeometry.resolve(pos,
                    state);
            if (attachment == null) continue;
            renderDevice(minecraft, poseStack, buffers, camera, pos,
                    attachment);
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

        HackingDeviceItemRenderer.beginAttachedRender(pos);
        try {
            minecraft.getItemRenderer().renderStatic(deviceStack,
                    ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, minecraft.level, 0);
        } finally {
            HackingDeviceItemRenderer.endAttachedRender();
        }
        poseStack.popPose();
    }

    /** Called from the Hacking Device's GeoRenderLayer in the live model pose. */
    public static void renderAttachedScreenText(BlockPos pos, Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        if (HackingDeviceMinigameClient.active()
                && pos != null && pos.equals(HackingDeviceMinigameClient.pos())) {
            renderSession(font, poseStack, buffers);
        } else {
            draw(font, poseStack, buffers, "CI FIELD UNIT // STANDBY",
                    10.0F, 68.0F, GREEN_DIM);
        }
    }

    /* Keep these method signatures stable: HackingDeviceUiPolishMixin adds the
     * Facility Mapping lines and the finalized success sequence here. */
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
            case COOLDOWN -> HackingDeviceScreenTextClient.renderAttachedCooldown(
                    font, poseStack, buffers);
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
                String.format("LINK %3d%%",
                        (int) Math.round(progress * 100.0D)),
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
                String.format("TARGET: KCR-L%d",
                        HackingDeviceMinigameClient.accessLevel()),
                10, 15, GREEN_DIM);
        String[] lines = {
                "> SNIFF AUTH BUS........OK",
                "> CAPTURE FRAME.........OK",
                "> CRC/XOR TABLE......LOADED",
                "> BREACH CHANNEL......READY"
        };
        for (int index = 0; index < count; index++) {
            draw(font, poseStack, buffers, lines[index], 10,
                    42 + index * 22,
                    index == count - 1 ? GREEN_BRIGHT : GREEN);
        }
        if ((System.nanoTime() / 80_000_000L & 1L) == 0L) {
            draw(font, poseStack, buffers, "7A:4C:FF/03  0xA91E",
                    104, 132, GREEN_DIM);
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
        HackingDeviceScreenTextClient.renderSuccess(font, poseStack, buffers);
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

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HackingDeviceClientState.clear();
        HackingDeviceFocusClient.forceClear();
    }
}
