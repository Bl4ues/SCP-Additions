package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The base map intentionally suppresses labels wider than their room. That is
 * sensible for short names, but makes authored names such as
 * "SCP-914 Containment Chamber" disappear completely. Render only those
 * suppressed labels again, wrapped on word boundaries with a small allowance
 * beyond the room outline.
 *
 * Keep this mixin free of helper/nested classes. Mixin 0.8.5 treats every class
 * generated inside the configured mixin package as transformer-owned and will
 * throw IllegalClassLoadError if the transformed target references one at
 * runtime. Primitive arrays and vanilla collections avoid that loader trap.
 */
@Mixin(value = Scp079FacilityMapScreen.class, remap = false)
public abstract class Scp079FacilityMapRoomLabelMixin {
    private static final int MAP_MARGIN_X = 58;
    private static final int MAP_TOP = 78;
    private static final int MAP_BOTTOM = 64;
    private static final float LABEL_SCALE = 1.02F;
    private static final int ORIGINAL_OVERFLOW_ALLOWANCE = 28;
    private static final int WRAPPED_OVERFLOW_ALLOWANCE = 18;
    private static final int LINE_GAP = 2;

    @Shadow private int floorIndex;
    @Shadow private double mapZoom;
    @Shadow private double panX;
    @Shadow private double panY;

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen;renderTopActions(Lnet/minecraft/client/gui/GuiGraphics;II)V",
                    shift = At.Shift.BEFORE))
    private void scpclassifieddirective$renderWrappedRoomLabels(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        Screen screen = (Screen) (Object) this;
        List<FacilityRoomSnapshot> rooms = scpclassifieddirective$activeFloorRooms();
        if (rooms.isEmpty()) return;
        double[] transform = scpclassifieddirective$transform(screen.width,
                screen.height, rooms, mapZoom, panX, panY);
        if (transform == null) return;

        Font font = minecraft.font;
        for (FacilityRoomSnapshot room : rooms) {
            if (room.name().isBlank()) continue;
            int[] roomBounds = scpclassifieddirective$bounds(List.of(room));
            if (roomBounds == null) continue;

            String name = room.name().strip().toUpperCase(Locale.ROOT);
            double roomWidth = (roomBounds[2] - roomBounds[0] + 1)
                    * transform[2];
            int naturalWidth = Scp079UiTheme.scaledWidth(font, name,
                    LABEL_SCALE);
            // The normal renderer already drew this one. Do not duplicate it.
            if (naturalWidth < roomWidth + ORIGINAL_OVERFLOW_ALLOWANCE) {
                continue;
            }

            int maxLineWidth = Math.max(32,
                    (int) Math.round(roomWidth + WRAPPED_OVERFLOW_ALLOWANCE));
            List<String> lines = scpclassifieddirective$wrap(font, name,
                    maxLineWidth);
            if (lines.isEmpty()) continue;

            int centerX = scpclassifieddirective$sx(transform,
                    (roomBounds[0] + roomBounds[2] + 1) * 0.5D);
            int centerY = scpclassifieddirective$sy(transform,
                    (roomBounds[1] + roomBounds[3] + 1) * 0.5D);
            int lineStep = Math.max(10,
                    Math.round(font.lineHeight * LABEL_SCALE) + LINE_GAP);
            float totalHeight = font.lineHeight * LABEL_SCALE
                    + Math.max(0, lines.size() - 1) * lineStep;
            float y = centerY - totalHeight * 0.5F;

            for (String line : lines) {
                Scp079UiTheme.drawCentered(graphics, font, line,
                        centerX, y, LABEL_SCALE, Scp079UiTheme.TEXT);
                y += lineStep;
            }
        }
    }

    private static List<String> scpclassifieddirective$wrap(Font font,
            String text, int maxWidth) {
        String[] words = text.split("\\s+");
        List<String> lines = new ArrayList<>();
        String current = "";
        for (String word : words) {
            if (word.isBlank()) continue;
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty()
                    && Scp079UiTheme.scaledWidth(font, candidate, LABEL_SCALE)
                            > maxWidth) {
                lines.add(current);
                current = word;
            } else {
                current = candidate;
            }
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }

    private List<FacilityRoomSnapshot> scpclassifieddirective$activeFloorRooms() {
        ResourceLocation dimension = Scp079PlayableClient.hostDimension();
        Map<String, List<FacilityRoomSnapshot>> grouped = new LinkedHashMap<>();
        for (FacilityRoomSnapshot room
                : FacilityMappingClientState.rooms(dimension)) {
            String label = room.floorLongLabel().isBlank()
                    ? "Unassigned Floor" : room.floorLongLabel();
            grouped.computeIfAbsent(label, ignored -> new ArrayList<>())
                    .add(room);
        }
        if (grouped.isEmpty()) return List.of();

        List<String> labels = new ArrayList<>(grouped.keySet());
        labels.sort(Comparator
                .comparingInt((String label) ->
                        scpclassifieddirective$floorY(grouped.get(label)))
                .reversed()
                .thenComparing(String.CASE_INSENSITIVE_ORDER));
        int index = Math.max(0, Math.min(floorIndex, labels.size() - 1));
        return grouped.getOrDefault(labels.get(index), List.of());
    }

    private static int scpclassifieddirective$floorY(
            List<FacilityRoomSnapshot> rooms) {
        return rooms.stream().flatMap(room -> room.patches().stream())
                .mapToInt(FacilityFloorPatch::y).min().orElse(0);
    }

    private static double[] scpclassifieddirective$transform(int width,
            int height, List<FacilityRoomSnapshot> rooms, double zoom,
            double offsetX, double offsetY) {
        int[] bounds = scpclassifieddirective$bounds(rooms);
        if (bounds == null) return null;
        int availableW = Math.max(80, width - MAP_MARGIN_X * 2);
        int availableH = Math.max(80, height - MAP_TOP - MAP_BOTTOM);
        double spanX = Math.max(1.0D, bounds[2] - bounds[0] + 1.0D);
        double spanZ = Math.max(1.0D, bounds[3] - bounds[1] + 1.0D);
        double baseScale = Math.min(availableW / spanX,
                availableH / spanZ);
        baseScale = Math.min(18.0D, Math.max(1.5D, baseScale));
        double scale = baseScale * zoom;
        double mapW = spanX * scale;
        double mapH = spanZ * scale;
        double originX = (width - mapW) * 0.5D
                - bounds[0] * scale + offsetX;
        double originY = MAP_TOP + (availableH - mapH) * 0.5D
                - bounds[1] * scale + offsetY;
        return new double[] {originX, originY, scale};
    }

    private static int[] scpclassifieddirective$bounds(
            List<FacilityRoomSnapshot> rooms) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (FacilityRoomSnapshot room : rooms) {
            for (FacilityFloorPatch patch : room.patches()) {
                minX = Math.min(minX, patch.minX());
                minZ = Math.min(minZ, patch.minZ());
                maxX = Math.max(maxX, patch.maxX());
                maxZ = Math.max(maxZ, patch.maxZ());
            }
        }
        return minX == Integer.MAX_VALUE ? null
                : new int[] {minX, minZ, maxX, maxZ};
    }

    private static int scpclassifieddirective$sx(double[] transform,
            double x) {
        return (int) Math.round(transform[0] + x * transform[2]);
    }

    private static int scpclassifieddirective$sy(double[] transform,
            double z) {
        return (int) Math.round(transform[1] + z * transform[2]);
    }
}
