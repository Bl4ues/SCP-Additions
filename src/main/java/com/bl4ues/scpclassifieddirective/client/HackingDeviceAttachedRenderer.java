package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient.Phase;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry.Attachment;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
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
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Renders reader-attached Hacking Devices in world space. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class HackingDeviceAttachedRenderer {
    private static final float LOGICAL_WIDTH =
            HackingDeviceScreenTextClient.LOGICAL_WIDTH;
    private static final float LOGICAL_HEIGHT =
            HackingDeviceScreenTextClient.LOGICAL_HEIGHT;
    private static final int GREEN = HackingDeviceScreenTextClient.GREEN;
    private static final int GREEN_BRIGHT =
            HackingDeviceScreenTextClient.GREEN_BRIGHT;
    private static final int GREEN_DIM = HackingDeviceScreenTextClient.GREEN_DIM;
    private static final double TEXT_EPSILON = 0.0080D;

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
        List<PendingScreen> pendingScreens = new ArrayList<>();

        for (BlockPos pos : visibleDevices) {
            if (!minecraft.level.hasChunkAt(pos)
                    || camera.distanceToSqr(Vec3.atCenterOf(pos)) > 4096.0D) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(pos);
            Attachment attachment = HackingDeviceAttachmentGeometry.resolve(pos,
                    state);
            if (attachment != null) {
                Matrix4f screenTransform = renderDevice(minecraft, poseStack,
                        buffers, camera, pos, attachment);

                /*
                 * The live Gecko capture is preferred because it follows the
                 * authored screen bone exactly. If a shader/renderer wrapper
                 * prevents that GeoRenderLayer callback from producing a matrix,
                 * fall back to the same physical frame used by the approved
                 * operation camera instead of silently drawing nothing.
                 */
                if (screenTransform == null) {
                    screenTransform = fallbackScreenTransform(poseStack, camera,
                            pos, attachment);
                }
                if (screenTransform != null) {
                    pendingScreens.add(new PendingScreen(pos, screenTransform));
                }
            }
        }

        /*
         * Finish the opaque Gecko body/glass before asking the font renderer for
         * its own text render type. This mirrors the Diagnostic Terminal's pass
         * separation and avoids carrying an item RenderType into the CRT pass.
         */
        buffers.endBatch();
        for (PendingScreen pending : pendingScreens) {
            poseStack.pushPose();
            poseStack.last().pose().set(pending.transform());
            renderAttachedScreenText(pending.pos(), minecraft.font, poseStack,
                    buffers);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static Matrix4f renderDevice(Minecraft minecraft,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            Vec3 camera, BlockPos pos, Attachment attachment) {
        double seating = HackingDeviceClientState.seatingOffset(pos);
        Vec3 origin = attachment.modelOrigin()
                .add(attachment.mountOutward().scale(seating))
                .subtract(camera);

        poseStack.pushPose();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                HackingDeviceAttachmentGeometry.modelYaw(attachment.facing())));
        if (Math.abs(attachment.pitchDegrees()) > 0.001F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(attachment.pitchDegrees()));
        }

        int light = LevelRenderer.getLightColor(minecraft.level, pos);
        ItemStack deviceStack = new ItemStack(
                ScpClassifiedDirectiveModItems.HACKING_DEVICE.get());

        /*
         * ItemDisplayContext.NONE is also used for this world render, so identify
         * the target explicitly while GeckoLib walks the model. The item renderer
         * captures the final logical-screen transform from the live screen cube.
         */
        Matrix4f screenTransform = null;
        HackingDeviceItemRenderer.beginAttachedRender(pos);
        try {
            minecraft.getItemRenderer().renderStatic(deviceStack,
                    ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, minecraft.level, 0);
            screenTransform = HackingDeviceItemRenderer
                    .takeAttachedScreenTransform();
        } finally {
            HackingDeviceItemRenderer.endAttachedRender();
            poseStack.popPose();
        }
        return screenTransform;
    }

    /**
     * Shader-safe fallback using the already-approved physical operation frame.
     * The font's logical origin is the visible top-left of the CRT and logical Y
     * runs downward. Flipping RIGHT together with the normal preserves readable
     * winding if the camera ever ends up on the opposite side.
     */
    private static Matrix4f fallbackScreenTransform(PoseStack poseStack,
            Vec3 camera, BlockPos pos, Attachment attachment) {
        if (attachment == null) return null;

        double seating = HackingDeviceClientState.seatingOffset(pos);
        Frame frame = attachment.screen();
        Vec3 center = frame.center()
                .add(attachment.mountOutward().scale(seating));
        Vec3 up = frame.up().normalize();
        Vec3 right = frame.right().normalize();
        Vec3 normal = frame.outward().normalize();

        if (camera.subtract(center).dot(normal) < 0.0D) {
            normal = normal.scale(-1.0D);
            right = right.scale(-1.0D);
        }

        Vec3 topLeft = center
                .add(right.scale(-frame.width() * 0.5D))
                .add(up.scale(frame.height() * 0.5D))
                .add(normal.scale(TEXT_EPSILON))
                .subtract(camera);

        Matrix4f basis = new Matrix4f().identity();
        basis.m00((float) right.x);
        basis.m01((float) right.y);
        basis.m02((float) right.z);
        basis.m10((float) up.x);
        basis.m11((float) up.y);
        basis.m12((float) up.z);
        basis.m20((float) normal.x);
        basis.m21((float) normal.y);
        basis.m22((float) normal.z);

        float pixelScaleX = (float) (frame.width() / LOGICAL_WIDTH);
        float pixelScaleY = (float) (frame.height() / LOGICAL_HEIGHT);
        float depthScale = Math.min(pixelScaleX, pixelScaleY);

        poseStack.pushPose();
        poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
        poseStack.mulPoseMatrix(basis);
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);
        Matrix4f transform = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();
        return transform;
    }

    /** Draws characters using the physical screen transform resolved above. */
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
                Font.DisplayMode.SEE_THROUGH, 0,
                LightTexture.FULL_BRIGHT);
    }

    private static void draw(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float x,
            float y, int color) {
        var sequence = ScpFonts.anonymousPro(text).getVisualOrderText();
        font.drawInBatch(sequence, x, y, color, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.SEE_THROUGH, 0,
                LightTexture.FULL_BRIGHT);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HackingDeviceClientState.clear();
        HackingDeviceFocusClient.forceClear();
    }

    private record PendingScreen(BlockPos pos, Matrix4f transform) {
    }
}
