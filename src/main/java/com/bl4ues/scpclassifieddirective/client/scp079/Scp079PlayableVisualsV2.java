package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp131AEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp131BEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Second-pass SCP-079 HUD built around mapped rooms instead of arbitrary range. */
public final class Scp079PlayableVisualsV2 {
    private static final ResourceLocation DOOR_ICON = resource(
            "textures/gui/scp079/icons/door.png");
    private static final ResourceLocation TESLA_ICON = resource(
            "textures/gui/scp079/icons/tesla_gate.png");
    private static final ResourceLocation CAMERA_ICON = resource(
            "textures/gui/scp079/icons/camera.png");
    private static final int ROOM_DEVICE_BORDER = 1;
    private static final int ADJACENT_ROOM_GAP = 2;
    private static final int ROOM_COLUMN_HEIGHT = 12;
    private static final int MAX_SCAN_BLOCKS = 280_000;

    private static final List<RecognitionBox> RECOGNITION = new ArrayList<>();
    private static final List<InteractionPrompt> PROMPTS = new ArrayList<>();
    private static List<WorldTarget> cachedTargets = List.of();
    private static ProjectionContext projectionContext;
    private static long lastTargetScanTick = Long.MIN_VALUE;
    private static UUID lastTargetRoom;

    private Scp079PlayableVisualsV2() { }

    public static void handleInventoryKey(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !Scp079PlayableClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        boolean requested = false;
        while (minecraft.options.keyInventory.consumeClick()) requested = true;
        if (!requested) return;
        if (minecraft.options.keyShift.isDown()) {
            Scp079LeaveRoleScreen.open();
        } else if (Scp079PlayableClient.networkAvailable()) {
            Scp079FacilityMapScreen.open();
        }
    }

    public static void captureRecognition(RenderLevelStageEvent event) {
        if (!Scp079PlayableClient.cameraMode()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        RECOGNITION.clear();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 look = new Vec3(camera.getLookVector()).normalize();
        Vec3 up = new Vec3(camera.getUpVector()).normalize();
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0D).normalize();
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        projectionContext = new ProjectionContext(cameraPos, right, up, look,
                projection, guiW, guiH);

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || entity == minecraft.player
                    || entity.position().distanceToSqr(cameraPos) > 42.0D * 42.0D) continue;
            int color;
            String label;
            if (entity instanceof Player) {
                color = 0xFF49D7F5;
                label = "HUMAN";
            } else {
                int number = scpNumber(entity);
                if (number < 0) continue;
                color = 0xFFFF5147;
                label = "SCP-" + String.format(Locale.ROOT, "%03d", number);
            }
            Vec3 center = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            if (!visibleFromCamera(minecraft, cameraPos, center)) continue;

            double halfW = Mth.clamp(entity.getBbWidth() * 0.38D, 0.22D, 0.52D);
            double halfH = Mth.clamp(entity.getBbHeight() * 0.19D, 0.27D, 0.48D);
            ScreenPoint tl = project(center.add(right.scale(-halfW)).add(up.scale(halfH)), projectionContext);
            ScreenPoint tr = project(center.add(right.scale(halfW)).add(up.scale(halfH)), projectionContext);
            ScreenPoint bl = project(center.add(right.scale(-halfW)).add(up.scale(-halfH)), projectionContext);
            ScreenPoint br = project(center.add(right.scale(halfW)).add(up.scale(-halfH)), projectionContext);
            if (tl == null || tr == null || bl == null || br == null) continue;
            int minX = Mth.floor(Math.min(Math.min(tl.x, tr.x), Math.min(bl.x, br.x)));
            int maxX = Mth.ceil(Math.max(Math.max(tl.x, tr.x), Math.max(bl.x, br.x)));
            int minY = Mth.floor(Math.min(Math.min(tl.y, tr.y), Math.min(bl.y, br.y)));
            int maxY = Mth.ceil(Math.max(Math.max(tl.y, tr.y), Math.max(bl.y, br.y)));
            if (maxX < 0 || minX > guiW || maxY < 0 || minY > guiH) continue;
            int minimum = 18;
            if (maxX - minX < minimum) {
                int c = (minX + maxX) / 2;
                minX = c - minimum / 2;
                maxX = c + minimum / 2;
            }
            if (maxY - minY < minimum) {
                int c = (minY + maxY) / 2;
                minY = c - minimum / 2;
                maxY = c + minimum / 2;
            }
            RECOGNITION.add(new RecognitionBox(minX, minY, maxX, maxY, label, color));
            if (RECOGNITION.size() >= 64) break;
        }
    }

    public static void renderLocalHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Scp079UiTheme.renderFrame(graphics, width, height);
        int x = 24;
        int y = 23;
        Scp079UiTheme.draw(graphics, minecraft.font, "SCP-079", x, y,
                1.34F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font, "LOCAL HOST", x, y + 21,
                1.24F, Scp079UiTheme.ACCENT);
        FacilityRoomSnapshot room = roomAt(Scp079PlayableClient.hostPos());
        if (room != null) {
            String floor = room.floorShortLabel().isBlank()
                    ? "UNASSIGNED" : room.floorShortLabel();
            Scp079UiTheme.draw(graphics, minecraft.font, floor.toUpperCase(Locale.ROOT),
                    x, y + 48, 1.14F, Scp079UiTheme.MUTED);
            if (!room.name().isBlank()) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        room.name().toUpperCase(Locale.ROOT), x, y + 66,
                        1.22F, Scp079UiTheme.TEXT);
            }
        }
        String key = mapKeyLabel(minecraft);
        if (Scp079PlayableClient.networkAvailable()) {
            drawRight(graphics, minecraft, "[" + key + "]  FACILITY MAP",
                    width - 24, 24, 1.18F, Scp079UiTheme.ACCENT);
        } else {
            drawRight(graphics, minecraft, "AUXILIARY POWER OFFLINE",
                    width - 24, 24, 1.18F, 0xFFD57D78);
        }
        drawRight(graphics, minecraft, "SHIFT + " + key + "  LEAVE SCP ROLE",
                width - 24, 44, 1.10F, Scp079UiTheme.MUTED);
        Scp079UiTheme.renderPower(graphics, minecraft, Scp079PlayableClient.power());
    }

    public static void renderCameraHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Scp079UiTheme.renderFrame(graphics, width, height);
        FacilityRoomSnapshot room = roomAt(BlockPos.containing(Scp079PlayableClient.viewPosition()));
        int x = 24;
        int y = 23;
        if (room != null) {
            String floor = room.floorShortLabel().isBlank()
                    ? "UNASSIGNED" : room.floorShortLabel();
            Scp079UiTheme.draw(graphics, minecraft.font, floor.toUpperCase(Locale.ROOT),
                    x, y, 1.30F, Scp079UiTheme.ACCENT);
            if (!room.name().isBlank()) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        room.name().toUpperCase(Locale.ROOT), x, y + 21,
                        1.34F, Scp079UiTheme.TEXT);
            }
        }
        int counterY = y + 62;
        Scp079UiTheme.draw(graphics, minecraft.font,
                "TOTAL LIFEFORMS: " + Scp079TrackingClientState.totalLifeforms(),
                x, counterY, 1.23F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font,
                "TARGETS: " + Scp079TrackingClientState.targets(),
                x, counterY + 20, 1.23F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font,
                "SCP SUBJECTS: " + Scp079TrackingClientState.scpSubjects(),
                x, counterY + 40, 1.23F, Scp079UiTheme.TEXT);

        String key = mapKeyLabel(minecraft);
        drawRight(graphics, minecraft, "OPEN FACILITY MAP  [" + key + "]",
                width - 24, 23, 1.20F, Scp079UiTheme.TEXT);
        drawRight(graphics, minecraft, "SHIFT + " + key + "  LEAVE SCP ROLE",
                width - 24, 43, 1.10F, Scp079UiTheme.MUTED);
        drawRight(graphics, minecraft, "HOLD SHIFT  CURSOR",
                width - 24, 61, 1.10F, Scp079UiTheme.MUTED);

        renderRecognition(graphics, minecraft);
        refreshPrompts(minecraft);
        renderPrompts(graphics, minecraft);
        Scp079UiTheme.renderPower(graphics, minecraft, Scp079PlayableClient.power());
    }

    public static boolean handleInteraction(boolean attack, boolean use) {
        if (!Scp079PlayableClient.cameraMode()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.screen != null) return false;
        refreshPrompts(minecraft);
        ScreenPoint pointer = pointer(minecraft);
        InteractionPrompt prompt = closestPrompt(pointer, 76.0D);
        if (prompt == null) return false;
        if (prompt.kind == TargetKind.CAMERA) {
            if (attack || use) {
                FacilityRoomSnapshot room = roomForDevice(prompt.pos, 1);
                if (room != null) Scp079PlayableNetwork.requestRoom(room.id());
            }
            return true;
        }
        if (prompt.kind == TargetKind.DOOR) {
            if (attack) {
                Scp079PlayableNetwork.requestAction(
                        Scp079PlayableManager.ManualAction.PRIMARY, prompt.pos);
            } else if (use) {
                Scp079PlayableNetwork.requestAction(
                        Scp079PlayableManager.ManualAction.LOCK, prompt.pos);
            }
            return true;
        }
        if (prompt.kind == TargetKind.TESLA) {
            if (attack || use) {
                Scp079PlayableNetwork.requestAction(
                        Scp079PlayableManager.ManualAction.PRIMARY, prompt.pos);
            }
            return true;
        }
        return false;
    }

    private static void renderRecognition(GuiGraphics graphics, Minecraft minecraft) {
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        for (RecognitionBox box : RECOGNITION) {
            int x1 = Mth.clamp(box.x1, 4, guiW - 6);
            int y1 = Mth.clamp(box.y1, 16, guiH - 6);
            int x2 = Mth.clamp(box.x2, x1 + 2, guiW - 4);
            int y2 = Mth.clamp(box.y2, y1 + 2, guiH - 4);
            graphics.fill(x1, y1, x2, y1 + 1, box.color);
            graphics.fill(x1, y2 - 1, x2, y2, box.color);
            graphics.fill(x1, y1, x1 + 1, y2, box.color);
            graphics.fill(x2 - 1, y1, x2, y2, box.color);
            Scp079UiTheme.drawCentered(graphics, minecraft.font, box.label,
                    (x1 + x2) * 0.5F, Math.max(2, y1 - 15), 1.10F, box.color);
        }
    }

    private static void refreshPrompts(Minecraft minecraft) {
        PROMPTS.clear();
        ProjectionContext context = projectionContext;
        if (context == null || minecraft.level == null) return;
        FacilityRoomSnapshot room = roomAt(BlockPos.containing(context.cameraPos));
        refreshTargetCache(minecraft, room);
        for (WorldTarget target : cachedTargets) {
            Vec3 anchor = anchor(target);
            boolean visible = target.kind == TargetKind.CAMERA
                    || visibleFromCamera(minecraft, context.cameraPos, anchor);
            if (!visible) continue;
            ScreenPoint point = project(anchor, context);
            if (point == null) continue;
            int margin = Scp079UiTheme.FRAME_MARGIN + 8;
            if (point.x < margin || point.x > context.width - margin
                    || point.y < margin || point.y > context.height - margin) continue;
            PROMPTS.add(new InteractionPrompt(target.kind, target.pos, point));
        }
    }

    private static void refreshTargetCache(Minecraft minecraft,
            FacilityRoomSnapshot current) {
        long tick = minecraft.level.getGameTime();
        UUID roomId = current == null ? null : current.id();
        if (tick - lastTargetScanTick < 10L
                && java.util.Objects.equals(roomId, lastTargetRoom)) return;
        lastTargetScanTick = tick;
        lastTargetRoom = roomId;
        if (current == null) {
            cachedTargets = List.of();
            return;
        }

        List<WorldTarget> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        int[] budget = new int[] {MAX_SCAN_BLOCKS};
        scanRoom(minecraft, current, true, true, visited, result, budget);
        for (FacilityRoomSnapshot candidate : FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension())) {
            if (candidate.id().equals(current.id()) || !adjacent(current, candidate)) continue;
            scanRoom(minecraft, candidate, false, true, visited, result, budget);
            if (budget[0] <= 0) break;
        }
        cachedTargets = List.copyOf(result);
    }

    private static void scanRoom(Minecraft minecraft, FacilityRoomSnapshot room,
            boolean includeFacilityDevices, boolean includeCameras,
            Set<Long> visited, List<WorldTarget> result, int[] budget) {
        for (FacilityFloorPatch patch : room.patches()) {
            int minX = patch.minX() - ROOM_DEVICE_BORDER;
            int maxX = patch.maxX() + ROOM_DEVICE_BORDER;
            int minZ = patch.minZ() - ROOM_DEVICE_BORDER;
            int maxZ = patch.maxZ() + ROOM_DEVICE_BORDER;
            for (int x = minX; x <= maxX && budget[0] > 0; x++) {
                for (int z = minZ; z <= maxZ && budget[0] > 0; z++) {
                    for (int y = patch.y() - 1;
                            y <= patch.y() + ROOM_COLUMN_HEIGHT && budget[0] > 0; y++) {
                        budget[0]--;
                        BlockPos pos = new BlockPos(x, y, z);
                        long packed = pos.asLong();
                        if (!visited.add(packed) || !minecraft.level.hasChunkAt(pos)) continue;
                        BlockState state = minecraft.level.getBlockState(pos);
                        if (includeCameras && state.is(SurveillanceCameraPlaceholderModule.BLOCK.get())) {
                            addTarget(result, new WorldTarget(TargetKind.CAMERA, pos.immutable()));
                        } else if (includeFacilityDevices && TeslaGateStructure.isController(state)) {
                            addTarget(result, new WorldTarget(TargetKind.TESLA, pos.immutable()));
                        } else if (includeFacilityDevices && FacilityModule.isFacilityDoor(state)) {
                            addTarget(result, new WorldTarget(TargetKind.DOOR, pos.immutable()));
                        }
                    }
                }
            }
            if (budget[0] <= 0) break;
        }
    }

    private static void addTarget(List<WorldTarget> targets, WorldTarget next) {
        for (WorldTarget existing : targets) {
            if (existing.kind == next.kind
                    && existing.pos.distSqr(next.pos) <= (next.kind == TargetKind.DOOR ? 3.0D : 1.0D)) {
                return;
            }
        }
        targets.add(next);
    }

    private static boolean adjacent(FacilityRoomSnapshot a, FacilityRoomSnapshot b) {
        if (!a.floorLongLabel().equalsIgnoreCase(b.floorLongLabel())) return false;
        for (FacilityFloorPatch pa : a.patches()) {
            for (FacilityFloorPatch pb : b.patches()) {
                if (Math.abs(pa.y() - pb.y()) > 3) continue;
                int gapX = intervalGap(pa.minX(), pa.maxX(), pb.minX(), pb.maxX());
                int gapZ = intervalGap(pa.minZ(), pa.maxZ(), pb.minZ(), pb.maxZ());
                if (Math.max(gapX, gapZ) <= ADJACENT_ROOM_GAP) return true;
            }
        }
        return false;
    }

    private static int intervalGap(int amin, int amax, int bmin, int bmax) {
        if (amax < bmin) return bmin - amax - 1;
        if (bmax < amin) return amin - bmax - 1;
        return 0;
    }

    private static void renderPrompts(GuiGraphics graphics, Minecraft minecraft) {
        ScreenPoint pointer = pointer(minecraft);
        InteractionPrompt active = closestPrompt(pointer, 76.0D);
        for (InteractionPrompt prompt : PROMPTS) {
            boolean focused = prompt == active;
            int size = focused ? 38 : prompt.kind == TargetKind.CAMERA ? 29 : 33;
            int x = Math.round(prompt.screen.x) - size / 2;
            int y = Math.round(prompt.screen.y) - size / 2;
            ResourceLocation icon = switch (prompt.kind) {
                case TESLA -> TESLA_ICON;
                case CAMERA -> CAMERA_ICON;
                default -> DOOR_ICON;
            };
            float tint = focused ? 1.0F : 0.76F;
            Scp079UiTheme.blitIcon64(graphics, icon, x, y, size,
                    0.72F * tint, 0.94F * tint, 1.0F * tint,
                    focused ? 1.0F : 0.80F);
            if (!focused) continue;
            String primary = switch (prompt.kind) {
                case DOOR -> "OPEN / CLOSE  [LMB]  " + cost(5.0D, minecraft);
                case TESLA -> "SUPPRESS  [LMB]  " + cost(12.0D, minecraft);
                case CAMERA -> "SWITCH CAMERA  [LMB]";
            };
            int tx = x + size + 8;
            int ty = y + 2;
            Scp079UiTheme.draw(graphics, minecraft.font, primary,
                    tx, ty, 1.17F, Scp079UiTheme.TEXT);
            if (prompt.kind == TargetKind.DOOR) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        "LOCK  [RMB]  " + cost(12.0D, minecraft),
                        tx, ty + 18, 1.13F, Scp079UiTheme.TEXT);
            }
        }
    }

    private static InteractionPrompt closestPrompt(ScreenPoint point, double maxDistance) {
        if (point == null) return null;
        double maxSqr = maxDistance * maxDistance;
        return PROMPTS.stream()
                .filter(prompt -> distanceSqr(prompt.screen, point) <= maxSqr)
                .min(Comparator.comparingDouble(prompt -> distanceSqr(prompt.screen, point)))
                .orElse(null);
    }

    private static ScreenPoint pointer(Minecraft minecraft) {
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        if (!minecraft.options.keyShift.isDown()) {
            return new ScreenPoint(guiW * 0.5F, guiH * 0.5F);
        }
        double rawX = minecraft.mouseHandler.xpos() * guiW
                / Math.max(1.0D, minecraft.getWindow().getScreenWidth());
        double rawY = minecraft.mouseHandler.ypos() * guiH
                / Math.max(1.0D, minecraft.getWindow().getScreenHeight());
        return new ScreenPoint((float) Scp079CrtPostProcessor.logicalX(
                rawX, rawY, guiW, guiH),
                (float) Scp079CrtPostProcessor.logicalY(
                        rawX, rawY, guiW, guiH));
    }

    private static double distanceSqr(ScreenPoint a, ScreenPoint b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private static Vec3 anchor(WorldTarget target) {
        Vec3 center = Vec3.atCenterOf(target.pos);
        return target.kind == TargetKind.DOOR
                ? center.add(0.0D, 0.22D, 0.0D) : center;
    }

    private static FacilityRoomSnapshot roomAt(BlockPos pos) {
        return FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(), pos);
    }

    private static FacilityRoomSnapshot roomForDevice(BlockPos pos, int border) {
        for (FacilityRoomSnapshot room : FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension())) {
            for (FacilityFloorPatch patch : room.patches()) {
                if (pos.getX() >= patch.minX() - border
                        && pos.getX() <= patch.maxX() + border
                        && pos.getZ() >= patch.minZ() - border
                        && pos.getZ() <= patch.maxZ() + border
                        && pos.getY() >= patch.y() - 1
                        && pos.getY() <= patch.y() + ROOM_COLUMN_HEIGHT) {
                    return room;
                }
            }
        }
        return null;
    }

    private static ScreenPoint project(Vec3 point, ProjectionContext context) {
        Vec3 delta = point.subtract(context.cameraPos);
        float vx = (float) delta.dot(context.right);
        float vy = (float) delta.dot(context.up);
        float vz = (float) -delta.dot(context.look);
        Vector4f clip = context.projection.transform(new Vector4f(vx, vy, vz, 1.0F));
        float w = clip.w();
        if (!Float.isFinite(w) || w <= 1.0E-5F) return null;
        float nx = clip.x() / w;
        float ny = clip.y() / w;
        if (!Float.isFinite(nx) || !Float.isFinite(ny)
                || nx < -1.25F || nx > 1.25F || ny < -1.25F || ny > 1.25F) return null;
        return new ScreenPoint((nx * 0.5F + 0.5F) * context.width,
                (0.5F - ny * 0.5F) * context.height);
    }

    private static boolean visibleFromCamera(Minecraft minecraft, Vec3 camera, Vec3 target) {
        if (minecraft.level == null || minecraft.player == null) return false;
        BlockHitResult hit = minecraft.level.clip(new ClipContext(camera, target,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() != HitResult.Type.BLOCK
                || camera.distanceToSqr(hit.getLocation()) + 0.35D >= camera.distanceToSqr(target);
    }

    private static int scpNumber(Entity entity) {
        if (entity instanceof Scp106Entity) return 106;
        if (entity instanceof Scp131AEntity || entity instanceof Scp131BEntity) return 131;
        if (entity instanceof Scp173Entity) return 173;
        if (entity instanceof Scp939Entity) return 939;
        return -1;
    }

    private static void drawRight(GuiGraphics graphics, Minecraft minecraft,
            String value, int right, int y, float scale, int color) {
        int width = Scp079UiTheme.scaledWidth(minecraft.font, value, scale);
        Scp079UiTheme.draw(graphics, minecraft.font, value, right - width, y, scale, color);
    }

    private static String mapKeyLabel(Minecraft minecraft) {
        return minecraft.options.keyInventory.getTranslatedKeyMessage().getString()
                .toUpperCase(Locale.ROOT);
    }

    private static String cost(double base, Minecraft minecraft) {
        if (minecraft.level == null) return ((int) base) + " AP";
        double multiplier = switch (minecraft.level.getDifficulty()) {
            case PEACEFUL -> 1.50D;
            case EASY -> 1.25D;
            case HARD -> 0.80D;
            default -> 1.0D;
        };
        double value = base * multiplier;
        return Math.abs(value - Math.rint(value)) < 0.01D
                ? (int) Math.rint(value) + " AP"
                : String.format(Locale.ROOT, "%.1f AP", value);
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private enum TargetKind { DOOR, TESLA, CAMERA }
    private record ProjectionContext(Vec3 cameraPos, Vec3 right, Vec3 up,
            Vec3 look, Matrix4f projection, int width, int height) { }
    private record ScreenPoint(float x, float y) { }
    private record RecognitionBox(int x1, int y1, int x2, int y2,
            String label, int color) { }
    private record WorldTarget(TargetKind kind, BlockPos pos) { }
    private record InteractionPrompt(TargetKind kind, BlockPos pos,
            ScreenPoint screen) { }
}
