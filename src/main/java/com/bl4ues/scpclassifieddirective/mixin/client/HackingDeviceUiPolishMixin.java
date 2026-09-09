package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceScreenTextClient;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds stolen Facility Mapping context to the Chaos Insurgency boot sequence. */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceUiPolishMixin {
    private static final int GREEN = 0xFF49F06F;
    private static final int GREEN_BRIGHT = 0xFF78FF94;
    private static final int GREEN_DIM = 0xFF238A42;
    private static final int AMBER = 0xFFFFC857;

    @Inject(method = "renderBoot", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderMappedBoot(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            CallbackInfo ci) {
        ci.cancel();
        FacilityContext facility = facilityContext();
        int count = HackingDeviceMinigameClient.bootLineCount();

        draw(font, poseStack, buffers,
                String.format("CI//SCIPNET-RIPPER  KCR-L%d",
                        HackingDeviceMinigameClient.accessLevel()),
                8.0F, 7.0F, GREEN_DIM);

        float bootY;
        float bootStep;
        if (facility != null) {
            drawFit(font, poseStack, buffers,
                    "STOLEN MAP: " + facility.zone() + " / " + facility.floor(),
                    8.0F, 21.0F, 240.0F, AMBER);
            drawFit(font, poseStack, buffers,
                    "TARGET ROOM: " + facility.room(),
                    8.0F, 34.0F, 240.0F, GREEN_BRIGHT);
            bootY = 52.0F;
            bootStep = 16.0F;
        } else {
            draw(font, poseStack, buffers,
                    "STOLEN MAP: [NO FACILITY CACHE]", 8.0F, 24.0F,
                    AMBER);
            bootY = 43.0F;
            bootStep = 18.0F;
        }

        String[] lines = {
                "> CERT CHAIN........FORGED",
                "> NODE MAP.........STOLEN",
                "> AUTH BUS........HOTWIRE",
                "> ACL CACHE.........LIED",
                "> BREACH MODULES....ARMED"
        };
        for (int index = 0; index < count; index++) {
            draw(font, poseStack, buffers, lines[index], 8.0F,
                    bootY + index * bootStep,
                    index == count - 1 ? GREEN_BRIGHT : GREEN);
        }

        draw(font, poseStack, buffers, "PATCHSET: BLACKBOX/0.7F-UNSIGNED",
                8.0F, 139.0F, AMBER);
    }

    @Inject(method = "renderSuccess", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderTerminalSuccess(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            CallbackInfo ci) {
        ci.cancel();
        HackingDeviceScreenTextClient.renderSuccess(font, poseStack, buffers);
    }

    @Inject(method = "renderSession", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderGrantCountdown(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            CallbackInfo ci) {
        if (HackingDeviceMinigameClient.phase()
                == HackingDeviceMinigameClient.Phase.COOLDOWN) {
            HackingDeviceScreenTextClient.renderAttachedCooldown(font, poseStack,
                    buffers);
            ci.cancel();
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
        String zone = "UNKNOWN";
        String floor = longLabel.isBlank() ? "UNASSIGNED" : longLabel;
        int separator = longLabel.indexOf(" - ");
        if (separator > 0) {
            zone = longLabel.substring(0, separator).strip();
            floor = longLabel.substring(separator + 3).strip();
        }
        String roomName = clean(room.name());
        if (roomName.isBlank()) roomName = "UNNAMED ROOM";
        return new FacilityContext(zone, floor, roomName);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip()
                .replace('\n', ' ').replace('\r', ' ');
    }

    private static void drawFit(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float x,
            float y, float maxWidth, int color) {
        String source = text == null ? "" : text;
        if (width(source) <= maxWidth) {
            draw(font, poseStack, buffers, source, x, y, color);
            return;
        }
        String suffix = "...";
        int end = source.length();
        while (end > 0) {
            String candidate = source.substring(0, end).stripTrailing() + suffix;
            if (width(candidate) <= maxWidth) {
                draw(font, poseStack, buffers, candidate, x, y, color);
                return;
            }
            end--;
        }
        draw(font, poseStack, buffers, suffix, x, y, color);
    }

    private static float width(String text) {
        return HackingDeviceAttachedRenderer.pixelWidth(text);
    }

    private static void draw(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, String text, float x,
            float y, int color) {
        HackingDeviceAttachedRenderer.pixelDraw(text, x, y, color);
    }

    private record FacilityContext(String zone, String floor, String room) {
    }
}
