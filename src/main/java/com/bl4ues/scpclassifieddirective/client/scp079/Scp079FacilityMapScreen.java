package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Full-screen Surveillance Map generated entirely from authored room floors. */
public final class Scp079FacilityMapScreen extends Screen {
    private static final ResourceLocation TRACKER_ARROW = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/gui/scp079/map/scp_tracker_arrow.png");
    private static final int MAP_MARGIN_X = 58;
    private static final int MAP_TOP = 68;
    private static final int MAP_BOTTOM = 48;

    private final List<FloorGroup> floors;
    private int floorIndex;
    private boolean floorMenuOpen;
    private boolean leaveConfirmation;
    private FacilityRoomSnapshot hoveredRoom;

    private Scp079FacilityMapScreen() {
        super(ScpFonts.titillium("SCP-079 Surveillance Map"));
        floors = buildFloors();
        floorIndex = initialFloor(floors);
    }

    public static void open() {
        if (!Scp079PlayableClient.active()) return;
        Minecraft.getInstance().setScreen(new Scp079FacilityMapScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        int width = this.width;
        int height = this.height;
        graphics.fill(0, 0, width, height, 0xFF06121A);
        renderScanlines(graphics, width, height);
        renderFrame(graphics, width, height);

        graphics.drawString(font, ScpFonts.roboto("SURVEILLANCE MAP"),
                26, 24, 0xFFBDEEFF, false);
        graphics.drawString(font, ScpFonts.roboto("SCP-079 / FACILITY NETWORK"),
                26, 38, 0xFF6F9DAE, false);

        if (!Scp079PlayableClient.networkAvailable()) {
            graphics.drawString(font, ScpFonts.roboto("NETWORK OFFLINE"),
                    26, 52, 0xFFD47C72, false);
        }

        if (floors.isEmpty()) {
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(Scp079PlayableClient.networkAvailable()
                            ? "NO MAPPED FACILITY ROOMS"
                            : "FACILITY NETWORK UNAVAILABLE"),
                    width / 2, height / 2, 0xFF6F9DAE);
        } else {
            FloorGroup floor = floors.get(Mth.clamp(floorIndex, 0,
                    floors.size() - 1));
            renderFloorSelector(graphics, mouseX, mouseY, floor);
            renderMap(graphics, mouseX, mouseY, floor);
        }

        renderPower(graphics);
        renderTopActions(graphics, mouseX, mouseY);
        if (leaveConfirmation) {
            renderLeaveConfirmation(graphics, mouseX, mouseY);
        }
    }

    private void renderTopActions(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = 22;
        int leaveW = 112;
        int leaveX = width - leaveW - 24;
        boolean leaveHover = !leaveConfirmation && inside(mouseX, mouseY,
                leaveX, y, leaveW, 22);
        graphics.fill(leaveX, y, leaveX + leaveW, y + 22,
                leaveHover ? 0xD5472429 : 0xB8231A20);
        border(graphics, leaveX, y, leaveW, 22,
                leaveHover ? 0xFFE09A91 : 0xFF72535B);
        graphics.drawCenteredString(font, ScpFonts.roboto("LEAVE SCP ROLE"),
                leaveX + leaveW / 2, y + 7,
                leaveHover ? 0xFFFFFFFF : 0xFFCBA7AA);

        if (!Scp079PlayableClient.cameraMode()) return;
        String label = "RETURN TO LOCAL HOST";
        int w = Math.max(126, font.width(ScpFonts.roboto(label)) + 20);
        int x = leaveX - w - 10;
        boolean hover = !leaveConfirmation && inside(mouseX, mouseY,
                x, y, w, 22);
        graphics.fill(x, y, x + w, y + 22,
                hover ? 0xD51A3545 : 0xB8122835);
        border(graphics, x, y, w, 22,
                hover ? 0xFFBDEEFF : 0xFF52798C);
        graphics.drawCenteredString(font, ScpFonts.roboto(label),
                x + w / 2, y + 7,
                hover ? 0xFFFFFFFF : 0xFFBDEEFF);
    }

    private void renderFloorSelector(GuiGraphics graphics, int mouseX,
            int mouseY, FloorGroup floor) {
        String label = floor.longLabel;
        int w = Math.max(160, font.width(label) + 36);
        int x = (width - w) / 2;
        int y = 21;
        boolean hovered = !leaveConfirmation
                && mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + 22;
        graphics.fill(x, y, x + w, y + 22,
                hovered || floorMenuOpen ? 0xD51A3545 : 0xB8122835);
        border(graphics, x, y, w, 22,
                floorMenuOpen ? 0xFFBDEEFF : 0xFF52798C);
        graphics.drawCenteredString(font, ScpFonts.roboto(label),
                width / 2 - 7, y + 7, 0xFFE8F8FF);
        graphics.drawString(font, ScpFonts.roboto(floorMenuOpen ? "▲" : "▼"),
                x + w - 17, y + 7, 0xFF9CDCF7, false);

        if (!floorMenuOpen) return;
        int rowY = y + 25;
        int maxRows = Math.min(8, floors.size());
        int start = Math.max(0, Math.min(floorIndex - maxRows / 2,
                floors.size() - maxRows));
        for (int i = start; i < start + maxRows; i++) {
            FloorGroup option = floors.get(i);
            boolean rowHover = !leaveConfirmation
                    && mouseX >= x && mouseX < x + w
                    && mouseY >= rowY && mouseY < rowY + 19;
            graphics.fill(x, rowY, x + w, rowY + 19,
                    i == floorIndex ? 0xE1265064
                            : rowHover ? 0xE11A3A4B : 0xE10B202B);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(option.longLabel), width / 2,
                    rowY + 5, i == floorIndex ? 0xFFFFFFFF : 0xFFBDEEFF);
            rowY += 19;
        }
    }

    private void renderMap(GuiGraphics graphics, int mouseX, int mouseY,
            FloorGroup floor) {
        Bounds bounds = Bounds.of(floor.rooms);
        if (bounds == null) return;
        int availableW = Math.max(80, width - MAP_MARGIN_X * 2);
        int availableH = Math.max(80, height - MAP_TOP - MAP_BOTTOM);
        double spanX = Math.max(1.0D, bounds.maxX - bounds.minX + 1.0D);
        double spanZ = Math.max(1.0D, bounds.maxZ - bounds.minZ + 1.0D);
        double scale = Math.min(availableW / spanX, availableH / spanZ);
        scale = Math.min(18.0D, Math.max(1.5D, scale));
        double mapW = spanX * scale;
        double mapH = spanZ * scale;
        double originX = (width - mapW) * 0.5D - bounds.minX * scale;
        double originY = MAP_TOP + (availableH - mapH) * 0.5D
                - bounds.minZ * scale;

        hoveredRoom = null;
        FacilityRoomSnapshot currentRoom = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                net.minecraft.core.BlockPos.containing(
                        Scp079PlayableClient.viewPosition()));

        for (FacilityRoomSnapshot room : floor.rooms) {
            boolean hovered = !leaveConfirmation
                    && roomContainsScreen(room, mouseX, mouseY,
                    originX, originY, scale);
            if (hovered) hoveredRoom = room;
            boolean current = currentRoom != null
                    && currentRoom.id().equals(room.id());
            int fill = current ? 0xC628A77B
                    : hovered ? 0xC65AA7C8 : 0xA63C6E87;
            int line = current ? 0xFF89FFD0
                    : hovered ? 0xFFE7FAFF : 0xFF73A5BC;
            for (FacilityFloorPatch patch : room.patches()) {
                int x1 = (int) Math.round(originX + patch.minX() * scale);
                int y1 = (int) Math.round(originY + patch.minZ() * scale);
                int x2 = (int) Math.round(originX
                        + (patch.maxX() + 1) * scale);
                int y2 = (int) Math.round(originY
                        + (patch.maxZ() + 1) * scale);
                graphics.fill(x1, y1, x2, y2, fill);
                border(graphics, x1, y1, Math.max(1, x2 - x1),
                        Math.max(1, y2 - y1), line);
            }
            if (!room.name().isBlank()) {
                RoomBounds rb = RoomBounds.of(room);
                if (rb != null) {
                    int centerX = (int) Math.round(originX
                            + (rb.minX + rb.maxX + 1) * 0.5D * scale);
                    int centerY = (int) Math.round(originY
                            + (rb.minZ + rb.maxZ + 1) * 0.5D * scale);
                    String name = room.name().toUpperCase();
                    if (font.width(name)
                            < (rb.maxX - rb.minX + 1) * scale + 28) {
                        graphics.drawCenteredString(font, ScpFonts.roboto(name),
                                centerX, centerY - 4, 0xFFE8F8FF);
                    }
                }
            }
        }

        renderTrackers(graphics, floor, originX, originY, scale);
    }

    private void renderTrackers(GuiGraphics graphics, FloorGroup floor,
            double originX, double originY, double scale) {
        for (Scp079PlayableNetwork.TrackerEntry marker
                : Scp079TrackingClientState.markers()) {
            if (!marker.dimension().equals(Scp079PlayableClient.hostDimension())
                    || floor.rooms.stream().noneMatch(room ->
                    room.id().equals(marker.roomId()))) continue;
            int x = (int) Math.round(originX + marker.x() * scale);
            int y = (int) Math.round(originY + marker.z() * scale);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 200.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-marker.yaw()));
            RenderSystem.setShaderColor(1.0F, 0.34F, 0.18F, 1.0F);
            graphics.blit(TRACKER_ARROW, -7, -7, 0.0F, 0.0F,
                    14, 14, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().popPose();
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(Integer.toString(marker.scpNumber())),
                    x, y + 9, 0xFFFF765D);
        }
    }

    private void renderPower(GuiGraphics graphics) {
        int x = width - 178;
        int y = height - 36;
        graphics.drawString(font, ScpFonts.roboto("AUXILIARY POWER"),
                x, y - 12, 0xFFBDEEFF, false);
        graphics.fill(x, y, x + 150, y + 7, 0xC4142732);
        int fill = Math.round(148 * Scp079PlayableClient.power() / 100.0F);
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + 6,
                0xFFBDEEFF);
        graphics.drawString(font, ScpFonts.roboto(
                        Scp079PlayableClient.power() + " / 100"),
                x + 154, y - 1, 0xFFE8F8FF, false);
    }

    private void renderLeaveConfirmation(GuiGraphics graphics,
            int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB8000000);
        int w = Math.min(330, width - 36);
        int h = 126;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, 0xF20A1820);
        border(graphics, x, y, w, h, 0xFF5C8493);
        graphics.drawCenteredString(font, ScpFonts.montserrat("LEAVE SCP ROLE?"),
                x + w / 2, y + 22, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                ScpFonts.roboto("Your original player state will be restored."),
                x + w / 2, y + 45, 0xFF8EAFBA);

        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 38;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        drawModalButton(graphics, cancelX, buttonY, buttonW, 25,
                "CANCEL", inside(mouseX, mouseY,
                        cancelX, buttonY, buttonW, 25), false);
        drawModalButton(graphics, leaveX, buttonY, buttonW, 25,
                "LEAVE ROLE", inside(mouseX, mouseY,
                        leaveX, buttonY, buttonW, 25), true);
    }

    private void drawModalButton(GuiGraphics graphics, int x, int y,
            int w, int h, String label, boolean hovered, boolean danger) {
        int fill = danger
                ? hovered ? 0xE0642828 : 0xD53D2022
                : hovered ? 0xE52A5868 : 0xD518303A;
        int line = danger ? 0xFFE28A7F : 0xFF75B7CC;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        graphics.drawCenteredString(font, ScpFonts.roboto(label),
                x + w / 2, y + 8, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft != null
                && minecraft.options.keyInventory.matchesMouse(button)) {
            if (leaveConfirmation) leaveConfirmation = false;
            else onClose();
            return true;
        }
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (leaveConfirmation) return handleLeaveConfirmationClick(mouseX, mouseY);

        int topY = 22;
        int leaveW = 112;
        int leaveX = width - leaveW - 24;
        if (inside(mouseX, mouseY, leaveX, topY, leaveW, 22)) {
            leaveConfirmation = true;
            return true;
        }

        if (Scp079PlayableClient.cameraMode()) {
            String label = "RETURN TO LOCAL HOST";
            int w = Math.max(126, font.width(ScpFonts.roboto(label)) + 20);
            int x = leaveX - w - 10;
            if (inside(mouseX, mouseY, x, topY, w, 22)) {
                Scp079PlayableNetwork.requestLocal();
                onClose();
                return true;
            }
        }

        if (!floors.isEmpty()) {
            FloorGroup floor = floors.get(floorIndex);
            int w = Math.max(160, font.width(floor.longLabel) + 36);
            int x = (width - w) / 2;
            int y = 21;
            if (mouseX >= x && mouseX < x + w
                    && mouseY >= y && mouseY < y + 22) {
                floorMenuOpen = !floorMenuOpen;
                return true;
            }
            if (floorMenuOpen) {
                int rowY = y + 25;
                int maxRows = Math.min(8, floors.size());
                int start = Math.max(0, Math.min(floorIndex - maxRows / 2,
                        floors.size() - maxRows));
                for (int i = start; i < start + maxRows; i++) {
                    if (mouseX >= x && mouseX < x + w
                            && mouseY >= rowY && mouseY < rowY + 19) {
                        floorIndex = i;
                        floorMenuOpen = false;
                        return true;
                    }
                    rowY += 19;
                }
                floorMenuOpen = false;
            }
        }
        if (hoveredRoom != null && Scp079PlayableClient.networkAvailable()) {
            Scp079PlayableNetwork.requestRoom(hoveredRoom.id());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleLeaveConfirmationClick(double mouseX,
            double mouseY) {
        int w = Math.min(330, width - 36);
        int h = 126;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 38;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        if (inside(mouseX, mouseY, cancelX, buttonY, buttonW, 25)) {
            leaveConfirmation = false;
            return true;
        }
        if (inside(mouseX, mouseY, leaveX, buttonY, buttonW, 25)) {
            ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.HUMAN);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (leaveConfirmation) return true;
        if (floors.size() <= 1 || delta == 0.0D) return true;
        floorIndex = Math.floorMod(floorIndex + (delta > 0 ? -1 : 1),
                floors.size());
        floorMenuOpen = false;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (leaveConfirmation) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE
                    || minecraft != null && minecraft.options.keyInventory.matches(
                    keyCode, scanCode)) {
                leaveConfirmation = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                ScpRoleSelectorNetwork.requestRole(
                        ScpRoleSelectorNetwork.Role.HUMAN);
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(
                keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<FloorGroup> buildFloors() {
        ResourceLocation dimension = Scp079PlayableClient.hostDimension();
        Map<String, List<FacilityRoomSnapshot>> grouped = new LinkedHashMap<>();
        for (FacilityRoomSnapshot room : FacilityMappingClientState.rooms(dimension)) {
            String label = room.floorLongLabel().isBlank()
                    ? "Unassigned Floor" : room.floorLongLabel();
            grouped.computeIfAbsent(label, ignored -> new ArrayList<>()).add(room);
        }
        List<FloorGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<FacilityRoomSnapshot>> entry
                : grouped.entrySet()) {
            int y = entry.getValue().stream().flatMap(room -> room.patches().stream())
                    .mapToInt(FacilityFloorPatch::y).min().orElse(0);
            result.add(new FloorGroup(entry.getKey(), y,
                    List.copyOf(entry.getValue())));
        }
        result.sort(Comparator.comparingInt((FloorGroup floor) -> floor.y)
                .reversed().thenComparing(floor -> floor.longLabel,
                        String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private int initialFloor(List<FloorGroup> options) {
        if (options.isEmpty()) return 0;
        Vec3 view = Scp079PlayableClient.viewPosition();
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(view);
        FacilityRoomSnapshot current = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(), pos);
        if (current != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).rooms.stream().anyMatch(room ->
                        room.id().equals(current.id()))) return i;
            }
        }
        return 0;
    }

    private static boolean roomContainsScreen(FacilityRoomSnapshot room,
            double mouseX, double mouseY, double originX, double originY,
            double scale) {
        for (FacilityFloorPatch patch : room.patches()) {
            double x1 = originX + patch.minX() * scale;
            double y1 = originY + patch.minZ() * scale;
            double x2 = originX + (patch.maxX() + 1) * scale;
            double y2 = originY + (patch.maxZ() + 1) * scale;
            if (mouseX >= x1 && mouseX < x2
                    && mouseY >= y1 && mouseY < y2) return true;
        }
        return false;
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    private static void renderScanlines(GuiGraphics graphics, int width,
            int height) {
        for (int y = 0; y < height; y += 3) {
            graphics.fill(0, y, width, y + 1, 0x17000000);
        }
    }

    private static void renderFrame(GuiGraphics graphics, int width,
            int height) {
        int color = 0xFFBDEEFF;
        int m = 17;
        int l = 24;
        graphics.fill(m, m, m + l, m + 2, color);
        graphics.fill(m, m, m + 2, m + l, color);
        graphics.fill(width - m - l, m, width - m, m + 2, color);
        graphics.fill(width - m - 2, m, width - m, m + l, color);
        graphics.fill(m, height - m - 2, m + l, height - m, color);
        graphics.fill(m, height - m - l, m + 2, height - m, color);
        graphics.fill(width - m - l, height - m - 2,
                width - m, height - m, color);
        graphics.fill(width - m - 2, height - m - l,
                width - m, height - m, color);
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record FloorGroup(String longLabel, int y,
            List<FacilityRoomSnapshot> rooms) {
    }

    private record Bounds(int minX, int minZ, int maxX, int maxZ) {
        private static Bounds of(List<FacilityRoomSnapshot> rooms) {
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
                    : new Bounds(minX, minZ, maxX, maxZ);
        }
    }

    private record RoomBounds(int minX, int minZ, int maxX, int maxZ) {
        private static RoomBounds of(FacilityRoomSnapshot room) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (FacilityFloorPatch patch : room.patches()) {
                minX = Math.min(minX, patch.minX());
                minZ = Math.min(minZ, patch.minZ());
                maxX = Math.max(maxX, patch.maxX());
                maxZ = Math.max(maxZ, patch.maxZ());
            }
            return minX == Integer.MAX_VALUE ? null
                    : new RoomBounds(minX, minZ, maxX, maxZ);
        }
    }
}
