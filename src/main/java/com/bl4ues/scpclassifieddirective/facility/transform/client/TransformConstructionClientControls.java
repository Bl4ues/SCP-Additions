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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Blockbench-style keyboard and direct-axis editing for world-space gizmos. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformConstructionClientControls {
    private static DragState drag;

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

        Vec3 hit = Vec3.atCenterOf(event.getPos());
        HitResult currentHit = minecraft.hitResult;
        if (currentHit instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(event.getPos())) {
            hit = blockHit.getLocation();
        }
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
        if (!holdingEditorTool(player)) return;

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_G -> {
                finishDrag();
                TransformConstructionClientState.setMode(EditMode.MOVE);
                status("Move");
            }
            case GLFW.GLFW_KEY_R -> {
                finishDrag();
                TransformConstructionClientState.setMode(EditMode.ROTATE);
                status("Rotate");
            }
            case GLFW.GLFW_KEY_X -> {
                finishDrag();
                TransformConstructionClientState.setAxis(Axis.X);
                status("Axis X");
            }
            case GLFW.GLFW_KEY_Y -> {
                finishDrag();
                TransformConstructionClientState.setAxis(Axis.Y);
                status("Axis Y");
            }
            case GLFW.GLFW_KEY_Z -> {
                finishDrag();
                TransformConstructionClientState.setAxis(Axis.Z);
                status("Axis Z");
            }
            case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> {
                finishDrag();
                deleteSelection();
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                finishDrag();
                TransformConstructionClientState.clearSelection();
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Selection selection = TransformConstructionClientState.selection();
        if (player == null || selection == null || minecraft.screen != null
                || !player.isCreative() || !holdingEditorTool(player)
                || TransformConstructionClientState.mode() != EditMode.MOVE) {
            finishDrag();
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            finishDrag();
            return;
        }

        Vec3 axis = dragAxis(selection);
        Vec3 handle = selection.type() == SelectionType.GROUP
                ? groupHandle(selection) : surfaceHandle(selection);
        if (handle == null || axis == null || axis.lengthSqr() < 1.0E-8D) {
            finishDrag();
            return;
        }
        axis = axis.normalize();
        double parameter = axisParameter(player.getEyePosition(),
                player.getViewVector(1.0F), handle, axis);
        if (!Double.isFinite(parameter)) return;
        if (drag == null || !drag.matches(selection)) {
            drag = DragState.begin(selection, parameter);
            return;
        }

        double delta = parameter - drag.startParameter();
        if (player.isShiftKeyDown()) {
            delta = Math.rint(delta * 16.0D) / 16.0D;
        }
        if (Math.abs(delta - drag.lastDelta()) < 1.0E-5D) return;
        drag = drag.withLastDelta(delta);
        Vec3 movement = axis.scale(delta);
        if (selection.type() == SelectionType.GROUP) {
            previewGroupDrag(drag, movement);
        } else {
            previewSurfaceDrag(drag, movement);
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
                || !player.isCreative() || !holdingEditorTool(player)
                || !player.isShiftKeyDown()) {
            // Ordinary wheel input must remain ordinary hotbar scrolling. Tool
            // edits deliberately require Shift so authoring never traps the
            // selected slot in the player's hand.
            return;
        }

        finishDrag();
        double sign = scroll > 0.0D ? 1.0D : -1.0D;
        if (selection.type() == SelectionType.GROUP) {
            editGroup(selection, sign);
        } else {
            editSurface(selection, sign);
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
            finishDrag();
            TransformConstructionClientState.selectGroup(best.id());
            status("Off-grid grid selected");
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
            finishDrag();
            TransformConstructionClientState.selectSurface(bestSurface.id(),
                    bestHandle);
            status(bestHandle == SurfaceHandle.CENTER
                    ? "Curve handle selected" : "Surface handle selected");
        }
    }

    private static void editGroup(Selection selection, double sign) {
        TransformGroup group = TransformConstructionClientState.group(selection.id());
        if (group == null) return;
        if (TransformConstructionClientState.mode() == EditMode.ROTATE) {
            float step = (float) (sign * 5.0D);
            float x = group.rotationX();
            float y = group.rotationY();
            float z = group.rotationZ();
            switch (TransformConstructionClientState.axis()) {
                case X -> x += step;
                case Y -> y += step;
                case Z -> z += step;
            }
            TransformConstructionClientState.upsertGroup(group.withTransform(
                    group.origin(), x, y, z));
            TransformConstructionNetwork.updateGroup(group.id(), group.origin(),
                    x, y, z);
        } else {
            double step = sign / 16.0D;
            Vec3 origin = group.origin().add(
                    TransformConstructionClientState.axisVector().scale(step));
            TransformConstructionClientState.upsertGroup(group.withTransform(origin,
                    group.rotationX(), group.rotationY(), group.rotationZ()));
            TransformConstructionNetwork.updateGroup(group.id(), origin,
                    group.rotationX(), group.rotationY(), group.rotationZ());
        }
    }

    private static void editSurface(Selection selection, double sign) {
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        if (surface == null) return;
        if (TransformConstructionClientState.mode() == EditMode.ROTATE) {
            status("Use the corner handles to tilt the surface");
            return;
        }
        Vec3 axis = selection.handle() == SurfaceHandle.CENTER
                ? surface.gridNormal(0.5D, 0.5D)
                : TransformConstructionClientState.axisVector();
        applySurfaceDelta(surface, selection.handle(),
                axis.normalize().scale(sign / 16.0D), true);
    }

    private static Vec3 dragAxis(Selection selection) {
        if (selection.type() != SelectionType.SURFACE
                || selection.handle() != SurfaceHandle.CENTER) {
            return TransformConstructionClientState.axisVector();
        }
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        return surface == null ? null : surface.gridNormal(0.5D, 0.5D);
    }

    private static void previewGroupDrag(DragState state, Vec3 movement) {
        if (state.baseGroup() == null) return;
        TransformGroup base = state.baseGroup();
        TransformConstructionClientState.upsertGroup(base.withTransform(
                base.origin().add(movement), base.rotationX(), base.rotationY(),
                base.rotationZ()));
    }

    private static void previewSurfaceDrag(DragState state, Vec3 movement) {
        if (state.baseSurface() == null) return;
        applySurfaceDelta(state.baseSurface(), state.selection().handle(),
                movement, false);
    }

    private static void applySurfaceDelta(ConstructionSurface surface,
            SurfaceHandle handle, Vec3 delta, boolean send) {
        Vec3 bs = surface.bottomStart();
        Vec3 be = surface.bottomEnd();
        Vec3 ts = surface.topStart();
        Vec3 te = surface.topEnd();
        Vec3 curve = surface.curveOffset();
        switch (handle) {
            case BOTTOM_START -> bs = bs.add(delta);
            case BOTTOM_END -> be = be.add(delta);
            case TOP_START -> ts = ts.add(delta);
            case TOP_END -> te = te.add(delta);
            // A quadratic control contributes 1/2 of its offset at u=.5, so
            // double movement keeps the visible center handle tracking 1:1.
            case CENTER -> curve = curve.add(delta.scale(2.0D));
        }
        TransformConstructionClientState.upsertSurface(surface.withGeometry(
                bs, be, ts, te, curve));
        if (send) {
            TransformConstructionNetwork.updateSurface(surface.id(), bs, be, ts,
                    te, curve);
        }
    }

    private static void finishDrag() {
        if (drag == null) return;
        Selection selection = drag.selection();
        if (Math.abs(drag.lastDelta()) > 1.0E-5D) {
            if (selection.type() == SelectionType.GROUP) {
                TransformGroup current = TransformConstructionClientState.group(
                        selection.id());
                if (current != null) {
                    TransformConstructionNetwork.updateGroup(current.id(),
                            current.origin(), current.rotationX(),
                            current.rotationY(), current.rotationZ());
                }
            } else {
                ConstructionSurface current =
                        TransformConstructionClientState.surface(selection.id());
                if (current != null) {
                    TransformConstructionNetwork.updateSurface(current.id(),
                            current.bottomStart(), current.bottomEnd(),
                            current.topStart(), current.topEnd(),
                            current.curveOffset());
                }
            }
        }
        drag = null;
    }

    private static Vec3 groupHandle(Selection selection) {
        TransformGroup group = TransformConstructionClientState.group(selection.id());
        return group == null ? null : group.origin();
    }

    private static Vec3 surfaceHandle(Selection selection) {
        ConstructionSurface surface = TransformConstructionClientState.surface(
                selection.id());
        return surface == null ? null : handlePosition(surface, selection.handle());
    }

    private static double axisParameter(Vec3 eye, Vec3 view, Vec3 origin,
            Vec3 axis) {
        Vec3 u = axis.normalize();
        Vec3 v = view.normalize();
        Vec3 w = origin.subtract(eye);
        double b = u.dot(v);
        double denominator = 1.0D - b * b;
        if (Math.abs(denominator) < 1.0E-6D) {
            return eye.subtract(origin).dot(u);
        }
        double d = u.dot(w);
        double e = v.dot(w);
        return (b * e - d) / denominator;
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

    private static boolean holdingEditorTool(LocalPlayer player) {
        return player.getMainHandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                || player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
    }

    private static void status(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), true);
    }

    private record DragState(Selection selection, double startParameter,
            double lastDelta, TransformGroup baseGroup,
            ConstructionSurface baseSurface) {
        private static DragState begin(Selection selection, double parameter) {
            return new DragState(selection, parameter, 0.0D,
                    selection.type() == SelectionType.GROUP
                            ? TransformConstructionClientState.group(selection.id())
                            : null,
                    selection.type() == SelectionType.SURFACE
                            ? TransformConstructionClientState.surface(selection.id())
                            : null);
        }

        private boolean matches(Selection other) {
            return other != null && selection.type() == other.type()
                    && selection.id().equals(other.id())
                    && selection.handle() == other.handle();
        }

        private DragState withLastDelta(double delta) {
            return new DragState(selection, startParameter, delta, baseGroup,
                    baseSurface);
        }
    }
}
