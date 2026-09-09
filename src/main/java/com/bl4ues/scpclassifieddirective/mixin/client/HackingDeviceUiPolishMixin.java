package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient;
import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds mapped facility context to the Hacking Device and replaces the old
 * lock drawing with the same terse terminal-language used by its boot sequence.
 */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceUiPolishMixin {
    private static final float WIDTH = 256.0F;
    private static final int GREEN = 0xFF49F06F;
    private static final int GREEN_BRIGHT = 0xFF78FF94;
    private static final int GREEN_DIM = 0xFF238A42;

    @Inject(method = "renderBoot", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderMappedBoot(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            CallbackInfo ci) {
        ci.cancel();
        FacilityContext facility = facilityContext();
        int count = HackingDeviceMinigameClient.bootLineCount();

        draw(font, poseStack, buffers,
                String.format("TARGET: KCR-L%d",
                        HackingDeviceMinigameClient.accessLevel()),
                8.0F, 7.0F, GREEN_DIM);

        float bootY;
        float bootStep;
        if (facility != null) {
            drawFit(font, poseStack, buffers, "ZONE....." + facility.zone(),
                    8.0F, 23.0F, 240.0F, GREEN);
            drawFit(font, poseStack, buffers, "FLOOR...." + facility.floor(),
                    8.0F, 37.0F, 240.0F, GREEN);
            drawFit(font, poseStack, buffers, "ROOM....." + facility.room(),
                    8.0F, 51.0F, 240.0F, GREEN_BRIGHT);
            bootY = 72.0F;
            bootStep = 15.0F;
        } else {
            bootY = 35.0F;
            bootStep = 22.0F;
        }

        String[] lines = {
                "> SNIFF AUTH BUS........OK",
                "> CAPTURE FRAME.........OK",
                "> CRC/XOR TABLE......LOADED",
                "> BREACH CHANNEL......READY"
        };
        for (int index = 0; index < count; index++) {
            draw(font, poseStack, buffers, lines[index], 8.0F,
                    bootY + index * bootStep,
                    index == count - 1 ? GREEN_BRIGHT : GREEN);
        }

        if ((System.nanoTime() / 80_000_000L & 1L) == 0L) {
            draw(font, poseStack, buffers, "7A:4C:FF/03  0xA91E",
                    104.0F, 138.0F, GREEN_DIM);
        }
    }

    @Inject(method = "renderPuzzle", at = @At("TAIL"))
    private static void scpclassifieddirective$renderMappedPuzzleContext(
            Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, CallbackInfo ci) {
        FacilityContext facility = facilityContext();
        if (facility == null) return;
        String line = "MAP: " + facility.compactZone() + " / "
                + facility.compactFloor() + " / " + facility.room();
        drawFit(font, poseStack, buffers, line, 8.0F, 116.0F,
                240.0F, GREEN_DIM);
    }

    @Inject(method = "renderSuccess", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderTerminalSuccess(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            CallbackInfo ci) {
        ci.cancel();
        double progress = HackingDeviceMinigameClient.phaseProgress();
        draw(font, poseStack, buffers, "BREACH COMMIT // FINAL",
                8.0F, 8.0F, GREEN_DIM);

        String[] lines = {
                "> CRC CHAIN............VALID",
                "> FORGE AUTH FRAME.......OK",
                "> INJECT CREDENTIAL......OK",
                "> OVERRIDE ACCESS LIST...OK",
                "> READER HANDSHAKE.....ACCEPT"
        };
        int visible = Math.min(lines.length,
                Math.max(1, 1 + (int) Math.floor(progress * 7.0D)));
        for (int index = 0; index < visible; index++) {
            draw(font, poseStack, buffers, lines[index], 8.0F,
                    31.0F + index * 17.0F,
                    index == visible - 1 ? GREEN_BRIGHT : GREEN);
        }

        if (progress > 0.38D) {
            long pulse = System.nanoTime() / 90_000_000L;
            String trace = String.format("AUTH TRACE %02X:%02X  TOKEN %04X",
                    (pulse * 29L) & 0xFFL,
                    (pulse * 71L + 0x4CL) & 0xFFL,
                    (pulse * 313L + 0xA91EL) & 0xFFFFL);
            draw(font, poseStack, buffers, trace, 8.0F, 120.0F,
                    GREEN_DIM);
        }
        if (progress >= 0.60D) {
            centered(font, poseStack, buffers, "ACCESS GRANTED",
                    138.0F, GREEN_BRIGHT);
        }
    }

    private static FacilityContext facilityContext() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || HackingDeviceMinigameClient.pos() == null) {
            return null;
        }
        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                minecraft.level.dimension().location(),
                HackingDeviceMinigameClient.pos());
        if (room == null) return null;

        String longLabel = clean(room.floorLongLabel());
        String shortLabel = clean(room.floorShortLabel());
        String zone = "UNKNOWN";
        String floor = longLabel.isBlank() ? "UNASSIGNED" : longLabel;
        String compactZone = "?";
        String compactFloor = shortLabel.isBlank() ? "?" : shortLabel;

        int longSeparator = longLabel.indexOf(" - ");
        if (longSeparator > 0) {
            zone = longLabel.substring(0, longSeparator).strip();
            floor = longLabel.substring(longSeparator + 3).strip();
        }
        int shortSeparator = shortLabel.indexOf(" - ");
        if (shortSeparator > 0) {
            compactZone = shortLabel.substring(0, shortSeparator).strip();
            compactFloor = shortLabel.substring(shortSeparator + 3).strip();
        }

        String roomName = clean(room.name());
        if (roomName.isBlank()) roomName = "UNNAMED ROOM";
        return new FacilityContext(zone, floor, roomName,
                compactZone, compactFloor);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip()
                .replace('\n', ' ').replace('\r', ' ');
    }

    private static void drawFit(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float x,
            float y, float maxWidth, int color) {
        String fitted = fit(font, text, maxWidth);
        draw(font, poseStack, buffers, fitted, x, y, color);
    }

    private static String fit(Font font, String text, float maxWidth) {
        String source = text == null ? "" : text;
        if (width(font, source) <= maxWidth) return source;
        String suffix = "...";
        int end = source.length();
        while (end > 0) {
            String candidate = source.substring(0, end).stripTrailing() + suffix;
            if (width(font, candidate) <= maxWidth) return candidate;
            end--;
        }
        return suffix;
    }

    private static int width(Font font, String text) {
        return font.width(ScpFonts.anonymousPro(text).getVisualOrderText());
    }

    private static void centered(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float y,
            int color) {
        var sequence = ScpFonts.anonymousPro(text).getVisualOrderText();
        float x = (WIDTH - font.width(sequence)) * 0.5F;
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

    private record FacilityContext(String zone, String floor, String room,
            String compactZone, String compactFloor) {
    }
}
