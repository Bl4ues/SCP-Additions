package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityFineGeometryNetwork;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/** Sub-block polygon editor for Facility Mapping room floors. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityMappingGeometryEditorClient {
    private static final double HANDLE_RADIUS_SQR = 0.42D * 0.42D;
    private static final double MAX_EDIT_DISTANCE = 48.0D;
    private static final int CURVE_SEGMENTS = 8;
    private static final int MAX_VERTICES = 64;

    private static UUID roomId;
    private static int patchIndex = -1;
    private static int selectedVertex = -1;
    private static List<FacilityFloorPatch.Vertex> preview = List.of();
    private static int floorY;
    private static boolean dragging;
    private static boolean changed;
    private static final Deque<List<FacilityFloorPatch.Vertex>> UNDO =
            new ArrayDeque<>();
    private static final int UNDO_LIMIT = 64;

    private FacilityMappingGeometryEditorClient() {
    }

    public static boolean isEditing() {
        return roomId != null;
    }

    /** Selects the visible precision handle instead of starting a new room. */
    public static boolean selectVertexUnderCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        if (roomId == null || preview.isEmpty() || minecraft.player == null) {
            return false;
        }
        Vec3 point = rayPlane(minecraft.player, floorY + 1.01D);
        if (point == null) return false;
        int vertex = nearestVertex(point.x, point.z, preview);
        if (vertex < 0) return false;
        selectedVertex = vertex;
        finishDrag();
        return true;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !player.isCreative()
                || !holdingTool(player)) return;

        if (event.getKey() == GLFW.GLFW_KEY_Z && Screen.hasControlDown()) {
            undo();
            return;
        }

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_G -> {
                if (roomId == null) selectUnderCrosshair(minecraft);
                else {
                    clear();
                    status("Precision edit closed");
                }
            }
            case GLFW.GLFW_KEY_I -> insertVertex(minecraft);
            case GLFW.GLFW_KEY_C -> curveNearestEdge(minecraft);
            case GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_BACKSPACE -> removeVertex();
            case GLFW.GLFW_KEY_ESCAPE -> clear();
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (roomId != null && minecraft.level != null
                && FacilityMappingClientState.roomById(
                        minecraft.level.dimension().location(), roomId) == null) {
            // A room can be deleted from its metadata screen while precision
            // edit is still active. Never leave orphaned handles behind.
            clear();
            return;
        }
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative() || !holdingTool(player)
                || roomId == null || selectedVertex < 0
                || selectedVertex >= preview.size()) {
            finishDrag();
            return;
        }

        if (!minecraft.options.keyAttack.isDown()) {
            finishDrag();
            return;
        }
        Vec3 point = rayPlane(player, floorY + 1.01D);
        if (point == null || point.distanceToSqr(player.getEyePosition())
                > MAX_EDIT_DISTANCE * MAX_EDIT_DISTANCE) return;
        double x = point.x;
        double z = point.z;
        if (player.isShiftKeyDown()) {
            x = Math.rint(x * 16.0D) / 16.0D;
            z = Math.rint(z * 16.0D) / 16.0D;
        }
        List<FacilityFloorPatch.Vertex> next = new ArrayList<>(preview);
        FacilityFloorPatch.Vertex old = next.get(selectedVertex);
        if (Math.abs(old.x() - x) < 1.0E-5D
                && Math.abs(old.z() - z) < 1.0E-5D) return;
        if (!dragging) remember();
        next.set(selectedVertex, new FacilityFloorPatch.Vertex(x, z));
        FacilityFloorPatch patch = FacilityFloorPatch.polygon(floorY, next);
        if (patch == null) return;
        preview = List.copyOf(next);
        dragging = true;
        changed = true;
        FacilityMappingClientState.replaceRoomPatch(
                minecraft.level.dimension().location(), roomId, patchIndex, patch);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS
                || roomId == null || preview.size() < 3) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !holdingTool(minecraft.player)) return;

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        double y = floorY + 1.035D;
        for (int index = 0; index < preview.size(); index++) {
            FacilityFloorPatch.Vertex a = preview.get(index);
            FacilityFloorPatch.Vertex b = preview.get((index + 1) % preview.size());
            boolean aligned = Math.abs(a.x() - b.x()) < 1.0E-5D
                    || Math.abs(a.z() - b.z()) < 1.0E-5D;
            line(pose, lines, new Vec3(a.x(), y, a.z()),
                    new Vec3(b.x(), y, b.z()),
                    aligned ? 0.15F : 1.0F,
                    aligned ? 1.0F : 0.80F,
                    aligned ? 0.32F : 0.16F, 1.0F);
            drawHandle(pose, lines, a.x(), y, a.z(),
                    index == selectedVertex);
            FacilityFloorPatch.Vertex midpoint = new FacilityFloorPatch.Vertex(
                    (a.x() + b.x()) * 0.5D, (a.z() + b.z()) * 0.5D);
            drawCross(pose, lines, midpoint.x(), y, midpoint.z());
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void selectUnderCrosshair(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return;
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                minecraft.level.dimension().location(), blockHit.getBlockPos());
        if (room == null) {
            status("No mapped room under crosshair");
            return;
        }
        Vec3 world = blockHit.getLocation();
        int bestPatch = -1;
        double best = Double.MAX_VALUE;
        for (int index = 0; index < room.patches().size(); index++) {
            FacilityFloorPatch patch = room.patches().get(index);
            double dy = Math.abs(world.y - (patch.y() + 1.0D));
            boolean inside = patch.containsXZ(world.x, world.z);
            double score = dy + (inside ? 0.0D : 4.0D);
            if (score < best) {
                best = score;
                bestPatch = index;
            }
        }
        if (bestPatch < 0) return;
        FacilityFloorPatch patch = room.patches().get(bestPatch);
        List<FacilityFloorPatch.Vertex> outline = new ArrayList<>(patch.outline());
        roomId = room.id();
        patchIndex = bestPatch;
        floorY = patch.y();
        preview = List.copyOf(outline);
        selectedVertex = nearestVertex(world.x, world.z, outline);
        if (selectedVertex < 0) selectedVertex = nearestVertexUnbounded(
                world.x, world.z, outline);
        changed = false;
        dragging = false;
        UNDO.clear();
        status("Precision edit opened");
    }

    private static void insertVertex(Minecraft minecraft) {
        if (roomId == null || preview.size() < 3 || minecraft.player == null) return;
        Vec3 point = rayPlane(minecraft.player, floorY + 1.01D);
        if (point == null) return;
        int edge = nearestEdge(point.x, point.z, preview);
        if (edge < 0 || preview.size() >= MAX_VERTICES) return;
        FacilityFloorPatch.Vertex a = preview.get(edge);
        FacilityFloorPatch.Vertex b = preview.get((edge + 1) % preview.size());
        FacilityFloorPatch.Vertex inserted = closestPoint(point.x, point.z, a, b);
        remember();
        List<FacilityFloorPatch.Vertex> next = new ArrayList<>(preview);
        next.add(edge + 1, inserted);
        preview = List.copyOf(next);
        selectedVertex = edge + 1;
        changed = true;
        pushPreview();
        status("Vertex added");
    }

    private static void curveNearestEdge(Minecraft minecraft) {
        if (roomId == null || preview.size() < 3 || minecraft.player == null) return;
        Vec3 point = rayPlane(minecraft.player, floorY + 1.01D);
        if (point == null) return;
        int edge = nearestEdge(point.x, point.z, preview);
        if (edge < 0) return;
        int available = MAX_VERTICES - preview.size();
        int segments = Math.min(CURVE_SEGMENTS, available + 1);
        if (segments < 2) {
            status("64-vertex limit reached");
            return;
        }

        FacilityFloorPatch.Vertex a = preview.get(edge);
        FacilityFloorPatch.Vertex b = preview.get((edge + 1) % preview.size());
        FacilityFloorPatch.Vertex control = new FacilityFloorPatch.Vertex(
                point.x, point.z);
        remember();
        List<FacilityFloorPatch.Vertex> next = new ArrayList<>(
                preview.size() + segments - 1);
        for (int index = 0; index < preview.size(); index++) {
            next.add(preview.get(index));
            if (index != edge) continue;
            for (int sample = 1; sample < segments; sample++) {
                double t = sample / (double) segments;
                next.add(quadratic(a, control, b, t));
            }
        }
        FacilityFloorPatch patch = FacilityFloorPatch.polygon(floorY, next);
        if (patch == null) return;
        preview = List.copyOf(next);
        selectedVertex = Math.min(edge + segments / 2, preview.size() - 1);
        changed = true;
        FacilityMappingClientState.replaceRoomPatch(
                minecraft.level.dimension().location(), roomId, patchIndex, patch);
        pushPreview();
        status("Edge curved");
    }

    private static FacilityFloorPatch.Vertex quadratic(
            FacilityFloorPatch.Vertex a, FacilityFloorPatch.Vertex control,
            FacilityFloorPatch.Vertex b, double t) {
        double inverse = 1.0D - t;
        return new FacilityFloorPatch.Vertex(
                inverse * inverse * a.x()
                        + 2.0D * inverse * t * control.x()
                        + t * t * b.x(),
                inverse * inverse * a.z()
                        + 2.0D * inverse * t * control.z()
                        + t * t * b.z());
    }

    private static void removeVertex() {
        if (roomId == null || selectedVertex < 0 || preview.size() <= 3) return;
        remember();
        List<FacilityFloorPatch.Vertex> next = new ArrayList<>(preview);
        next.remove(selectedVertex);
        preview = List.copyOf(next);
        selectedVertex = Mth.clamp(selectedVertex, 0, preview.size() - 1);
        changed = true;
        pushPreview();
        status("Vertex removed");
    }

    private static void remember() {
        if (preview.isEmpty()) return;
        List<FacilityFloorPatch.Vertex> snapshot = List.copyOf(preview);
        if (snapshot.equals(UNDO.peekLast())) return;
        UNDO.addLast(snapshot);
        while (UNDO.size() > UNDO_LIMIT) UNDO.removeFirst();
    }

    private static void undo() {
        if (roomId == null) return;
        finishDrag();
        List<FacilityFloorPatch.Vertex> previous = UNDO.pollLast();
        if (previous == null || previous.size() < 3) {
            status("Nothing to undo");
            return;
        }
        FacilityFloorPatch patch = FacilityFloorPatch.polygon(floorY, previous);
        if (patch == null) return;
        preview = List.copyOf(previous);
        selectedVertex = Mth.clamp(selectedVertex, 0, preview.size() - 1);
        changed = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            FacilityMappingClientState.replaceRoomPatch(
                    minecraft.level.dimension().location(), roomId, patchIndex,
                    patch);
        }
        FacilityFineGeometryNetwork.requestPatchUpdate(roomId, patchIndex,
                floorY, preview);
        status("Undo");
    }

    private static void finishDrag() {
        if (!dragging) return;
        dragging = false;
        if (changed) pushPreview();
    }

    private static void pushPreview() {
        if (roomId == null || patchIndex < 0 || preview.size() < 3) return;
        FacilityFineGeometryNetwork.requestPatchUpdate(roomId, patchIndex,
                floorY, preview);
        changed = false;
    }

    private static void clear() {
        finishDrag();
        roomId = null;
        patchIndex = -1;
        selectedVertex = -1;
        preview = List.of();
        changed = false;
        UNDO.clear();
    }

    private static Vec3 rayPlane(LocalPlayer player, double planeY) {
        Vec3 eye = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        if (Math.abs(direction.y) < 1.0E-5D) return null;
        double t = (planeY - eye.y) / direction.y;
        if (t < 0.0D || t > MAX_EDIT_DISTANCE) return null;
        return eye.add(direction.scale(t));
    }

    private static int nearestVertex(double x, double z,
            List<FacilityFloorPatch.Vertex> vertices) {
        int best = -1;
        double distance = HANDLE_RADIUS_SQR;
        for (int i = 0; i < vertices.size(); i++) {
            FacilityFloorPatch.Vertex vertex = vertices.get(i);
            double dx = vertex.x() - x;
            double dz = vertex.z() - z;
            double candidate = dx * dx + dz * dz;
            if (candidate < distance) {
                distance = candidate;
                best = i;
            }
        }
        return best;
    }

    private static int nearestVertexUnbounded(double x, double z,
            List<FacilityFloorPatch.Vertex> vertices) {
        int best = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < vertices.size(); i++) {
            FacilityFloorPatch.Vertex vertex = vertices.get(i);
            double dx = vertex.x() - x;
            double dz = vertex.z() - z;
            double candidate = dx * dx + dz * dz;
            if (candidate < distance) {
                distance = candidate;
                best = i;
            }
        }
        return best;
    }

    private static int nearestEdge(double x, double z,
            List<FacilityFloorPatch.Vertex> vertices) {
        int best = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < vertices.size(); i++) {
            FacilityFloorPatch.Vertex a = vertices.get(i);
            FacilityFloorPatch.Vertex b = vertices.get((i + 1) % vertices.size());
            FacilityFloorPatch.Vertex point = closestPoint(x, z, a, b);
            double dx = point.x() - x;
            double dz = point.z() - z;
            double candidate = dx * dx + dz * dz;
            if (candidate < distance) {
                distance = candidate;
                best = i;
            }
        }
        return best;
    }

    private static FacilityFloorPatch.Vertex closestPoint(double x, double z,
            FacilityFloorPatch.Vertex a, FacilityFloorPatch.Vertex b) {
        double dx = b.x() - a.x();
        double dz = b.z() - a.z();
        double length = dx * dx + dz * dz;
        double t = length < 1.0E-9D ? 0.0D
                : ((x - a.x()) * dx + (z - a.z()) * dz) / length;
        t = Mth.clamp(t, 0.0D, 1.0D);
        return new FacilityFloorPatch.Vertex(a.x() + dx * t,
                a.z() + dz * t);
    }

    private static boolean holdingTool(LocalPlayer player) {
        return player.getMainHandItem().is(FacilityMappingItems.getTool())
                || player.getOffhandItem().is(FacilityMappingItems.getTool());
    }

    private static void drawHandle(PoseStack pose, VertexConsumer lines,
            double x, double y, double z, boolean selected) {
        double s = selected ? 0.13D : 0.08D;
        line(pose, lines, new Vec3(x - s, y, z), new Vec3(x + s, y, z),
                selected ? 1.0F : 0.2F, selected ? 0.78F : 1.0F,
                0.15F, 1.0F);
        line(pose, lines, new Vec3(x, y, z - s), new Vec3(x, y, z + s),
                selected ? 1.0F : 0.2F, selected ? 0.78F : 1.0F,
                0.15F, 1.0F);
    }

    private static void drawCross(PoseStack pose, VertexConsumer lines,
            double x, double y, double z) {
        double s = 0.045D;
        line(pose, lines, new Vec3(x - s, y, z - s),
                new Vec3(x + s, y, z + s), 0.25F, 0.78F, 1.0F, 0.75F);
        line(pose, lines, new Vec3(x - s, y, z + s),
                new Vec3(x + s, y, z - s), 0.25F, 0.78F, 1.0F, 0.75F);
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 a,
            Vec3 b, float red, float green, float blue, float alpha) {
        Vec3 normal = b.subtract(a);
        if (normal.lengthSqr() < 1.0E-9D) return;
        normal = normal.normalize();
        PoseStack.Pose current = pose.last();
        lines.vertex(current.pose(), (float) a.x, (float) a.y, (float) a.z)
                .color(red, green, blue, alpha)
                .normal(current.normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z).endVertex();
        lines.vertex(current.pose(), (float) b.x, (float) b.y, (float) b.z)
                .color(red, green, blue, alpha)
                .normal(current.normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z).endVertex();
    }

    private static void status(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), true);
    }
}
