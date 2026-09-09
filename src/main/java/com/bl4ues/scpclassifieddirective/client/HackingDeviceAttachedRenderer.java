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

/** Renders reader-attached Hacking Devices and delegates their CRT to the item renderer. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class HackingDeviceAttachedRenderer {
    private static final int GREEN = HackingDeviceScreenTextClient.GREEN;
    private static final int GREEN_BRIGHT =
            HackingDeviceScreenTextClient.GREEN_BRIGHT;
    private static final int GREEN_DIM = HackingDeviceScreenTextClient.GREEN_DIM;
    private static final int AMBER = 0xFFFFC857;
    private static final int RED = 0xFFFF675C;
    private static final float PAD_X = 12.0F;
    private static final float INNER_X = 16.0F;

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
            if (attachment != null) {
                renderDevice(minecraft, poseStack, buffers, camera, pos,
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
        HackingDeviceItemRenderer.beginAttachedRender(pos);
        try {
            minecraft.getItemRenderer().renderStatic(deviceStack,
                    ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, minecraft.level, 0);
        } finally {
            HackingDeviceItemRenderer.endAttachedRender();
            poseStack.popPose();
        }
    }

    public static void renderAttachedScreenText(BlockPos pos, Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        renderAttachedPixels(pos);
    }

    public static void renderAttachedPixels(BlockPos pos) {
        if (HackingDeviceMinigameClient.active()
                && pos != null && pos.equals(HackingDeviceMinigameClient.pos())) {
            renderSession(null, null, null);
        } else {
            HackingDevicePixelFont.draw("CI FIELD UNIT // STANDBY",
                    PAD_X, 68.0F, GREEN_DIM);
        }
    }

    /* Keep signatures stable for the facility-mapping/UI-polish mixin. */
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
        int filled = (int) Math.round(progress * 28.0D);
        String bar = "[" + "#".repeat(Math.max(0, Math.min(28, filled)))
                + ".".repeat(Math.max(0, 28 - filled)) + "]";
        draw(font, poseStack, buffers, "CI//FIELDNODE HDEV-RIPPER", PAD_X, 12,
                GREEN_DIM);
        draw(font, poseStack, buffers, "SCIPNET BRIDGE: UNSIGNED / DIRTY",
                PAD_X, 30, AMBER);
        draw(font, poseStack, buffers, "> JACK READER BUS", PAD_X, 52, GREEN);
        draw(font, poseStack, buffers, "> SPOOF FOUNDATION HANDSHAKE", PAD_X,
                69, GREEN);
        draw(font, poseStack, buffers, bar, PAD_X, 94, GREEN_BRIGHT);
        draw(font, poseStack, buffers,
                String.format("BOOTSTRAP %3d%% // DO NOT UNPLUG",
                        (int) Math.round(progress * 100.0D)),
                PAD_X, 111, GREEN_DIM);
        if (progress > 0.72D) {
            draw(font, poseStack, buffers, "FALLBACK ROUTE......ARMED", PAD_X,
                    132, AMBER);
        }
    }

    private static void renderBoot(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        int count = HackingDeviceMinigameClient.bootLineCount();
        draw(font, poseStack, buffers,
                String.format("CI//SCIPNET-RIPPER  KCR-L%d",
                        HackingDeviceMinigameClient.accessLevel()),
                PAD_X, 9, GREEN_DIM);
        String[] lines = {
                "> CERT CHAIN........FORGED",
                "> NODE MAP.........STOLEN",
                "> AUTH BUS........HOTWIRE",
                "> ACL CACHE.........LIED",
                "> BREACH MODULES....ARMED"
        };
        for (int index = 0; index < count; index++) {
            draw(font, poseStack, buffers, lines[index], PAD_X,
                    34 + index * 20,
                    index == count - 1 ? GREEN_BRIGHT : GREEN);
        }
        draw(font, poseStack, buffers, "PATCHSET: BLACKBOX/0.7F-UNSIGNED",
                PAD_X, 139, AMBER);
    }

    private static void renderPuzzle(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        HackingDevicePuzzle puzzle = HackingDeviceMinigameClient.puzzle();
        if (puzzle == null) return;

        draw(font, poseStack, buffers, "CI//SCIPNET BRIDGE [UNSIGNED]", PAD_X,
                6, GREEN_DIM);
        draw(font, poseStack, buffers,
                String.format("KCR-L%d  CHAIN %d/%d  ERR %d/3",
                        HackingDeviceMinigameClient.accessLevel(),
                        HackingDeviceMinigameClient.round(),
                        HackingDeviceMinigameClient.requiredWins(),
                        HackingDeviceMinigameClient.failures()),
                PAD_X, 17, GREEN);
        draw(font, poseStack, buffers,
                "--------------------------------------", PAD_X, 28, GREEN_DIM);

        switch (puzzle.type()) {
            case CIRCUIT_PATH -> renderCircuit(font, poseStack, buffers, puzzle);
            case VISUAL_CHECKSUM -> renderChecksum(font, poseStack, buffers,
                    puzzle);
            case FIREWALL_WINDOWS -> renderFirewall(font, poseStack, buffers,
                    puzzle);
            case HOLD_SIGNAL -> renderHoldSignal(font, poseStack, buffers,
                    puzzle);
            case FREQUENCY_LOCK -> renderFrequency(font, poseStack, buffers,
                    puzzle);
        }
    }

    private static void renderCircuit(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, HackingDevicePuzzle puzzle) {
        int count = Math.max(1, puzzle.data(0, 3));
        int target = puzzle.data(2, 0);
        int current = HackingDeviceMinigameClient.circuitMask();
        int selected = HackingDeviceMinigameClient.selectedIndex();

        draw(font, poseStack, buffers, "HOTWIRE // CIRCUIT PATH", PAD_X, 39,
                AMBER);
        draw(font, poseStack, buffers, "MATCH TRACE. PATCH BROKEN NODES.",
                PAD_X, 52, GREEN_DIM);
        draw(font, poseStack, buffers, "TRACE  " + circuitString(target, count),
                PAD_X, 70, GREEN_BRIGHT);
        draw(font, poseStack, buffers, "PATCH  " + circuitString(current, count),
                PAD_X, 88, GREEN);
        float caretX = PAD_X + pixelWidth("PATCH  ") + selected * 12.0F;
        draw(font, poseStack, buffers, "^", caretX, 100, AMBER);
        int bad = Integer.bitCount((target ^ current) & ((1 << count) - 1));
        draw(font, poseStack, buffers,
                String.format("OPEN LINKS: %d   NODE %d/%d", bad,
                        selected + 1, count), PAD_X, 115,
                bad == 0 ? GREEN_BRIGHT : GREEN_DIM);
        controls(font, poseStack, buffers,
                "A/D NODE  SPACE FLIP  ENTER INJECT");
    }

    private static void renderChecksum(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, HackingDevicePuzzle puzzle) {
        int count = Math.max(1, Math.min(5, puzzle.data(0, 3)));
        int max = Math.max(1, puzzle.data(1, 3));
        int target = puzzle.data(2, 0);
        int selected = HackingDeviceMinigameClient.selectedIndex();

        draw(font, poseStack, buffers, "VISUAL CHECKSUM // FORGE", PAD_X, 39,
                AMBER);
        draw(font, poseStack, buffers,
                String.format("MAKE SUM = %02d   CURRENT = %02d", target,
                        HackingDeviceMinigameClient.checksumSum()),
                PAD_X, 52, GREEN_BRIGHT);

        for (int i = 0; i < count; i++) {
            int value = HackingDeviceMinigameClient.checksumValue(i);
            String bar = "#".repeat(value) + ".".repeat(Math.max(0, max - value));
            draw(font, poseStack, buffers,
                    String.format("%s CH%d [%s] %d", i == selected ? ">" : " ",
                            i + 1, bar, value),
                    INNER_X, 67 + i * 13, i == selected ? GREEN_BRIGHT : GREEN);
        }
        controls(font, poseStack, buffers,
                "A/D CHANNEL  SPACE +  ENTER FORGE");
    }

    private static void renderFirewall(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, HackingDevicePuzzle puzzle) {
        int gates = Math.max(1, puzzle.data(0, 2));
        int gate = HackingDeviceMinigameClient.firewallGate();
        int lane = HackingDeviceMinigameClient.firewallLane();
        int open = HackingDeviceMinigameClient.firewallOpenLane();
        int periodMs = Math.max(250, puzzle.data(1, 800));

        draw(font, poseStack, buffers, "FIREWALL WINDOWS // PUNCH", PAD_X, 39,
                AMBER);
        draw(font, poseStack, buffers,
                String.format("GATE %d/%d   WINDOW %.2fs", Math.min(gate + 1,
                                gates), gates, periodMs / 1000.0F),
                PAD_X, 52, GREEN_DIM);
        for (int row = 0; row < 3; row++) {
            String state = row == open ? "[     OPEN     ]" : "[##############]";
            draw(font, poseStack, buffers,
                    String.format("%s LANE %d -- %s", row == lane ? ">" : " ",
                            row + 1, state), INNER_X, 70 + row * 18,
                    row == open ? GREEN_BRIGHT : GREEN_DIM);
        }
        draw(font, poseStack, buffers,
                "MOVE TO OPEN LANE. SPACE = CROSS.", PAD_X, 127, GREEN);
        controls(font, poseStack, buffers, "A/D LANE  SPACE BREACH");
    }

    private static void renderHoldSignal(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, HackingDevicePuzzle puzzle) {
        int center = puzzle.data(0, 50);
        int half = Math.max(2, puzzle.data(1, 10));
        float marker = HackingDeviceMinigameClient.holdMarker();
        float progress = HackingDeviceMinigameClient.holdProgress();

        draw(font, poseStack, buffers, "HOLD SIGNAL // DIRTY PLL", PAD_X, 39,
                AMBER);
        draw(font, poseStack, buffers, "KEEP CARRIER INSIDE LOCK BAND.", PAD_X,
                52, GREEN_DIM);
        draw(font, poseStack, buffers,
                signalBar(marker, center - half, center + half), PAD_X, 72,
                GREEN_BRIGHT);
        draw(font, poseStack, buffers,
                String.format("CARRIER %05.1f   BAND %02d..%02d", marker,
                        center - half, center + half), PAD_X, 91, GREEN);
        int filled = Math.max(0, Math.min(18, Math.round(progress * 18.0F)));
        draw(font, poseStack, buffers,
                "LOCK [" + "#".repeat(filled)
                        + ".".repeat(18 - filled) + "]",
                PAD_X, 109, progress >= 1.0F ? GREEN_BRIGHT : GREEN);
        draw(font, poseStack, buffers,
                String.format("HOLD %3d%% // KEEP SIGNAL STABLE",
                        Math.round(progress * 100.0F)),
                PAD_X, 125, GREEN_DIM);
        controls(font, poseStack, buffers, "A/D TRIM  HOLD IN BAND");
    }

    private static void renderFrequency(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, HackingDevicePuzzle puzzle) {
        int target = puzzle.data(0, 50);
        int tolerance = Math.max(1, puzzle.data(1, 5));
        int current = HackingDeviceMinigameClient.frequencyValue();

        draw(font, poseStack, buffers, "FREQUENCY LOCK // BUS TAP", PAD_X, 39,
                AMBER);
        draw(font, poseStack, buffers, "TUNE CARRIER INTO CAPTURE BAND.", PAD_X,
                52, GREEN_DIM);
        draw(font, poseStack, buffers,
                signalBar(current, target - tolerance, target + tolerance),
                PAD_X, 75, GREEN_BRIGHT);
        draw(font, poseStack, buffers,
                String.format("TUNE %03d   TARGET %03d +/- %02d", current,
                        target, tolerance), PAD_X, 95,
                Math.abs(current - target) <= tolerance
                        ? GREEN_BRIGHT : GREEN);
        draw(font, poseStack, buffers,
                Math.abs(current - target) <= tolerance
                        ? "LOCK WINDOW ACQUIRED // INJECT NOW"
                        : "NO CARRIER // KEEP TUNING",
                PAD_X, 116,
                Math.abs(current - target) <= tolerance
                        ? GREEN_BRIGHT : GREEN_DIM);
        controls(font, poseStack, buffers, "A/D TUNE  ENTER LOCK");
    }

    private static void renderRoundOk(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        centered(font, poseStack, buffers, "CHANNEL BREACHED", 43,
                GREEN_BRIGHT);
        centered(font, poseStack, buffers, "PATCH ACCEPTED BY SCIPNET", 62,
                GREEN);
        centered(font, poseStack, buffers,
                String.format("CHAIN %d/%d", HackingDeviceMinigameClient.round(),
                        HackingDeviceMinigameClient.requiredWins()),
                83, AMBER);
        centered(font, poseStack, buffers, "> SWAPPING ATTACK MODULE...", 108,
                GREEN_DIM);
        centered(font, poseStack, buffers, "> NEXT VECTOR STAGED", 128,
                GREEN_DIM);
    }

    private static void renderDenied(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        centered(font, poseStack, buffers, "!! SCIPNET KICKBACK !!", 41, RED);
        centered(font, poseStack, buffers, "ROUTE BURNED / AUTH REJECTED", 61,
                GREEN);
        centered(font, poseStack, buffers,
                String.format("ERROR BUDGET %d/3",
                        HackingDeviceMinigameClient.failures()),
                82, AMBER);
        centered(font, poseStack, buffers, "> DUMPING MODULE...", 105,
                GREEN_DIM);
        centered(font, poseStack, buffers, "> SWITCHING BREACH VECTOR...", 122,
                GREEN_DIM);
    }

    private static void renderLocked(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        centered(font, poseStack, buffers, "SCIPNET HAS NOTICED US", 42, RED);
        centered(font, poseStack, buffers, "THREE BAD HANDSHAKES", 63, AMBER);
        centered(font, poseStack, buffers, "BURN SESSION / PULL CABLE", 85,
                GREEN);
        centered(font, poseStack, buffers, "FIELD SESSION TERMINATED", 113,
                GREEN_DIM);
    }

    private static void renderSuccess(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        HackingDeviceScreenTextClient.renderSuccess(font, poseStack, buffers);
    }

    private static void controls(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String line) {
        draw(font, poseStack, buffers,
                HackingDeviceMinigameClient.waitingForServer()
                        ? "[ WAITING ON FORGED AUTH... ]" : line,
                PAD_X, 140, HackingDeviceMinigameClient.waitingForServer()
                        ? AMBER : GREEN_DIM);
    }

    private static String circuitString(int mask, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) builder.append(' ');
            builder.append((mask & 1 << i) != 0 ? '/' : '\\');
        }
        return builder.toString();
    }

    private static String signalBar(float marker, float low, float high) {
        int cells = 30;
        int markerCell = Math.max(0, Math.min(cells - 1,
                Math.round(marker / 100.0F * (cells - 1))));
        int lowCell = Math.max(0, Math.min(cells - 1,
                Math.round(low / 100.0F * (cells - 1))));
        int highCell = Math.max(0, Math.min(cells - 1,
                Math.round(high / 100.0F * (cells - 1))));
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < cells; i++) {
            if (i == markerCell) builder.append('|');
            else if (i >= lowCell && i <= highCell) builder.append('=');
            else builder.append('.');
        }
        return builder.append(']').toString();
    }

    /** Public bridge used by the UI-polish mixin without touching Font buffers. */
    public static void pixelDraw(String text, float x, float y, int color) {
        HackingDevicePixelFont.draw(text, x, y, color);
    }

    public static void pixelCentered(String text, float y, int color) {
        HackingDevicePixelFont.centered(text, y, color);
    }

    public static float pixelWidth(String text) {
        return HackingDevicePixelFont.width(text);
    }

    private static void centered(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float y,
            int color) {
        HackingDevicePixelFont.centered(text, y, color);
    }

    private static void draw(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float x,
            float y, int color) {
        HackingDevicePixelFont.draw(text, x, y, color);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HackingDevicePixelFont.end();
        HackingDeviceClientState.clear();
        HackingDeviceFocusClient.forceClear();
    }
}
