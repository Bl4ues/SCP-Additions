package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingGeometryEditorClient;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringState;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.EditMode;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.safezone.client.SafeZoneClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Small contextual cheat-sheet shared by Creative facility authoring tools. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class BuilderToolGuideHud {
    private static final String AXES = "$AXES$";

    private BuilderToolGuideHud() {
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || !minecraft.player.isCreative() || minecraft.screen != null) {
            return;
        }
        Guide guide = guide(minecraft);
        if (guide == null || guide.lines().isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int keyWidth = 0;
        int actionWidth = 0;
        for (Line line : guide.lines()) {
            keyWidth = Math.max(keyWidth, keyWidth(font, line));
            actionWidth = Math.max(actionWidth, font.width(line.action()));
        }
        int width = Math.max(font.width(guide.title()),
                keyWidth + 8 + actionWidth) + 16;
        int height = 18 + guide.lines().size() * 11 + 6;
        int x = event.getWindow().getGuiScaledWidth() - width - 8;
        int y = 8;

        graphics.fill(x, y, x + width, y + height, 0xA90A0D11);
        graphics.fill(x, y, x + 2, y + height, 0xD9C8A74B);
        graphics.drawString(font, guide.title(), x + 8, y + 6,
                0xFFE7E8EB, false);
        int rowY = y + 18;
        for (Line line : guide.lines()) {
            int keyX = x + 8 + keyWidth - keyWidth(font, line);
            if (AXES.equals(line.key())) {
                drawAxes(graphics, font, keyX, rowY);
            } else {
                graphics.drawString(font, line.key(), keyX, rowY,
                        0xFFD2B65B, false);
            }
            graphics.drawString(font, line.action(), x + 16 + keyWidth,
                    rowY, line.muted() ? 0xFF8F949C : 0xFFB7BBC2, false);
            rowY += 11;
        }
    }

    private static int keyWidth(Font font, Line line) {
        return AXES.equals(line.key()) ? font.width("X Y Z")
                : font.width(line.key());
    }

    private static void drawAxes(GuiGraphics graphics, Font font, int x, int y) {
        graphics.drawString(font, "X", x, y, 0xFFFF514B, false);
        x += font.width("X ");
        graphics.drawString(font, "Y", x, y, 0xFF56FF67, false);
        x += font.width("Y ");
        graphics.drawString(font, "Z", x, y, 0xFF5590FF, false);
    }

    private static Guide guide(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack off = minecraft.player.getOffhandItem();
        if (main.is(TransformConstructionModule.getOffGridTool())
                || off.is(TransformConstructionModule.getOffGridTool())) {
            return offGridGuide();
        }
        if (main.is(TransformConstructionModule.getSurfaceTool())
                || off.is(TransformConstructionModule.getSurfaceTool())) {
            return surfaceGuide();
        }
        if (main.is(FacilityMappingItems.getTool())
                || off.is(FacilityMappingItems.getTool())) {
            return mappingGuide();
        }
        if (main.is(ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get())
                || off.is(ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get())) {
            return safeZoneGuide();
        }
        return null;
    }

    private static Guide offGridGuide() {
        Selection selection = TransformConstructionClientState.selection();
        List<Line> lines = new ArrayList<>();
        if (selection == null || selection.type() != SelectionType.GROUP) {
            lines.add(new Line("RMB", "create local grid"));
            lines.add(new Line("LMB", "select grid"));
            lines.add(new Line("MMB", "copy transformed block"));
        } else {
            EditMode mode = TransformConstructionClientState.mode();
            lines.add(new Line("G", "move mode" + (mode == EditMode.MOVE
                    ? "  •" : "")));
            lines.add(new Line("R", "rotate mode" + (mode == EditMode.ROTATE
                    ? "  •" : "")));
            lines.add(Line.axes("choose axis"));
            lines.add(new Line("LMB", mode == EditMode.ROTATE
                    ? "drag rotation ring" : "drag selected axis"));
            lines.add(new Line("Shift", mode == EditMode.ROTATE
                    ? "snap 5°" : "snap to 1/16"));
            lines.add(new Line("Ctrl+Z", "undo"));
            lines.add(new Line("Del", "delete grid"));
        }
        return new Guide("OFF-GRID CONSTRUCTION", lines);
    }

    private static Guide surfaceGuide() {
        List<Line> lines = new ArrayList<>();
        int step = TransformSurfaceAuthoringState.step();
        if (step == 0) {
            Selection selection = TransformConstructionClientState.selection();
            if (selection != null && selection.type() == SelectionType.SURFACE) {
                lines.add(new Line("LMB", "select / drag handle"));
                if (selection.handle() == SurfaceHandle.CENTER) {
                    lines.add(new Line("Shift", "snap curve handle to 1/16"));
                } else {
                    lines.add(Line.axes("choose movement axis"));
                    lines.add(new Line("Shift", "snap handle to 1/16"));
                }
                lines.add(new Line("F", "flip wall side"));
                lines.add(new Line("V", "bend / rigid aimed block"));
                String angle = angleText(selection);
                if (angle != null) lines.add(new Line("Angle", angle, true));
                lines.add(new Line("Ctrl+Z", "undo"));
                lines.add(new Line("Del", "delete surface"));
            } else {
                lines.add(new Line("RMB", "set baseline point 1"));
                lines.add(new Line("Shift", "snap point to 1/16"));
                lines.add(new Line("LMB", "select existing handle"));
                lines.add(new Line("MMB", "copy transformed block"));
            }
        } else if (step == 1) {
            lines.add(new Line("RMB", "set baseline point 2"));
            lines.add(new Line("Shift", "snap point to 1/16"));
            lines.add(new Line("Esc", "cancel"));
        } else {
            lines.add(new Line("RMB", "set height and create"));
            lines.add(new Line("Shift", "snap height to 1/16"));
            lines.add(new Line("Esc", "cancel"));
        }
        return new Guide("SURFACE CONSTRUCTION", lines);
    }

    private static String angleText(Selection selection) {
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        if (surface == null) return null;
        SurfaceHandle handle = TransformConstructionClientState.hoveredSurfaceId()
                != null && TransformConstructionClientState.hoveredSurfaceId()
                .equals(surface.id())
                ? TransformConstructionClientState.hoveredSurfaceHandle()
                : selection.handle();
        if (handle == SurfaceHandle.CENTER) {
            Vec3 a = surface.gridTangent(0.0D, 0.5D).normalize();
            Vec3 b = surface.gridTangent(1.0D, 0.5D).normalize();
            double dot = Mth.clamp(a.dot(b), -1.0D, 1.0D);
            return String.format(java.util.Locale.ROOT, "curve %.1f°",
                    Math.toDegrees(Math.acos(dot)));
        }
        Vec3 delta = switch (handle) {
            case BOTTOM_START, BOTTOM_END, BOTTOM_EDGE ->
                    surface.bottomEnd().subtract(surface.bottomStart());
            case TOP_START, TOP_END, TOP_EDGE ->
                    surface.topEnd().subtract(surface.topStart());
            case START_EDGE -> surface.topStart().subtract(surface.bottomStart());
            case END_EDGE -> surface.topEnd().subtract(surface.bottomEnd());
            case CENTER -> Vec3.ZERO;
        };
        if (delta.lengthSqr() < 1.0E-8D) return null;
        Vec3 n = delta.normalize();
        double max = Math.max(Math.abs(n.x),
                Math.max(Math.abs(n.y), Math.abs(n.z)));
        double deviation = Math.toDegrees(Math.acos(
                Mth.clamp(max, -1.0D, 1.0D)));
        return String.format(java.util.Locale.ROOT, "%.1f° from axis", deviation);
    }

    private static Guide mappingGuide() {
        List<Line> lines = new ArrayList<>();
        if (FacilityMappingGeometryEditorClient.isEditing()) {
            lines.add(new Line("LMB", "select / drag vertex"));
            lines.add(new Line("I", "split nearest edge"));
            lines.add(new Line("C", "curve nearest edge"));
            lines.add(new Line("Del", "remove vertex"));
            lines.add(new Line("G / Esc", "finish precision edit"));
            lines.add(new Line("Shift", "snap drag to 1/16"));
        } else if (FacilityMappingClientState.cameraLinkSelection() != null) {
            lines.add(new Line("LMB", "assign camera to room"));
            lines.add(new Line("Shift+RMB", "cancel"));
        } else if (FacilityMappingClientState.selectionStart() != null) {
            lines.add(new Line("RMB", "finish room rectangle"));
            lines.add(new Line("Shift+RMB", "cancel"));
        } else {
            lines.add(new Line("LMB", "room corner 1 / select camera"));
            lines.add(new Line("G", "precision-edit aimed room"));
            lines.add(new Line("Shift+RMB", "room settings / detach camera"));
        }
        return new Guide("FACILITY MAPPING", lines);
    }

    private static Guide safeZoneGuide() {
        List<Line> lines = new ArrayList<>();
        if (SafeZoneClientState.selectionStart() == null) {
            lines.add(new Line("LMB", "set first corner"));
            lines.add(new Line("Shift+RMB", "edit existing zone"));
        } else {
            lines.add(new Line("RMB", "create Safe Zone"));
            lines.add(new Line("Shift+RMB", "cancel"));
        }
        return new Guide("SAFE ZONE", lines);
    }

    private record Guide(String title, List<Line> lines) {
    }

    private record Line(String key, String action, boolean muted) {
        private Line(String key, String action) {
            this(key, action, false);
        }

        private static Line axes(String action) {
            return new Line(AXES, action, false);
        }
    }
}
