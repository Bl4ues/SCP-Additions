package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Axis;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.EditMode;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Blockbench-style keyboard editing for the physical world-space gizmos. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformConstructionClientControls {
    private TransformConstructionClientControls() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getEntity() != player
                || event.getHand() != InteractionHand.MAIN_HAND
                || minecraft.screen != null || !player.isCreative()) return;
        boolean offGrid = player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool());
        boolean surfaceTool = player.getMainHandItem().is(
                TransformConstructionModule.getSurfaceTool());
        if (!offGrid && !surfaceTool) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(
                TransformConstructionModule.getProxy())) return;

        Vec3 hit = event.getHitVec().getLocation();
        if (offGrid) selectNearestGroup(hit);
        else selectNearestSurface(hit);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isCreative()
                || TransformConstructionClientState.selection() == null) return;
        boolean editing = player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                || player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool());
        if (!editing) return;

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_G -> {
                TransformConstructionClientState.setMode(EditMode.MOVE);
                status("Move mode");
            }
            case GLFW.GLFW_KEY_R -> {
                TransformConstructionClientState.setMode(EditMode.ROTATE);
                status("Rotate mode");
            }
            case GLFW.GLFW_KEY_X -> {
                TransformConstructionClientState.setAxis(Axis.X);
                status("Axis X");
            }
            case GLFW.GLFW_KEY_Y -> {
                TransformConstructionClientState.setAxis(Axis.Y);
                status("Axis Y");
            }
            case GLFW.GLFW_KEY_Z -> {
                TransformConstructionClientState.setAxis(Axis.Z);
                status("Axis Z");
            }
            case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> deleteSelection();
            case GLFW.GLFW_KEY_ESCAPE -> TransformConstructionClientState.clearSelection();
            default -> { }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        double scroll = event.getScrollDelta();
        if (scroll == 0.0D) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Selection selection = TransformConstructionClientState.selection();
        if (player == null || selection == null || minecraft.screen != null
                || !player.isCreative()) return;
        boolean offGrid = player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool());
        boolean surfaceTool = player.getMainHandItem().is(
                TransformConstructionModule.getSurfaceTool());
        if (!offGrid && !surfaceTool) return;

        double sign = scroll > 0.0D ? 1.0D : -1.0D;
        boolean shift = player.isShiftKeyDown();
        if (selection.type() == SelectionType.GROUP) {
            editGroup(selection, sign, shift);
        } else {
            editSurface(selection, sign, shift);
        }
        event.setCanceled(true);
    }

    private static void selectNearestGroup(Vec3 hit) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        TransformGroup best = null;
        double bestDistance = 4.0D;
        for (TransformGroup group : TransformConstructionClientState.groups(
                minecraft.level.dimension().location())) {
            Vec3 local = TransformMath.worldToLocal(group.origin(), hit,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (TransformGroup.GridPos cell : group.cells().keySet()) {
                double distance = local.distanceToSqr(new Vec3(cell.x(), cell.y(),
                        cell.z()));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = group;
                }
            }
        }
        if (best != null) {
            TransformConstructionClientState.selectGroup(best.id());
            status("Off-grid grid selected: G/R, X/Y/Z, mouse wheel");
        }
    }

    private static void selectNearestSurface(Vec3 hit) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ConstructionSurface bestSurface = null;
        SurfaceHandle bestHandle = SurfaceHandle.CENTER;
        double bestDistance = 6.25D;
        for (ConstructionSurface surface : TransformConstructionClientState.surfaces(
                minecraft.level.dimension().location())) {
            for (SurfaceHandle handle : SurfaceHandle.values()) {
                double distance = handlePosition(surface, handle).distanceToSqr(hit);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSurface = surface;
                    bestHandle = handle;
                }
            }
            if (bestSurface == null) {
                for (int i = 0; i <= 16; i++) {
                    double u = i / 16.0D;
                    for (int j = 0; j <= 8; j++) {
                        double v = j / 8.0D;
                        double distance = surface.gridPoint(u, v).distanceToSqr(hit);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestSurface = surface;
                            bestHandle = SurfaceHandle.CENTER;
                        }
                    }
                }
            }
        }
        if (bestSurface != null) {
            TransformConstructionClientState.selectSurface(bestSurface.id(),
                    bestHandle);
            status("Surface handle selected: G, X/Y/Z, mouse wheel");
        }
    }

    private static void editGroup(Selection selection, double sign,
            boolean shift) {
        TransformGroup group = TransformConstructionClientState.group(selection.id());
        if (group == null) return;
        if (TransformConstructionClientState.mode() == EditMode.ROTATE) {
            float step = (float) (sign * (shift ? 15.0D : 5.0D));
            float x = group.rotationX();
            float y = group.rotationY();
            float z = group.rotationZ();
            switch (TransformConstructionClientState.axis()) {
                case X -> x += step;
                case Y -> y += step;
                case Z -> z += step;
            }
            TransformGroup preview = group.withTransform(group.origin(), x, y, z);
            TransformConstructionClientState.upsertGroup(preview);
            TransformConstructionNetwork.updateGroup(group.id(), group.origin(),
                    x, y, z);
        } else {
            double step = sign * (shift ? 0.5D : 1.0D / 16.0D);
            Vec3 origin = group.origin().add(
                    TransformConstructionClientState.axisVector().scale(step));
            TransformGroup preview = group.withTransform(origin,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            TransformConstructionClientState.upsertGroup(preview);
            TransformConstructionNetwork.updateGroup(group.id(), origin,
                    group.rotationX(), group.rotationY(), group.rotationZ());
        }
    }

    private static void editSurface(Selection selection, double sign,
            boolean shift) {
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        if (surface == null) return;
        if (TransformConstructionClientState.mode() == EditMode.ROTATE) {
            status("Surface angle is edited by moving its corner handles");
            return;
        }
        double step = sign * (shift ? 0.5D : 1.0D / 16.0D);
        Vec3 delta = TransformConstructionClientState.axisVector().scale(step);
        Vec3 bs = surface.bottomStart();
        Vec3 be = surface.bottomEnd();
        Vec3 ts = surface.topStart();
        Vec3 te = surface.topEnd();
        Vec3 curve = surface.curveOffset();
        switch (selection.handle()) {
            case BOTTOM_START -> bs = bs.add(delta);
            case BOTTOM_END -> be = be.add(delta);
            case TOP_START -> ts = ts.add(delta);
            case TOP_END -> te = te.add(delta);
            case CENTER -> curve = curve.add(delta);
        }
        ConstructionSurface preview = surface.withGeometry(bs, be, ts, te, curve);
        TransformConstructionClientState.upsertSurface(preview);
        TransformConstructionNetwork.updateSurface(surface.id(), bs, be, ts, te,
                curve);
    }

    private static Vec3 handlePosition(ConstructionSurface surface,
            SurfaceHandle handle) {
        return switch (handle) {
            case BOTTOM_START -> surface.bottomStart();
            case BOTTOM_END -> surface.bottomEnd();
            case TOP_START -> surface.topStart();
            case TOP_END -> surface.topEnd();
            case CENTER -> surface.gridPoint(0.5D, 0.5D);
        };
    }

    private static void deleteSelection() {
        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        TransformConstructionNetwork.delete(selection.id(),
                selection.type() == SelectionType.SURFACE);
        TransformConstructionClientState.remove(selection.id(),
                selection.type() == SelectionType.SURFACE);
    }

    private static void status(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(text), true);
        }
    }
}
