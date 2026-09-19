package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringState;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Axis;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.EditMode;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/** Blockbench-style keyboard and direct-axis editing for world-space gizmos. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformConstructionClientControls {
    private static final double HANDLE_BASE_RADIUS = 0.24D;
    private static final double HANDLE_MAX_DISTANCE = 32.0D;
    private static DragState drag;
    private static boolean attackLatch;
    private static int suppressPauseTicks;

    private TransformConstructionClientControls() {
    }
    /**
     * During handle dragging only the edit guides change. The payload mesh
     * and collision are updated once the gesture is committed.
     */
    static boolean previewingSurface(UUID id) {
        return id != null && drag != null && drag.remembered()
                && drag.selection().type() == SelectionType.SURFACE
                && id.equals(drag.selection().id());
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
        else if (TransformSurfaceAuthoringState.step() == 0) {
            AimedSurface aimed = findAimedSurface(player);
            if (aimed != null) {
                finishDrag();
                TransformConstructionClientState.selectSurface(
                        aimed.surfaceId(), SurfaceHandle.CENTER);
                TransformConstructionClientState.setHoveredSurface(
                        aimed.surfaceId(), SurfaceHandle.CENTER);
                status("Surface selected");
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseEditorTool(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative() || !holdingOffGridTool(player)) return;

        UUID aimedGroup = TransformGroupPlacementClient.findAimedGroup(player);
        if (aimedGroup == null) return;

        finishDrag();
        TransformConstructionClientState.selectGroup(aimedGroup);
        status("Off-grid group selected");
        // Do not allow the vanilla block behind a clipped transformed cell to
        // receive the same RMB and create another grid accidentally.
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isCreative()
                || !holdingEditorTool(player)) return;

        // LMB is an editor gesture for these tools, never a normal attack.
        // Cancel every generated attack trigger, including air hits, and only
        // process the press edge once while the mouse is held.
        event.setCanceled(true);
        if (attackLatch) return;
        attackLatch = true;

        Selection currentSelection = TransformConstructionClientState.selection();
        if (currentSelection != null
                && currentSelection.type() == SelectionType.GROUP) {
            Axis directAxis = findGroupGizmoAxis(player, currentSelection);
            if (directAxis != null) {
                finishDrag();
                TransformConstructionClientState.setAxis(directAxis);
                return;
            }
        }
        if (holdingOffGridTool(player)) {
            UUID aimedGroup = TransformGroupPlacementClient.findAimedGroup(player);
            if (aimedGroup != null) {
                finishDrag();
                TransformConstructionClientState.selectGroup(aimedGroup);
                status("Off-grid group selected");
                return;
            }
            HitResult aimed = minecraft.hitResult;
            if (aimed instanceof BlockHitResult blockHit
                    && aimed.getType() == HitResult.Type.BLOCK
                    && minecraft.level.getBlockState(blockHit.getBlockPos()).is(
                            TransformConstructionModule.getProxy())) {
                selectNearestGroup(blockHit.getLocation());
                return;
            }
        }

        if (currentSelection != null
                && currentSelection.type() == SelectionType.SURFACE) {
            Axis directAxis = findSurfaceGizmoAxis(player, currentSelection);
            if (directAxis != null) {
                finishDrag();
                TransformConstructionClientState.setAxis(directAxis);
                return;
            }
        }

        boolean selectedSurface = false;
        if (holdingSurfaceTool(player)
                && TransformSurfaceAuthoringState.step() == 0) {
            HoveredHandle hovered = findHoveredSurfaceHandle(player);
            if (hovered != null) {
                finishDrag();
                TransformConstructionClientState.selectSurface(
                        hovered.surfaceId(), hovered.handle());
                TransformConstructionClientState.setHoveredSurface(
                        hovered.surfaceId(), hovered.handle());
                selectedSurface = true;
            } else {
                AimedSurface aimed = findAimedSurface(player);
                if (aimed != null) {
                    finishDrag();
                    TransformConstructionClientState.selectSurface(
                            aimed.surfaceId(), SurfaceHandle.CENTER);
                    TransformConstructionClientState.setHoveredSurface(
                            aimed.surfaceId(), SurfaceHandle.CENTER);
                    status("Surface selected");
                    selectedSurface = true;
                }
            }
        }

        // Cancellation is unconditional above while an editor tool is held.
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!(event.getNewScreen() instanceof PauseScreen)
                || player == null || !player.isCreative()
                || !holdingEditorTool(player)) return;

        // ESC is an editor cancel while a construction gesture/selection is
        // active. Keyboard handling clears the selection before Minecraft asks
        // to open PauseScreen, so keep a tiny one-tick latch to suppress that
        // follow-up screen (and its pause/audio transition) without stealing
        // ESC when the tool is merely being held.
        if (suppressPauseTicks > 0) {
            suppressPauseTicks = 0;
            event.setCanceled(true);
            return;
        }

        if (!holdingSurfaceTool(player)
                || !TransformSurfaceAuthoringState.active()) return;
        finishDrag();
        TransformSurfaceAuthoringState.clear();
        TransformConstructionNetwork.cancelSurfaceAuthoring();
        status("Surface selection cancelled");
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isCreative()
                || !holdingEditorTool(player)) return;

        if (event.getKey() == GLFW.GLFW_KEY_Z && Screen.hasControlDown()) {
            finishDrag();
            status(TransformConstructionClientState.undoLast()
                    ? "Undo" : "Nothing to undo");
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE
                && TransformSurfaceAuthoringState.active()) {
            suppressPauseTicks = 2;
            finishDrag();
            TransformSurfaceAuthoringState.clear();
            TransformConstructionNetwork.cancelSurfaceAuthoring();
            status("Surface selection cancelled");
            return;
        }

        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        switch (event.getKey()) {
            case GLFW.GLFW_KEY_G -> {
                finishDrag();
                TransformConstructionClientState.setMode(EditMode.MOVE);
                status("Move mode");
            }
            case GLFW.GLFW_KEY_R -> {
                finishDrag();
                if (selection.type() == SelectionType.GROUP) {
                    TransformConstructionClientState.setMode(EditMode.ROTATE);
                    status("Rotate mode");
                } else {
                    status("Tilt surfaces with their edge and corner handles");
                }
            }
            case GLFW.GLFW_KEY_X -> {
                finishDrag();
                TransformConstructionClientState.setAxis(Axis.X);
                status("X axis");
            }
            case GLFW.GLFW_KEY_Y -> {
                finishDrag();
                TransformConstructionClientState.setAxis(Axis.Y);
                status("Y axis");
            }
            case GLFW.GLFW_KEY_Z -> {
                finishDrag();
                TransformConstructionClientState.setAxis(Axis.Z);
                status("Z axis");
            }
            case GLFW.GLFW_KEY_L -> {
                finishDrag();
                // L is also Minecraft's default Advancements key. While an
                // authoring tool owns this press it must remain an editor
                // command instead of opening another screen on top of the gizmo.
                while (minecraft.options.keyAdvancements.consumeClick()) {
                    // Drain the click queued by this physical key press.
                }
                minecraft.options.keyAdvancements.setDown(false);
                TransformConstructionClientState.toggleTransformSpace();
                status(TransformConstructionClientState.transformSpace()
                        == TransformConstructionClientState.TransformSpace.LOCAL
                        ? "Local transform space" : "Global transform space");
            }
            case GLFW.GLFW_KEY_C -> {
                finishDrag();
                if (selection.type() == SelectionType.SURFACE) {
                    TransformConstructionClientState.toggleSurfaceCurveAxis();
                    status(TransformConstructionClientState.surfaceCurveAxis()
                            == TransformConstructionClientState.SurfaceCurveAxis.WIDTH
                            ? "Curve axis: width" : "Curve axis: height");
                }
            }
            case GLFW.GLFW_KEY_F -> {
                finishDrag();
                if (selection.type() == SelectionType.SURFACE) {
                    // F belongs to the Surface editor here, not vanilla's
                    // swap-offhand action. Drain the queued KeyMapping click so
                    // the tool does not jump hands while flipping placement.
                    while (minecraft.options.keySwapOffhand.consumeClick()) {
                        // Drain all repeats generated by this physical press.
                    }
                    minecraft.options.keySwapOffhand.setDown(false);
                    ConstructionSurface surface =
                            TransformConstructionClientState.surface(selection.id());
                    if (surface != null) {
                        TransformConstructionClientState.remember(selection);
                        ConstructionSurface next =
                                surface.withFlipped(!surface.flipped());
                        TransformConstructionClientState.upsertSurface(next);
                        TransformConstructionNetwork.setSurfaceFlipped(next.id(),
                                next.flipped());
                        status(next.flipped()
                                ? "Placement side flipped" : "Placement side normal");
                    }
                }
            }
            case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> {
                finishDrag();
                deleteSelection();
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                suppressPauseTicks = 2;
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
        if (player == null) return;
        if (suppressPauseTicks > 0) suppressPauseTicks--;
        if (!minecraft.options.keyAttack.isDown()) attackLatch = false;

        updateSurfaceHover(player);

        Selection selection = TransformConstructionClientState.selection();
        if (selection == null || minecraft.screen != null
                || !player.isCreative() || !holdingEditorTool(player)) {
            finishDrag();
            return;
        }
        if (!minecraft.options.keyAttack.isDown()) {
            finishDrag();
            return;
        }

        if (TransformConstructionClientState.mode() == EditMode.ROTATE
                && selection.type() == SelectionType.GROUP) {
            tickRotationDrag(player, selection);
        } else {
            tickMoveDrag(player, selection);
        }
    }

    private static void tickMoveDrag(LocalPlayer player, Selection selection) {
        if (drag == null || !drag.matches(selection, EditMode.MOVE,
                TransformConstructionClientState.axis())) {
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
            drag = DragState.begin(selection, EditMode.MOVE,
                    TransformConstructionClientState.axis(), parameter,
                    handle, axis);
            return;
        }

        // The drag frame is frozen on mouse-down. Recomputing the handle or
        // surface normal from the preview we just moved creates a feedback
        // loop, which is visible as jitter on angled/curved construction.
        double parameter = axisParameter(player.getEyePosition(),
                player.getViewVector(1.0F), drag.anchor(), drag.dragAxis());
        if (!Double.isFinite(parameter)) return;
        double delta = parameter - drag.startParameter();
        if (Math.abs(delta - drag.lastDelta()) < 1.0E-5D) return;
        rememberDragIfNeeded();
        drag = drag.withLastDelta(delta);
        Vec3 movement = drag.dragAxis().scale(delta);
        if (selection.type() == SelectionType.GROUP) {
            previewGroupDrag(drag, movement, player.isShiftKeyDown());
        } else {
            previewSurfaceDrag(drag, movement, player.isShiftKeyDown());
        }
    }

    private static void tickRotationDrag(LocalPlayer player,
            Selection selection) {
        TransformGroup group = TransformConstructionClientState.group(
                selection.id());
        if (group == null) {
            finishDrag();
            return;
        }
        Vec3 axis = gizmoAxisDirection(selection,
                TransformConstructionClientState.axis());
        if (drag != null && drag.matches(selection, EditMode.ROTATE,
                TransformConstructionClientState.axis())) {
            axis = drag.dragAxis();
        }
        double angle = rotationParameter(player.getEyePosition(),
                player.getViewVector(1.0F), group.origin(), axis);
        if (!Double.isFinite(angle)) return;
        if (drag == null || !drag.matches(selection, EditMode.ROTATE,
                TransformConstructionClientState.axis())) {
            drag = DragState.begin(selection, EditMode.ROTATE,
                    TransformConstructionClientState.axis(), angle,
                    group.origin(), axis);
            return;
        }

        double delta = Mth.wrapDegrees(Math.toDegrees(
                angle - drag.startParameter()));
        if (player.isShiftKeyDown()) {
            delta = Math.rint(delta / 5.0D) * 5.0D;
        }
        if (Math.abs(delta - drag.lastDelta()) < 1.0E-4D) return;
        rememberDragIfNeeded();
        drag = drag.withLastDelta(delta);
        TransformGroup base = drag.baseGroup();
        if (base == null) return;
        Vec3 worldAxis = drag.dragAxis().normalize();
        boolean local = TransformConstructionClientState.transformSpace()
                == TransformConstructionClientState.TransformSpace.LOCAL;
        Vec3 compositionAxis = local
                ? TransformMath.inverseRotate(worldAxis, base.rotationX(),
                        base.rotationY(), base.rotationZ()).normalize()
                : worldAxis;
        float[] rotation = TransformMath.composeAxisRotation(
                base.rotationX(), base.rotationY(), base.rotationZ(),
                compositionAxis, (float) delta, local);
        TransformConstructionClientState.previewGroup(base.withTransform(
                base.origin(), rotation[0], rotation[1], rotation[2]));
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
                || !player.isShiftKeyDown()) return;

        finishDrag();
        TransformConstructionClientState.remember(selection);
        double sign = scroll > 0.0D ? 1.0D : -1.0D;
        if (selection.type() == SelectionType.GROUP) {
            editGroup(selection, sign);
        } else {
            editSurface(selection, sign);
        }
        event.setCanceled(true);
    }

    private static void updateSurfaceHover(LocalPlayer player) {
        if (!holdingSurfaceTool(player)
                || TransformSurfaceAuthoringState.step() != 0) {
            TransformConstructionClientState.clearHoveredSurface();
            return;
        }
        HoveredHandle hovered = findHoveredSurfaceHandle(player);
        if (hovered == null) {
            TransformConstructionClientState.clearHoveredSurface();
        } else {
            TransformConstructionClientState.setHoveredSurface(
                    hovered.surfaceId(), hovered.handle());
        }
    }

    private static HoveredHandle findHoveredSurfaceHandle(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F).normalize();
        HoveredHandle best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location())) {
            for (SurfaceHandle handle : SurfaceHandle.values()) {
                Vec3 point = handlePosition(surface, handle);
                double rayDistance = point.subtract(eye).dot(view);
                if (rayDistance < 0.0D || rayDistance > HANDLE_MAX_DISTANCE) {
                    continue;
                }
                Vec3 rayPoint = eye.add(view.scale(rayDistance));
                double distance = point.distanceToSqr(rayPoint);
                double radius = HANDLE_BASE_RADIUS
                        + Math.min(0.18D, rayDistance * 0.008D);
                if (distance <= radius * radius && distance < bestDistance) {
                    bestDistance = distance;
                    best = new HoveredHandle(surface.id(), handle);
                }
            }
        }
        return best;
    }

    /**
     * Empty construction surfaces intentionally have no vanilla proxy hitbox.
     * Selection therefore uses the same kind of mathematical cell-ray test as
     * block placement, so an authored plane remains selectable without
     * becoming a physical obstruction.
     */
    private static AimedSurface findAimedSurface(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        TransformSurfaceRaycast.Target target = TransformSurfaceRaycast.target(
                player, TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));
        return target == null ? null
                : new AimedSurface(target.surface().id(), target.hit());
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
        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location())) {
            for (SurfaceHandle handle : SurfaceHandle.values()) {
                double distance = handlePosition(surface, handle)
                        .distanceToSqr(hit);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSurface = surface;
                    bestHandle = handle;
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

    static Axis hoveredSurfaceGizmoAxis(LocalPlayer player) {
        Selection selection = TransformConstructionClientState.selection();
        return selection == null || selection.type() != SelectionType.SURFACE
                ? null : findSurfaceGizmoAxis(player, selection);
    }

    private static Axis findSurfaceGizmoAxis(LocalPlayer player,
            Selection selection) {
        if (player == null || selection == null
                || selection.type() != SelectionType.SURFACE) return null;
        ConstructionSurface surface =
                TransformConstructionClientState.surface(selection.id());
        if (surface == null) return null;

        Vec3 origin = handlePosition(surface, selection.handle());
        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getViewVector(1.0F).normalize();
        double distance = Math.sqrt(eye.distanceToSqr(origin));
        double tolerance = 0.10D + Math.min(0.16D, distance * 0.008D);
        Axis best = null;
        double bestScore = Double.MAX_VALUE;
        for (Axis candidate : Axis.values()) {
            Vec3 axis = gizmoAxisDirection(selection, candidate);
            double score = segmentHitScore(eye, ray, origin,
                    origin.add(axis.scale(1.18D)));
            if (score <= tolerance * tolerance && score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    static Axis hoveredGroupGizmoAxis(LocalPlayer player) {
        Selection selection = TransformConstructionClientState.selection();
        return selection == null || selection.type() != SelectionType.GROUP
                ? null : findGroupGizmoAxis(player, selection);
    }

    private static Axis findGroupGizmoAxis(LocalPlayer player,
            Selection selection) {
        if (player == null || selection == null
                || selection.type() != SelectionType.GROUP) return null;
        TransformGroup group = TransformConstructionClientState.group(
                selection.id());
        if (group == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getViewVector(1.0F).normalize();
        double distance = Math.sqrt(eye.distanceToSqr(group.origin()));
        double tolerance = 0.10D + Math.min(0.16D, distance * 0.008D);
        Axis best = null;
        double bestScore = Double.MAX_VALUE;
        for (Axis candidate : Axis.values()) {
            Vec3 axis = gizmoAxisDirection(selection, candidate);
            double score = TransformConstructionClientState.mode()
                    == EditMode.ROTATE
                    ? ringHitScore(eye, ray, group.origin(), axis, 0.92D)
                    : segmentHitScore(eye, ray, group.origin(),
                            group.origin().add(axis.scale(1.18D)));
            if (score <= tolerance * tolerance && score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    static Vec3 gizmoAxisDirection(Selection selection, Axis axis) {
        Vec3 basis = switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
        if (selection == null
                || TransformConstructionClientState.transformSpace()
                == TransformConstructionClientState.TransformSpace.GLOBAL) {
            return basis;
        }
        if (selection.type() == SelectionType.GROUP) {
            TransformGroup group = TransformConstructionClientState.group(
                    selection.id());
            return group == null ? basis : TransformMath.rotate(basis,
                    group.rotationX(), group.rotationY(), group.rotationZ())
                    .normalize();
        }
        ConstructionSurface surface =
                TransformConstructionClientState.surface(selection.id());
        if (surface == null) return basis;
        double[] uv = handleUv(selection.handle());
        return switch (axis) {
            case X -> surface.gridFrameTangent(uv[0], uv[1]).normalize();
            case Y -> surface.gridVertical(uv[0]).normalize();
            case Z -> surface.gridNormal(uv[0], uv[1]).normalize();
        };
    }

    private static double[] handleUv(SurfaceHandle handle) {
        return switch (handle) {
            case BOTTOM_START -> new double[]{0.0D, 0.0D};
            case BOTTOM_END -> new double[]{1.0D, 0.0D};
            case TOP_START -> new double[]{0.0D, 1.0D};
            case TOP_END -> new double[]{1.0D, 1.0D};
            case BOTTOM_EDGE -> new double[]{0.5D, 0.0D};
            case TOP_EDGE -> new double[]{0.5D, 1.0D};
            case START_EDGE -> new double[]{0.0D, 0.5D};
            case END_EDGE -> new double[]{1.0D, 0.5D};
            case CENTER -> new double[]{0.5D, 0.5D};
        };
    }

    private static double ringHitScore(Vec3 eye, Vec3 ray, Vec3 center,
            Vec3 normal, double radius) {
        Vec3 basisA = Math.abs(normal.y) < 0.85D
                ? normal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize()
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 basisB = normal.cross(basisA).normalize();
        double best = Double.MAX_VALUE;
        Vec3 previous = center.add(basisA.scale(radius));
        final int segments = 48;
        for (int index = 1; index <= segments; index++) {
            double angle = Math.PI * 2.0D * index / segments;
            Vec3 current = center.add(basisA.scale(Math.cos(angle) * radius))
                    .add(basisB.scale(Math.sin(angle) * radius));
            best = Math.min(best, segmentHitScore(eye, ray, previous, current));
            previous = current;
        }
        return best;
    }

    private static double segmentHitScore(Vec3 eye, Vec3 ray,
            Vec3 a, Vec3 b) {
        Vec3 segment = b.subtract(a);
        double len2 = segment.lengthSqr();
        if (len2 < 1.0E-10D) return Double.MAX_VALUE;
        Vec3 w0 = eye.subtract(a);
        double aa = ray.dot(ray);
        double bb = ray.dot(segment);
        double cc = len2;
        double dd = ray.dot(w0);
        double ee = segment.dot(w0);
        double denominator = aa * cc - bb * bb;
        double rayT;
        double segT;
        if (Math.abs(denominator) < 1.0E-9D) {
            segT = Mth.clamp(ee / cc, 0.0D, 1.0D);
            rayT = Math.max(0.0D,
                    a.add(segment.scale(segT)).subtract(eye).dot(ray));
        } else {
            rayT = (bb * ee - cc * dd) / denominator;
            segT = (aa * ee - bb * dd) / denominator;
            if (rayT < 0.0D) rayT = 0.0D;
            segT = Mth.clamp(segT, 0.0D, 1.0D);
            // Re-evaluate the ray after clamping the finite segment.
            rayT = Math.max(0.0D,
                    a.add(segment.scale(segT)).subtract(eye).dot(ray));
        }
        Vec3 onRay = eye.add(ray.scale(rayT));
        Vec3 onSegment = a.add(segment.scale(segT));
        return onRay.distanceToSqr(onSegment);
    }

    private static void editGroup(Selection selection, double sign) {
        TransformGroup group = TransformConstructionClientState.group(selection.id());
        if (group == null) return;
        if (TransformConstructionClientState.mode() == EditMode.ROTATE) {
            float step = (float) (sign * 5.0D);
            Vec3 worldAxis = gizmoAxisDirection(selection,
                    TransformConstructionClientState.axis());
            boolean local = TransformConstructionClientState.transformSpace()
                    == TransformConstructionClientState.TransformSpace.LOCAL;
            Vec3 compositionAxis = local
                    ? TransformMath.inverseRotate(worldAxis, group.rotationX(),
                            group.rotationY(), group.rotationZ()).normalize()
                    : worldAxis;
            float[] rotation = TransformMath.composeAxisRotation(
                    group.rotationX(), group.rotationY(), group.rotationZ(),
                    compositionAxis, step, local);
            TransformConstructionClientState.upsertGroup(group.withTransform(
                    group.origin(), rotation[0], rotation[1], rotation[2]));
            TransformConstructionNetwork.updateGroup(group.id(), group.origin(),
                    rotation[0], rotation[1], rotation[2]);
        } else {
            Vec3 origin = group.origin().add(
                    gizmoAxisDirection(selection,
                            TransformConstructionClientState.axis())
                            .scale(sign / 16.0D));
            origin = snap16(origin);
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
        Vec3 axis = gizmoAxisDirection(selection,
                TransformConstructionClientState.axis());
        applySurfaceDelta(surface, selection.handle(),
                axis.normalize().scale(sign / 16.0D), true, true);
    }

    private static Vec3 dragAxis(Selection selection) {
        return gizmoAxisDirection(selection,
                TransformConstructionClientState.axis());
    }

    private static void previewGroupDrag(DragState state, Vec3 movement,
            boolean snap) {
        if (state.baseGroup() == null) return;
        TransformGroup base = state.baseGroup();
        Vec3 origin = base.origin().add(movement);
        if (snap) origin = snap16(origin);
        TransformConstructionClientState.previewGroup(base.withTransform(
                origin, base.rotationX(), base.rotationY(), base.rotationZ()));
    }

    private static void previewSurfaceDrag(DragState state, Vec3 movement,
            boolean snap) {
        if (state.baseSurface() == null) return;
        applySurfaceDelta(state.baseSurface(), state.selection().handle(),
                movement, false, snap);
    }

    private static void applySurfaceDelta(ConstructionSurface surface,
            SurfaceHandle handle, Vec3 delta, boolean send, boolean snap) {
        Vec3 bs = surface.bottomStart();
        Vec3 be = surface.bottomEnd();
        Vec3 ts = surface.topStart();
        Vec3 te = surface.topEnd();
        Vec3 curve = surface.curveOffset();
        Vec3 heightCurve = surface.heightCurveOffset();
        switch (handle) {
            case BOTTOM_START -> bs = bs.add(delta);
            case BOTTOM_END -> be = be.add(delta);
            case TOP_START -> ts = ts.add(delta);
            case TOP_END -> te = te.add(delta);
            case BOTTOM_EDGE -> {
                bs = bs.add(delta);
                be = be.add(delta);
            }
            case TOP_EDGE -> {
                ts = ts.add(delta);
                te = te.add(delta);
            }
            case START_EDGE -> {
                bs = bs.add(delta);
                ts = ts.add(delta);
            }
            case END_EDGE -> {
                be = be.add(delta);
                te = te.add(delta);
            }
            case CENTER -> {
                if (TransformConstructionClientState.surfaceCurveAxis()
                        == TransformConstructionClientState.SurfaceCurveAxis.WIDTH) {
                    // The horizontal quadratic contributes half of its control
                    // offset at u=.5.
                    curve = curve.add(delta.scale(2.0D));
                } else {
                    // The vertical bulge reaches its full offset at v=.5.
                    heightCurve = heightCurve.add(delta);
                }
            }
        }

        if (snap && !Screen.hasControlDown()) {
            switch (handle) {
                case BOTTOM_START -> bs = snap16(bs);
                case BOTTOM_END -> be = snap16(be);
                case TOP_START -> ts = snap16(ts);
                case TOP_END -> te = snap16(te);
                case BOTTOM_EDGE -> {
                    bs = snap16(bs);
                    be = snap16(be);
                }
                case TOP_EDGE -> {
                    ts = snap16(ts);
                    te = snap16(te);
                }
                case START_EDGE -> {
                    bs = snap16(bs);
                    ts = snap16(ts);
                }
                case END_EDGE -> {
                    be = snap16(be);
                    te = snap16(te);
                }
                case CENTER -> {
                    Vec3 geometricCenter = bs.add(be).add(ts).add(te)
                            .scale(0.25D);
                    Vec3 visibleCenter = geometricCenter
                            .add(curve.scale(0.5D)).add(heightCurve);
                    Vec3 snappedCenter = snap16(visibleCenter);
                    if (TransformConstructionClientState.surfaceCurveAxis()
                            == TransformConstructionClientState.SurfaceCurveAxis.WIDTH) {
                        curve = snappedCenter.subtract(geometricCenter)
                                .subtract(heightCurve).scale(2.0D);
                    } else {
                        heightCurve = snappedCenter.subtract(geometricCenter)
                                .subtract(curve.scale(0.5D));
                    }
                }
            }
        }

        // Nearby real blocks and other Surface boundaries are preferred even
        // without Shift; Ctrl opts out of all snapping for precision editing.
        // Compute the target from the already-moved handle, not from the
        // original plane or a vanilla proxy, so a curved join remains exact.
        if (!Screen.hasControlDown() && handle != SurfaceHandle.CENTER) {
            Vec3 handlePoint = switch (handle) {
                case BOTTOM_START -> bs;
                case BOTTOM_END -> be;
                case TOP_START -> ts;
                case TOP_END -> te;
                case BOTTOM_EDGE -> bs.add(be).scale(0.5D);
                case TOP_EDGE -> ts.add(te).scale(0.5D);
                case START_EDGE -> bs.add(ts).scale(0.5D);
                case END_EDGE -> be.add(te).scale(0.5D);
                case CENTER -> Vec3.ZERO;
            };
            Vec3 adjustment = TransformSurfaceSnapClient.snap(handlePoint,
                    surface.id()).subtract(handlePoint);
            if (adjustment.lengthSqr() > 1.0E-10D) {
                switch (handle) {
                    case BOTTOM_START -> bs = bs.add(adjustment);
                    case BOTTOM_END -> be = be.add(adjustment);
                    case TOP_START -> ts = ts.add(adjustment);
                    case TOP_END -> te = te.add(adjustment);
                    case BOTTOM_EDGE -> {
                        bs = bs.add(adjustment);
                        be = be.add(adjustment);
                    }
                    case TOP_EDGE -> {
                        ts = ts.add(adjustment);
                        te = te.add(adjustment);
                    }
                    case START_EDGE -> {
                        bs = bs.add(adjustment);
                        ts = ts.add(adjustment);
                    }
                    case END_EDGE -> {
                        be = be.add(adjustment);
                        te = te.add(adjustment);
                    }
                    case CENTER -> { }
                }
            }
        }

        ConstructionSurface next = surface.withGeometry(bs, be, ts, te, curve,
                heightCurve);
        if (send) TransformConstructionClientState.upsertSurface(next);
        else TransformConstructionClientState.previewSurface(next);
        if (send) {
            TransformConstructionNetwork.updateSurface(surface.id(), bs, be, ts,
                    te, curve, heightCurve);
        }
    }

    private static void rememberDragIfNeeded() {
        if (drag == null || drag.remembered()) return;
        TransformConstructionClientState.remember(drag.selection());
        drag = drag.withRemembered();
    }

    private static void finishDrag() {
        if (drag == null) return;
        Selection selection = drag.selection();
        if (drag.remembered() && Math.abs(drag.lastDelta()) > 1.0E-5D) {
            if (selection.type() == SelectionType.GROUP) {
                TransformGroup current = TransformConstructionClientState.group(
                        selection.id());
                if (current != null) {
                    TransformConstructionNetwork.updateGroup(current.id(),
                            current.origin(), current.rotationX(),
                            current.rotationY(), current.rotationZ());
                    // Transform-only movement reuses the local mesh throughout
                    // the drag. Rebuild once here so world-light samples match
                    // the accepted final position.
                    TransformConstructionClientRenderer.invalidateGroup(
                            current.id());
                }
            } else {
                ConstructionSurface current =
                        TransformConstructionClientState.surface(selection.id());
                if (current != null) {
                    TransformConstructionNetwork.updateSurface(current.id(),
                            current.bottomStart(), current.bottomEnd(),
                            current.topStart(), current.topEnd(),
                            current.curveOffset(), current.heightCurveOffset());
                }
            }
        }
        TransformConstructionClientState.commitPreviewGeometry();
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

    public static Vec3 handlePosition(ConstructionSurface surface,
            SurfaceHandle handle) {
        return switch (handle) {
            case BOTTOM_START -> surface.bottomStart();
            case BOTTOM_END -> surface.bottomEnd();
            case TOP_START -> surface.topStart();
            case TOP_END -> surface.topEnd();
            case BOTTOM_EDGE -> surface.bottomStart().add(surface.bottomEnd())
                    .scale(0.5D);
            case TOP_EDGE -> surface.topStart().add(surface.topEnd())
                    .scale(0.5D);
            case START_EDGE -> surface.bottomStart().add(surface.topStart())
                    .scale(0.5D);
            case END_EDGE -> surface.bottomEnd().add(surface.topEnd())
                    .scale(0.5D);
            case CENTER -> surface.gridPoint(0.5D, 0.5D);
        };
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

    private static double rotationParameter(Vec3 eye, Vec3 view, Vec3 origin,
            Vec3 axis) {
        Vec3 normal = axis.normalize();
        double denominator = view.normalize().dot(normal);
        if (Math.abs(denominator) < 1.0E-5D) return Double.NaN;
        double t = origin.subtract(eye).dot(normal) / denominator;
        if (!Double.isFinite(t) || t < 0.0D || t > 64.0D) {
            return Double.NaN;
        }
        Vec3 radial = eye.add(view.normalize().scale(t)).subtract(origin);
        if (radial.lengthSqr() < 1.0E-7D) return Double.NaN;
        Vec3 basisA = Math.abs(normal.y) < 0.85D
                ? normal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize()
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 basisB = normal.cross(basisA).normalize();
        return Math.atan2(radial.dot(basisB), radial.dot(basisA));
    }

    private static Vec3 snap16(Vec3 point) {
        return TransformSurfaceAuthoringMath.snap16(point);
    }

    private static void deleteSelection() {
        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        TransformConstructionNetwork.delete(selection.id(),
                selection.type() == SelectionType.SURFACE);
        TransformConstructionClientState.remove(selection.id(),
                selection.type() == SelectionType.SURFACE);
    }

    private static boolean holdingOffGridTool(LocalPlayer player) {
        return player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                || player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool());
    }

    private static boolean holdingSurfaceTool(LocalPlayer player) {
        return player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                || player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
    }

    private static boolean holdingEditorTool(LocalPlayer player) {
        return player.getMainHandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || holdingSurfaceTool(player);
    }

    private static void status(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), true);
    }

    private record HoveredHandle(java.util.UUID surfaceId,
            SurfaceHandle handle) {
    }

    private record AimedSurface(java.util.UUID surfaceId, Vec3 hit) {
    }

    private record DragState(Selection selection, EditMode mode, Axis axis,
            double startParameter, double lastDelta, boolean remembered,
            TransformGroup baseGroup, ConstructionSurface baseSurface,
            Vec3 anchor, Vec3 dragAxis) {
        private static DragState begin(Selection selection, EditMode mode,
                Axis axis, double parameter, Vec3 anchor, Vec3 dragAxis) {
            return new DragState(selection, mode, axis, parameter, 0.0D, false,
                    selection.type() == SelectionType.GROUP
                            ? TransformConstructionClientState.group(selection.id())
                            : null,
                    selection.type() == SelectionType.SURFACE
                            ? TransformConstructionClientState.surface(selection.id())
                            : null,
                    anchor, dragAxis == null ? Vec3.ZERO : dragAxis.normalize());
        }

        private boolean matches(Selection other, EditMode currentMode,
                Axis currentAxis) {
            return other != null && selection.type() == other.type()
                    && selection.id().equals(other.id())
                    && selection.handle() == other.handle()
                    && mode == currentMode && axis == currentAxis;
        }

        private DragState withLastDelta(double delta) {
            return new DragState(selection, mode, axis, startParameter, delta,
                    remembered, baseGroup, baseSurface, anchor, dragAxis);
        }

        private DragState withRemembered() {
            return new DragState(selection, mode, axis, startParameter,
                    lastDelta, true, baseGroup, baseSurface, anchor, dragAxis);
        }
    }
}
