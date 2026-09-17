package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingGeometryEditorClient;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringState;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.safezone.client.SafeZoneClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
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
            keyWidth = Math.max(keyWidth, font.width(line.key()));
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
            graphics.drawString(font, line.key(), x + 8 + keyWidth
                    - font.width(line.key()), rowY, 0xFFD2B65B, false);
            graphics.drawString(font, line.action(), x + 16 + keyWidth,
                    rowY, 0xFFB7BBC2, false);
            rowY += 11;
        }
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
            lines.add(new Line("G / LMB", "move / drag gizmo"));
            lines.add(new Line("R", "rotate"));
            lines.add(new Line("X Y Z", "choose axis"));
            lines.add(new Line("Shift+Wheel", "precise step"));
            lines.add(new Line("Del", "delete grid"));
            lines.add(new Line("Hotbar", "switch to blocks and place"));
        }
        return new Guide("OFF-GRID CONSTRUCTION", lines);
    }

    private static Guide surfaceGuide() {
        List<Line> lines = new ArrayList<>();
        int step = TransformSurfaceAuthoringState.step();
        if (step == 0) {
            Selection selection = TransformConstructionClientState.selection();
            if (selection != null && selection.type() == SelectionType.SURFACE) {
                lines.add(new Line("LMB", "select handle"));
                lines.add(new Line("G / LMB", "move / drag"));
                if (selection.handle() == SurfaceHandle.CENTER) {
                    lines.add(new Line("Shift+Wheel", "curve depth"));
                } else {
                    lines.add(new Line("X Y Z", "choose axis"));
                    lines.add(new Line("Shift+Wheel", "precise step"));
                }
                lines.add(new Line("Del", "delete surface"));
                lines.add(new Line("Hotbar", "switch to blocks and place"));
            } else {
                lines.add(new Line("RMB", "set baseline point 1"));
                lines.add(new Line("LMB", "select existing surface"));
                lines.add(new Line("MMB", "copy transformed block"));
            }
        } else if (step == 1) {
            lines.add(new Line("RMB", "set baseline point 2"));
            lines.add(new Line("Shift+RMB", "cancel"));
        } else {
            lines.add(new Line("RMB", "set height and create"));
            lines.add(new Line("Shift+RMB", "cancel"));
        }
        return new Guide("SURFACE CONSTRUCTION", lines);
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

    private record Line(String key, String action) {
    }
}
