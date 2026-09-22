from pathlib import Path

def rep(path, old, new, count=1):
    p=Path(path); s=p.read_text()
    n=s.count(old)
    if n != count:
        raise SystemExit(f"{path}: expected {count} matches, found {n}: {old[:100]!r}")
    p.write_text(s.replace(old,new,count))

controls="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientControls.java"
renderer="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java"
hud="src/main/java/com/bl4ues/scpclassifieddirective/client/BuilderToolGuideHud.java"
item="src/main/java/com/bl4ues/scpclassifieddirective/item/SurfaceConstructionToolItem.java"

rep(controls,
'''import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringMath;
''',
'''import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceAuthoringMath;
''')

rep(controls,
'''    private static boolean independentEdgeHandles;

    public static boolean independentEdgeHandles() {
''',
'''    private static boolean independentEdgeHandles;
    private static boolean linkedSurfaceMode;
    private static LinkedSurfaceEdge linkedFirstEdge;
    private static LinkedSurfaceEdge linkedHoveredEdge;
    private static ConstructionSurface linkedPreview;
    private static final UUID LINKED_PREVIEW_ID =
            new UUID(0x5A71FACE5A71FACEL, 0x1EE7C0DE1EE7C0DEL);

    public record LinkedSurfaceEdge(UUID surfaceId, int edge) { }

    public static boolean linkedSurfaceMode() {
        return linkedSurfaceMode;
    }

    public static boolean linkedSurfaceHasFirstEdge() {
        return linkedFirstEdge != null;
    }

    static LinkedSurfaceEdge linkedFirstEdge() {
        return linkedFirstEdge;
    }

    static LinkedSurfaceEdge linkedHoveredEdge() {
        return linkedHoveredEdge;
    }

    static ConstructionSurface linkedPreview() {
        return linkedPreview;
    }

    public static boolean independentEdgeHandles() {
''')

rep(controls,
'''        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative() || !holdingOffGridTool(player)) return;

        UUID aimedGroup = TransformGroupPlacementClient.findAimedGroup(player);
''',
'''        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative()) return;

        if (holdingSurfaceTool(player) && linkedSurfaceMode) {
            // Linked-Surface authoring deliberately owns RMB while active so
            // the normal three-click wall authoring flow cannot start behind it.
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!holdingOffGridTool(player)) return;

        UUID aimedGroup = TransformGroupPlacementClient.findAimedGroup(player);
''')

rep(controls,
'''        event.setCanceled(true);
        if (attackLatch) return;
        attackLatch = true;

        Selection currentSelection = TransformConstructionClientState.selection();
''',
'''        event.setCanceled(true);
        if (attackLatch) return;
        attackLatch = true;

        if (linkedSurfaceMode && holdingSurfaceTool(player)
                && TransformSurfaceAuthoringState.step() == 0) {
            handleLinkedSurfaceClick(player);
            return;
        }

        Selection currentSelection = TransformConstructionClientState.selection();
''')

rep(controls,
'''        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE
                && TransformSurfaceAuthoringState.active()) {
''',
'''        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && linkedSurfaceMode) {
            suppressPauseTicks = 2;
            finishDrag();
            clearLinkedSurfaceMode();
            status("Linked surface creation cancelled");
            return;
        }

        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE
                && TransformSurfaceAuthoringState.active()) {
''')

rep(controls,
'''        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        switch (event.getKey()) {
''',
'''        if (event.getKey() == GLFW.GLFW_KEY_B && holdingSurfaceTool(player)
                && TransformSurfaceAuthoringState.step() == 0) {
            finishDrag();
            linkedSurfaceMode = !linkedSurfaceMode;
            linkedFirstEdge = null;
            linkedHoveredEdge = null;
            linkedPreview = null;
            TransformConstructionClientState.clearHoveredSurface();
            status(linkedSurfaceMode
                    ? "Linked surface: click the first parent edge"
                    : "Linked surface mode disabled");
            return;
        }

        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        switch (event.getKey()) {
''')

rep(controls,
'''        if (!minecraft.options.keyAttack.isDown()) attackLatch = false;

        updateSurfaceHover(player);

        Selection selection = TransformConstructionClientState.selection();
''',
'''        if (!minecraft.options.keyAttack.isDown()) attackLatch = false;

        if (!holdingSurfaceTool(player) && linkedSurfaceMode) {
            clearLinkedSurfaceMode();
        }
        updateLinkedSurfaceHover(player);
        updateSurfaceHover(player);

        Selection selection = TransformConstructionClientState.selection();
''')

rep(controls,
'''    private static void updateSurfaceHover(LocalPlayer player) {
        if (!holdingSurfaceTool(player)
                || TransformSurfaceAuthoringState.step() != 0) {
''',
'''    private static void updateSurfaceHover(LocalPlayer player) {
        if (linkedSurfaceMode || !holdingSurfaceTool(player)
                || TransformSurfaceAuthoringState.step() != 0) {
''')

rep(controls,
'''            for (SurfaceHandle handle : SurfaceHandle.values()) {
                Vec3 point = handlePosition(surface, handle);
''',
'''            for (SurfaceHandle handle : SurfaceHandle.values()) {
                if (surface.bridge() != null && handle != SurfaceHandle.CENTER) {
                    continue;
                }
                Vec3 point = handlePosition(surface, handle);
''',1)

rep(controls,
'''            for (SurfaceHandle handle : SurfaceHandle.values()) {
                double distance = handlePosition(surface, handle)
''',
'''            for (SurfaceHandle handle : SurfaceHandle.values()) {
                if (surface.bridge() != null && handle != SurfaceHandle.CENTER) {
                    continue;
                }
                double distance = handlePosition(surface, handle)
''',1)

rep(controls,
'''        for (Axis candidate : Axis.values()) {
            Vec3 axis = gizmoAxisDirection(selection, candidate);
''',
'''        for (Axis candidate : Axis.values()) {
            if (surface.bridge() != null && candidate != Axis.Y) continue;
            Vec3 axis = gizmoAxisDirection(selection, candidate);
''',1)

rep(controls,
'''        ConstructionSurface surface =
                TransformConstructionClientState.surface(selection.id());
        if (surface == null) return basis;
        double[] uv = handleUv(selection.handle());
''',
'''        ConstructionSurface surface =
                TransformConstructionClientState.surface(selection.id());
        if (surface == null) return basis;
        if (surface.bridge() != null) {
            // A linked ceiling is permanently attached to both parent edges.
            // Its only editable degree of freedom is the world-up crown.
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        double[] uv = handleUv(selection.handle());
''')

rep(controls,
'''    private static void applySurfaceDelta(ConstructionSurface surface,
            SurfaceHandle handle, Vec3 delta, boolean send, boolean snap) {
        Vec3 bs = surface.bottomStart();
''',
'''    private static void applySurfaceDelta(ConstructionSurface surface,
            SurfaceHandle handle, Vec3 delta, boolean send, boolean snap) {
        if (surface.bridge() != null) {
            applyLinkedSurfaceCrownDelta(surface, handle, delta, send, snap);
            return;
        }
        Vec3 bs = surface.bottomStart();
''')

rep(controls,
'''            case END_EDGE -> surface.point(1.0D, 0.5D);
            case CENTER -> surface.point(0.5D, 0.5D);
        };
    }

    private static double axisParameter''',
'''            case END_EDGE -> surface.point(1.0D, 0.5D);
            case CENTER -> surface.bridge() == null
                    ? surface.point(0.5D, 0.5D)
                    : surface.gridPoint(0.5D, 0.5D);
        };
    }

    private static void applyLinkedSurfaceCrownDelta(ConstructionSurface surface,
            SurfaceHandle handle, Vec3 delta, boolean send, boolean snap) {
        if (handle != SurfaceHandle.CENTER) return;
        Vec3 currentCenter = surface.gridPoint(0.5D, 0.5D);
        double targetY = currentCenter.y + delta.y;

        if (!Screen.hasControlDown()) {
            Vec3 probe = new Vec3(currentCenter.x, targetY, currentCenter.z);
            Vec3 snappedFeature = TransformSurfaceSnapClient.snap(probe,
                    surface.id());
            if (snappedFeature != null
                    && Math.abs(snappedFeature.y - targetY) <= 0.31D) {
                targetY = snappedFeature.y;
            }
        }
        if (snap) targetY = Math.rint(targetY * 16.0D) / 16.0D;

        double nextCrown = surface.heightCurveOffset().y
                + targetY - currentCenter.y;
        Vec3 crown = new Vec3(0.0D, nextCrown, 0.0D);
        ConstructionSurface next = surface.withGeometry(
                surface.bottomStart(), surface.bottomEnd(),
                surface.topStart(), surface.topEnd(),
                surface.curveOffset(), crown);
        if (send) TransformConstructionClientState.upsertSurface(next);
        else TransformConstructionClientState.previewSurface(next);
        if (send) {
            TransformConstructionNetwork.updateSurface(surface.id(),
                    next.bottomStart(), next.bottomEnd(),
                    next.topStart(), next.topEnd(),
                    next.curveOffset(), next.heightCurveOffset());
        }
    }

    private static double axisParameter''')

# Insert linked-surface helpers before ordinary hover logic.
rep(controls,
'''    private static void updateSurfaceHover(LocalPlayer player) {
''',
'''    private static void clearLinkedSurfaceMode() {
        linkedSurfaceMode = false;
        linkedFirstEdge = null;
        linkedHoveredEdge = null;
        linkedPreview = null;
    }

    private static void updateLinkedSurfaceHover(LocalPlayer player) {
        linkedHoveredEdge = null;
        linkedPreview = null;
        if (!linkedSurfaceMode || !holdingSurfaceTool(player)
                || TransformSurfaceAuthoringState.step() != 0) return;
        linkedHoveredEdge = findAimedLinkedSurfaceEdge(player);
        if (linkedFirstEdge == null || linkedHoveredEdge == null
                || linkedFirstEdge.surfaceId().equals(
                        linkedHoveredEdge.surfaceId())) return;
        ConstructionSurface first = TransformConstructionClientState.surface(
                linkedFirstEdge.surfaceId());
        ConstructionSurface second = TransformConstructionClientState.surface(
                linkedHoveredEdge.surfaceId());
        linkedPreview = TransformSurfaceBridge.create(LINKED_PREVIEW_ID,
                first, linkedFirstEdge.edge(), second,
                linkedHoveredEdge.edge());
    }

    private static void handleLinkedSurfaceClick(LocalPlayer player) {
        LinkedSurfaceEdge aimed = findAimedLinkedSurfaceEdge(player);
        if (aimed == null) {
            status(linkedFirstEdge == null
                    ? "Aim at a Surface edge"
                    : "Aim at an edge on the second Surface");
            return;
        }
        if (linkedFirstEdge == null) {
            linkedFirstEdge = aimed;
            linkedHoveredEdge = null;
            linkedPreview = null;
            status("First parent edge selected. Click an edge on the other wall");
            return;
        }
        if (linkedFirstEdge.surfaceId().equals(aimed.surfaceId())) {
            status("The second edge must belong to another Surface");
            return;
        }
        ConstructionSurface first = TransformConstructionClientState.surface(
                linkedFirstEdge.surfaceId());
        ConstructionSurface second = TransformConstructionClientState.surface(
                aimed.surfaceId());
        UUID id = UUID.randomUUID();
        ConstructionSurface created = TransformSurfaceBridge.create(id,
                first, linkedFirstEdge.edge(), second, aimed.edge());
        if (created == null) {
            status("Those two edges cannot create a linked surface");
            return;
        }

        finishDrag();
        TransformConstructionClientState.upsertSurface(created);
        TransformConstructionClientState.selectSurface(id, SurfaceHandle.CENTER);
        TransformConstructionClientState.setAxis(Axis.Y);
        TransformConstructionNetwork.createLinkedSurface(id,
                linkedFirstEdge.surfaceId(), linkedFirstEdge.edge(),
                aimed.surfaceId(), aimed.edge());
        clearLinkedSurfaceMode();
        status("Linked surface created. Drag its center up or down");
    }

    private static LinkedSurfaceEdge findAimedLinkedSurfaceEdge(
            LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getViewVector(1.0F).normalize();
        LinkedSurfaceEdge best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        double bestAlong = Double.POSITIVE_INFINITY;
        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location())) {
            if (surface.bridge() != null) continue;
            Vec3 center = surface.gridPoint(0.5D, 0.5D);
            double radius = Math.max(surface.width(), surface.height()) + 2.0D;
            if (center.distanceToSqr(eye)
                    > (HANDLE_MAX_DISTANCE + radius)
                    * (HANDLE_MAX_DISTANCE + radius)) continue;
            for (int edge = 0; edge < 4; edge++) {
                Vec3 previous = linkedEdgePoint(surface, edge, 0.0D);
                for (int sample = 1; sample <= 32; sample++) {
                    double t = sample / 32.0D;
                    Vec3 current = linkedEdgePoint(surface, edge, t);
                    Vec3 middle = previous.add(current).scale(0.5D);
                    double along = middle.subtract(eye).dot(ray);
                    if (along > 0.0D && along <= HANDLE_MAX_DISTANCE) {
                        double tolerance = 0.13D
                                + Math.min(0.20D, along * 0.009D);
                        double score = segmentHitScore(eye, ray,
                                previous, current);
                        if (score <= tolerance * tolerance
                                && (score < bestScore - 1.0E-7D
                                || Math.abs(score - bestScore) < 1.0E-7D
                                && along < bestAlong)) {
                            bestScore = score;
                            bestAlong = along;
                            best = new LinkedSurfaceEdge(surface.id(), edge);
                        }
                    }
                    previous = current;
                }
            }
        }
        return best;
    }

    static Vec3 linkedEdgePoint(ConstructionSurface surface, int edge,
            double t) {
        return switch (edge) {
            case 0 -> surface.gridPoint(0.0D, t);
            case 1 -> surface.gridPoint(1.0D, t);
            case 2 -> surface.gridPoint(t, 0.0D);
            case 3 -> surface.gridPoint(t, 1.0D);
            default -> surface.gridPoint(0.5D, 0.5D);
        };
    }

    private static void updateSurfaceHover(LocalPlayer player) {
''')

# Item tooltip.
rep(item,
'''        tooltip.add(Component.literal(
                "Practical controls and the current authoring step are shown on-screen.")
                .withStyle(ChatFormatting.DARK_GRAY));
''',
'''        tooltip.add(Component.literal(
                "Practical controls and the current authoring step are shown on-screen.")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(
                "Press B to link edges from two existing Surfaces into a parent-anchored curved plane.")
                .withStyle(ChatFormatting.DARK_GRAY));
''')

# HUD linked creation and linked editing.
rep(hud,
'''        if (step == 0) {
            Selection selection = TransformConstructionClientState.selection();
            if (selection != null && selection.type() == SelectionType.SURFACE) {
''',
'''        if (step == 0) {
            if (TransformConstructionClientControls.linkedSurfaceMode()) {
                lines.add(new Line("LMB",
                        TransformConstructionClientControls
                                .linkedSurfaceHasFirstEdge()
                                ? "select second parent edge"
                                : "select first parent edge"));
                lines.add(new Line("B", "cancel linked surface mode"));
                lines.add(new Line("Esc", "cancel"));
                return new Guide("SURFACE LINK", lines);
            }
            Selection selection = TransformConstructionClientState.selection();
            if (selection != null && selection.type() == SelectionType.SURFACE) {
                ConstructionSurface selectedSurface =
                        TransformConstructionClientState.surface(selection.id());
                if (selectedSurface != null && selectedSurface.bridge() != null) {
                    lines.add(new Line("LMB", "drag crown up / down"));
                    lines.add(new Line("Shift", "snap crown height to 1/16"));
                    lines.add(new Line("Ctrl", "hold to disable feature snap"));
                    lines.add(new Line("Parents", "both boundary curves locked", true));
                    String angle = angleText(selection);
                    if (angle != null) lines.add(new Line("Angle", angle, true));
                    lines.add(new Line("Ctrl+Z", "undo"));
                    lines.add(new Line("Del", "delete linked surface"));
                    return new Guide("LINKED SURFACE", lines);
                }
''')

rep(hud,
'''                lines.add(new Line("MMB", "copy transformed block"));
            }
''',
'''                lines.add(new Line("MMB", "copy transformed block"));
                lines.add(new Line("B", "link two existing Surface edges"));
            }
''',1)

# Linked crown has its own angle readout.
rep(hud,
'''        if (handle == SurfaceHandle.CENTER) {
            Vec3 a;
''',
'''        if (surface.bridge() != null) {
            Vec3 a = surface.gridVertical(0.5D, 0.0D).normalize();
            Vec3 b = surface.gridVertical(0.5D, 1.0D).normalize();
            double dot = Mth.clamp(a.dot(b), -1.0D, 1.0D);
            return String.format(java.util.Locale.ROOT, "crown %.1f°",
                    Math.toDegrees(Math.acos(dot)));
        }
        if (handle == SurfaceHandle.CENTER) {
            Vec3 a;
''')

# Renderer: import bridge class.
rep(renderer,
'''import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
''',
'''import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
''')

# Draw link authoring preview after normal Surface guides.
rep(renderer,
'''                for (ConstructionSurface surface : surfaces) {
                    if (selection != null
                            && selection.type() == SelectionType.SURFACE
                            && surface.id().equals(selection.id())) {
                        renderSurfaceGrid(pose, lines, surface, camera);
                    } else {
                        Vec3 outlineCenter = surface.gridPoint(0.5D, 0.5D);
                        if (outlineCenter.distanceToSqr(camera) <= 64.0D * 64.0D) {
                            renderSurfaceOutline(pose, lines, surface, camera);
                        }
                    }
                }
''',
'''                for (ConstructionSurface surface : surfaces) {
                    if (selection != null
                            && selection.type() == SelectionType.SURFACE
                            && surface.id().equals(selection.id())) {
                        renderSurfaceGrid(pose, lines, surface, camera);
                    } else {
                        Vec3 outlineCenter = surface.gridPoint(0.5D, 0.5D);
                        if (outlineCenter.distanceToSqr(camera) <= 64.0D * 64.0D) {
                            renderSurfaceOutline(pose, lines, surface, camera);
                        }
                    }
                }
                renderLinkedSurfaceAuthoring(pose, lines, camera);
''',1)

# Only Y gizmo and center handle for linked plane.
rep(renderer,
'''        for (Axis axis : Axis.values()) {
            renderMoveAxis(pose, lines, origin,
''',
'''        for (Axis axis : Axis.values()) {
            if (surface.bridge() != null && axis != Axis.Y) continue;
            renderMoveAxis(pose, lines, origin,
''',1)

rep(renderer,
'''        for (SurfaceHandle handle : SurfaceHandle.values()) {
            Vec3 point = TransformConstructionClientControls.handlePosition(
''',
'''        for (SurfaceHandle handle : SurfaceHandle.values()) {
            if (surface.bridge() != null && handle != SurfaceHandle.CENTER) {
                continue;
            }
            Vec3 point = TransformConstructionClientControls.handlePosition(
''',1)

# Preview helpers before renderSurfaceOutline.
rep(renderer,
'''    private static void renderSurfaceOutline(PoseStack pose,
''',
'''    private static void renderLinkedSurfaceAuthoring(PoseStack pose,
            VertexConsumer lines, Vec3 camera) {
        if (!TransformConstructionClientControls.linkedSurfaceMode()) return;
        TransformConstructionClientControls.LinkedSurfaceEdge first =
                TransformConstructionClientControls.linkedFirstEdge();
        TransformConstructionClientControls.LinkedSurfaceEdge hovered =
                TransformConstructionClientControls.linkedHoveredEdge();
        if (first != null) {
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(first.surfaceId());
            if (surface != null) {
                renderLinkedSurfaceEdge(pose, lines, surface, first.edge(),
                        0.20F, 0.85F, 1.0F, 1.0F);
            }
        }
        if (hovered != null) {
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(hovered.surfaceId());
            if (surface != null) {
                renderLinkedSurfaceEdge(pose, lines, surface, hovered.edge(),
                        1.0F, 0.78F, 0.16F, 1.0F);
            }
        }
        ConstructionSurface preview =
                TransformConstructionClientControls.linkedPreview();
        if (preview != null
                && preview.gridPoint(0.5D, 0.5D).distanceToSqr(camera)
                        <= MAX_RENDER_DISTANCE_SQR) {
            renderSurfaceGrid(pose, lines, preview, camera);
            Vec3 a = preview.gridPoint(0.5D, 0.0D);
            Vec3 crown = preview.gridPoint(0.5D, 0.5D);
            Vec3 b = preview.gridPoint(0.5D, 1.0D);
            line(pose, lines, a, crown, 0.35F, 0.92F, 1.0F, 0.96F);
            line(pose, lines, crown, b, 0.35F, 0.92F, 1.0F, 0.96F);
        }
    }

    private static void renderLinkedSurfaceEdge(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface, int edge,
            float red, float green, float blue, float alpha) {
        Vec3 previous = TransformConstructionClientControls.linkedEdgePoint(
                surface, edge, 0.0D);
        for (int sample = 1; sample <= 48; sample++) {
            double t = sample / 48.0D;
            Vec3 current = TransformConstructionClientControls.linkedEdgePoint(
                    surface, edge, t);
            line(pose, lines, previous, current, red, green, blue, alpha);
            previous = current;
        }
    }

    private static void renderSurfaceOutline(PoseStack pose,
''')

# Exact canonical shared border, no visible outward overlap.
rep(renderer,
'''            double otherFraction = match.projectionCache().computeIfAbsent(
                    Math.round(fraction * 1_000_000.0D), ignored ->
                            closestEdgeFraction(match.samples(), sourcePoint));
''',
'''            double otherFraction = match.partial()
                    ? match.projectionCache().computeIfAbsent(
                            Math.round(fraction * 1_000_000.0D), ignored ->
                                    closestEdgeFraction(match.samples(),
                                            sourcePoint))
                    : (match.reversed() ? 1.0D - fraction : fraction);
''',1)

rep(renderer,
'''            Vec3 sharedBase = sourcePoint.add(otherPoint).scale(0.5D);
''',
'''            Vec3 sharedBase = match.partial()
                    ? sourcePoint.add(otherPoint).scale(0.5D)
                    : canonicalSharedEdgePoint(surface, edge, other,
                            match.otherEdge(), match.reversed(), fraction);
''',1)

old_overlap='''        if (found == 0) return null;
        // Adjacent curved surfaces may use different cell subdivisions: even
        // after their sampled border points agree, their polygon chords can
        // leave a narrow sky slit between samples. Continue each joined edge
        // 0.055 block OUTSIDE its own parametric domain, into the neighbouring
        // solid surface. This gives both meshes a small physical overlap rather
        // than relying on two independently tessellated lines touching at
        // infinitely thin vertices. The authored curve, interior vertices,
        // collision, and saved geometry are unchanged.
        Vec3 overlap = Vec3.ZERO;
        if (Math.abs(u) < 1.0E-6D) {
            overlap = overlap.subtract(surface.gridTangent(u, v));
        } else if (Math.abs(u - 1.0D) < 1.0E-6D) {
            overlap = overlap.add(surface.gridTangent(u, v));
        }
        if (Math.abs(v) < 1.0E-6D) {
            overlap = overlap.subtract(surface.gridVertical(u, v));
        } else if (Math.abs(v - 1.0D) < 1.0E-6D) {
            overlap = overlap.add(surface.gridVertical(u, v));
        }
        Vec3 joined = result.scale(1.0D / found);
        return overlap.lengthSqr() < 1.0E-10D ? joined
                : joined.add(overlap.normalize().scale(0.055D));
    }
'''
new_overlap='''        if (found == 0) return null;
        // Do not extend a seam beyond either authored plane. That old overlap
        // hid pinholes but produced the visible "raised" lips in tight curves.
        // Full matches now share one deterministic piecewise-linear border,
        // so both independently subdivided grids land on the same seam.
        return result.scale(1.0D / found);
    }

    private static Vec3 canonicalSharedEdgePoint(ConstructionSurface first,
            int firstEdge, ConstructionSurface second, int secondEdge,
            boolean reversed, double fraction) {
        final int segments = 128;
        double scaled = Mth.clamp(fraction, 0.0D, 1.0D) * segments;
        int index = Math.min(segments - 1, (int) Math.floor(scaled));
        double local = scaled - index;
        double a = index / (double) segments;
        double b = (index + 1.0D) / segments;
        Vec3 p0 = edgePoint(first, firstEdge, a).add(
                edgePoint(second, secondEdge,
                        reversed ? 1.0D - a : a)).scale(0.5D);
        Vec3 p1 = edgePoint(first, firstEdge, b).add(
                edgePoint(second, secondEdge,
                        reversed ? 1.0D - b : b)).scale(0.5D);
        return p0.lerp(p1, local);
    }
'''
rep(renderer,old_overlap,new_overlap)

# Seam-only tessellation remains cheap but denser than ordinary perimeter.
rep(renderer,
'''                ? curvedPipe ? 12 : horizontalEdge ? 16 : 2 : 1;
''',
'''                ? curvedPipe ? 12 : horizontalEdge ? 24 : 2 : 1;
''',1)
rep(renderer,
'''                ? verticalEdge ? 16 : 2 : 1;
''',
'''                ? verticalEdge ? 24 : 2 : 1;
''',1)

# Make editor grid itself show the canonical welded border.
rep(renderer,
'''        Vec3 point = surface.gridPoint(u, v);
        Vec3 normal = TransformMath.safeNormalize(surface.gridNormal(u, v),
''',
'''        Vec3 point = surface.gridPoint(u, v);
        boolean border = Math.abs(u) < 1.0E-8D
                || Math.abs(u - 1.0D) < 1.0E-8D
                || Math.abs(v) < 1.0E-8D
                || Math.abs(v - 1.0D) < 1.0E-8D;
        if (border) {
            Vec3 joined = joinedSurfaceEdge(surface, u, v, 0.0D);
            if (joined != null) point = joined;
        }
        Vec3 normal = TransformMath.safeNormalize(surface.gridNormal(u, v),
''',1)
