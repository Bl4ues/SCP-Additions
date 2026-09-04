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
import java.util.List;
import java.util.Locale;

/** Unified visual and contextual-interaction layer for playable SCP-079. */
public final class Scp079PlayableVisuals {
    private static final ResourceLocation DOOR_ICON = resource(
            "textures/gui/scp079/icons/door.png");
    private static final ResourceLocation TESLA_ICON = resource(
            "textures/gui/scp079/icons/tesla_gate.png");
    private static final ResourceLocation CAMERA_ICON = resource(
            "textures/gui/scp079/icons/camera.png");

    private static final List<RecognitionBox> RECOGNITION = new ArrayList<>();
    private static final List<InteractionPrompt> PROMPTS = new ArrayList<>();
    private static List<BlockPos> nearbyCameras = List.of();
    private static ProjectionContext projectionContext;
    private static long lastCameraScanTick = Long.MIN_VALUE;
    private static Vec3 lastCameraScanOrigin = Vec3.ZERO;

    private Scp079PlayableVisuals() { }

    /** Inventory opens the map; Shift + Inventory always opens role exit. */
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

    /**
     * Capture camera projection and recognition boxes once per world frame.
     *
     * The stage check deliberately comes before clearing the list. Render-level
     * events continue after AFTER_ENTITIES; clearing first used to erase every
     * identification box before the HUD could display it.
     */
    public static void captureRecognition(RenderLevelStageEvent event) {
        if (!Scp079PlayableClient.cameraMode()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
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
                    || entity.position().distanceToSqr(cameraPos)
                    > 42.0D * 42.0D) continue;

            int color;
            String label;
            if (entity instanceof Player) {
                color = 0xFF45D8FF;
                label = "HUMAN";
            } else {
                int number = scpNumber(entity);
                if (number < 0) continue;
                color = 0xFFFF5147;
                label = "SCP-" + String.format(Locale.ROOT, "%03d", number);
            }

            Vec3 center = new Vec3(entity.getX(), entity.getEyeY(),
                    entity.getZ());
            if (!visibleFromCamera(minecraft, cameraPos, center)) continue;

            double halfW = Mth.clamp(entity.getBbWidth() * 0.38D,
                    0.22D, 0.52D);
            double halfH = Mth.clamp(entity.getBbHeight() * 0.19D,
                    0.27D, 0.48D);
            ScreenPoint tl = project(center.add(right.scale(-halfW))
                    .add(up.scale(halfH)), projectionContext);
            ScreenPoint tr = project(center.add(right.scale(halfW))
                    .add(up.scale(halfH)), projectionContext);
            ScreenPoint bl = project(center.add(right.scale(-halfW))
                    .add(up.scale(-halfH)), projectionContext);
            ScreenPoint br = project(center.add(right.scale(halfW))
                    .add(up.scale(-halfH)), projectionContext);
            if (tl == null || tr == null || bl == null || br == null) continue;

            int minX = Mth.floor(Math.min(Math.min(tl.x, tr.x),
                    Math.min(bl.x, br.x)));
            int maxX = Mth.ceil(Math.max(Math.max(tl.x, tr.x),
                    Math.max(bl.x, br.x)));
            int minY = Mth.floor(Math.min(Math.min(tl.y, tr.y),
                    Math.min(bl.y, br.y)));
            int maxY = Mth.ceil(Math.max(Math.max(tl.y, tr.y),
                    Math.max(bl.y, br.y)));
            if (maxX < 0 || minX > guiW || maxY < 0 || minY > guiH) continue;

            int minSize = 18;
            if (maxX - minX < minSize) {
                int c = (minX + maxX) / 2;
                minX = c - minSize / 2;
                maxX = c + minSize / 2;
            }
            if (maxY - minY < minSize) {
                int c = (minY + maxY) / 2;
                minY = c - minSize / 2;
                maxY = c + minSize / 2;
            }
            RECOGNITION.add(new RecognitionBox(minX, minY, maxX, maxY,
                    label, color));
            if (RECOGNITION.size() >= 64) break;
        }

        refreshNearbyCameras(minecraft, cameraPos);
    }

    public static void renderLocalHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Scp079UiTheme.renderFrame(graphics, width, height);

        int x = 36;
        int y = 31;
        Scp079UiTheme.draw(graphics, minecraft.font, "SCP-079",
                x, y, 1.34F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font, "LOCAL HOST",
                x, y + 20, 1.20F, Scp079UiTheme.ACCENT);

        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                Scp079PlayableClient.hostPos());
        int roomY = y + 43;
        if (room != null) {
            String floor = room.floorShortLabel().isBlank()
                    ? "UNASSIGNED" : room.floorShortLabel();
            Scp079UiTheme.draw(graphics, minecraft.font, floor.toUpperCase(Locale.ROOT),
                    x, roomY, 1.12F, Scp079UiTheme.MUTED);
            if (!room.name().isBlank()) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        room.name().toUpperCase(Locale.ROOT), x, roomY + 17,
                        1.20F, Scp079UiTheme.TEXT);
            }
        }

        String mapKey = mapKeyLabel(minecraft);
        if (Scp079PlayableClient.networkAvailable()) {
            drawRight(graphics, minecraft, "[" + mapKey + "]  FACILITY MAP",
                    width - 36, 32, 1.16F, Scp079UiTheme.ACCENT);
        } else {
            drawRight(graphics, minecraft, "AUXILIARY POWER OFFLINE",
                    width - 36, 32, 1.16F, 0xFFD57D78);
        }
        drawRight(graphics, minecraft,
                "SHIFT + " + mapKey + "  LEAVE SCP ROLE",
                width - 36, 50, 1.08F, Scp079UiTheme.MUTED);
        Scp079UiTheme.renderPower(graphics, minecraft,
                Scp079PlayableClient.power());
    }

    public static void renderCameraHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Scp079UiTheme.renderFrame(graphics, width, height);

        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                BlockPos.containing(Scp079PlayableClient.viewPosition()));
        int x = 36;
        int y = 31;
        if (room != null) {
            String floor = room.floorShortLabel().isBlank()
                    ? "UNASSIGNED" : room.floorShortLabel();
            Scp079UiTheme.draw(graphics, minecraft.font,
                    floor.toUpperCase(Locale.ROOT), x, y,
                    1.22F, Scp079UiTheme.ACCENT);
            if (!room.name().isBlank()) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        room.name().toUpperCase(Locale.ROOT), x, y + 19,
                        1.28F, Scp079UiTheme.TEXT);
            }
        }

        int counterY = y + 58;
        Scp079UiTheme.draw(graphics, minecraft.font,
                "TOTAL LIFEFORMS: " + Scp079TrackingClientState.totalLifeforms(),
                x, counterY, 1.20F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font,
                "TARGETS: " + Scp079TrackingClientState.targets(),
                x, counterY + 18, 1.20F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, minecraft.font,
                "SCP SUBJECTS: " + Scp079TrackingClientState.scpSubjects(),
                x, counterY + 36, 1.20F, Scp079UiTheme.TEXT);

        String key = mapKeyLabel(minecraft);
        drawRight(graphics, minecraft, "OPEN FACILITY MAP  [" + key + "]",
                width - 36, 31, 1.16F, Scp079UiTheme.TEXT);
        drawRight(graphics, minecraft,
                "SHIFT + " + key + "  LEAVE SCP ROLE",
                width - 36, 49, 1.08F, Scp079UiTheme.MUTED);
        drawRight(graphics, minecraft, "HOLD SHIFT  CURSOR",
                width - 36, 66, 1.08F, Scp079UiTheme.MUTED);

        renderRecognitionBoxes(graphics, minecraft);
        refreshPrompts(minecraft);
        renderPrompts(graphics, minecraft);
        Scp079UiTheme.renderPower(graphics, minecraft,
                Scp079PlayableClient.power());
    }

    /** Compatibility target for older mixin revisions while master converges. */
    public static void renderLocalExtras(GuiGraphics graphics) {
        // The complete local-host HUD is rendered by renderLocalHud().
    }

    /** Route LMB/RMB through the same projected prompt the player can see. */
    public static boolean handleInteraction(boolean attack, boolean use) {
        if (!Scp079PlayableClient.cameraMode()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.screen != null) return false;
        refreshPrompts(minecraft);
        ScreenPoint pointer = pointer(minecraft);
        InteractionPrompt prompt = closestPrompt(pointer, 56.0D);

        // Keep direct centre aiming functional even before the first projected
        // HUD frame has populated a prompt list.
        if (prompt == null && !minecraft.options.keyShift.isDown()) {
            WorldTarget direct = resolveAimedTarget(minecraft);
            if (direct != null) {
                prompt = new InteractionPrompt(direct.kind, direct.pos,
                        new ScreenPoint(minecraft.getWindow().getGuiScaledWidth() * 0.5F,
                                minecraft.getWindow().getGuiScaledHeight() * 0.5F));
            }
        }
        if (prompt == null) return false;

        if (prompt.kind == TargetKind.CAMERA) {
            if (attack || use) {
                FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                        Scp079PlayableClient.hostDimension(), prompt.pos);
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
            if (attack) {
                Scp079PlayableNetwork.requestAction(
                        Scp079PlayableManager.ManualAction.PRIMARY, prompt.pos);
            }
            return true;
        }
        return false;
    }

    private static void renderRecognitionBoxes(GuiGraphics graphics,
            Minecraft minecraft) {
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        for (RecognitionBox box : RECOGNITION) {
            int x1 = Mth.clamp(box.x1, 7, guiW - 8);
            int y1 = Mth.clamp(box.y1, 20, guiH - 8);
            int x2 = Mth.clamp(box.x2, x1 + 2, guiW - 7);
            int y2 = Mth.clamp(box.y2, y1 + 2, guiH - 7);
            int c = box.color;
            int corner = Math.max(6,
                    Math.min(13, Math.min(x2 - x1, y2 - y1) / 3));
            int dim = (c & 0x00FFFFFF) | 0x62000000;
            graphics.fill(x1, y1, x2, y1 + 1, dim);
            graphics.fill(x1, y2 - 1, x2, y2, dim);
            graphics.fill(x1, y1, x1 + 1, y2, dim);
            graphics.fill(x2 - 1, y1, x2, y2, dim);
            graphics.fill(x1, y1, x1 + corner, y1 + 2, c);
            graphics.fill(x1, y1, x1 + 2, y1 + corner, c);
            graphics.fill(x2 - corner, y1, x2, y1 + 2, c);
            graphics.fill(x2 - 2, y1, x2, y1 + corner, c);
            graphics.fill(x1, y2 - 2, x1 + corner, y2, c);
            graphics.fill(x1, y2 - corner, x1 + 2, y2, c);
            graphics.fill(x2 - corner, y2 - 2, x2, y2, c);
            graphics.fill(x2 - 2, y2 - corner, x2, y2, c);
            Scp079UiTheme.drawCentered(graphics, minecraft.font, box.label,
                    (x1 + x2) * 0.5F, Math.max(4, y1 - 15), 1.06F, c);
        }
    }

    private static void refreshPrompts(Minecraft minecraft) {
        PROMPTS.clear();
        ProjectionContext context = projectionContext;
        if (context == null || minecraft.level == null) return;

        WorldTarget aimed = resolveAimedTarget(minecraft);
        if (aimed != null) addPrompt(minecraft, aimed, context);

        Vec3 camera = context.cameraPos;
        for (BlockPos pos : nearbyCameras) {
            Vec3 anchor = Vec3.atCenterOf(pos);
            if (anchor.distanceToSqr(camera) < 2.25D
                    || anchor.distanceToSqr(camera) > 24.0D * 24.0D
                    || !visibleFromCamera(minecraft, camera, anchor)) continue;
            addPrompt(minecraft, new WorldTarget(TargetKind.CAMERA, pos), context);
        }
    }

    private static void addPrompt(Minecraft minecraft, WorldTarget target,
            ProjectionContext context) {
        if (PROMPTS.stream().anyMatch(existing -> existing.kind == target.kind
                && existing.pos.equals(target.pos))) return;
        Vec3 anchor = Vec3.atCenterOf(target.pos);
        ScreenPoint point = project(anchor, context);
        if (point == null) return;
        int margin = Scp079UiTheme.FRAME_MARGIN + 16;
        if (point.x < margin || point.x > context.width - margin
                || point.y < margin || point.y > context.height - margin) return;
        PROMPTS.add(new InteractionPrompt(target.kind,
                target.pos.immutable(), point));
    }

    private static void renderPrompts(GuiGraphics graphics,
            Minecraft minecraft) {
        ScreenPoint pointer = pointer(minecraft);
        InteractionPrompt active = closestPrompt(pointer, 56.0D);
        for (InteractionPrompt prompt : PROMPTS) {
            boolean focused = prompt == active;
            int size = focused ? 34 : prompt.kind == TargetKind.CAMERA ? 25 : 30;
            int x = Math.round(prompt.screen.x) - size / 2;
            int y = Math.round(prompt.screen.y) - size / 2;
            ResourceLocation icon = switch (prompt.kind) {
                case TESLA -> TESLA_ICON;
                case CAMERA -> CAMERA_ICON;
                default -> DOOR_ICON;
            };
            float tint = focused ? 1.0F : 0.78F;
            Scp079UiTheme.blitIcon64(graphics, icon, x, y, size,
                    0.72F * tint, 0.94F * tint, 1.0F * tint,
                    focused ? 1.0F : 0.82F);

            if (!focused) continue;
            String primary = switch (prompt.kind) {
                case DOOR -> "OPEN / CLOSE  [LMB]  " + cost(5.0D, minecraft);
                case TESLA -> "SUPPRESS  [LMB]  " + cost(12.0D, minecraft);
                case CAMERA -> "SWITCH CAMERA  [LMB]";
                default -> "";
            };
            int tx = x + size + 8;
            int ty = y + 2;
            Scp079UiTheme.draw(graphics, minecraft.font, primary,
                    tx, ty, 1.12F, Scp079UiTheme.TEXT);
            if (prompt.kind == TargetKind.DOOR) {
                Scp079UiTheme.draw(graphics, minecraft.font,
                        "LOCK  [RMB]  " + cost(12.0D, minecraft),
                        tx, ty + 17, 1.08F, Scp079UiTheme.TEXT);
            }
        }
    }

    private static InteractionPrompt closestPrompt(ScreenPoint point,
            double maxDistance) {
        if (point == null) return null;
        double maxSqr = maxDistance * maxDistance;
        return PROMPTS.stream()
                .filter(prompt -> distanceSqr(prompt.screen, point) <= maxSqr)
                .min(Comparator.comparingDouble(prompt ->
                        distanceSqr(prompt.screen, point)))
                .orElse(null);
    }

    private static ScreenPoint pointer(Minecraft minecraft) {
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        if (!minecraft.options.keyShift.isDown()) {
            return new ScreenPoint(guiW * 0.5F, guiH * 0.5F);
        }
        double x = minecraft.mouseHandler.xpos() * guiW
                / Math.max(1.0D, minecraft.getWindow().getScreenWidth());
        double y = minecraft.mouseHandler.ypos() * guiH
                / Math.max(1.0D, minecraft.getWindow().getScreenHeight());
        return new ScreenPoint((float) x, (float) y);
    }

    private static double distanceSqr(ScreenPoint a, ScreenPoint b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private static WorldTarget resolveAimedTarget(Minecraft minecraft) {
        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level == null) return null;
        BlockPos hitPos = hit.getBlockPos();
        BlockState direct = minecraft.level.getBlockState(hitPos);
        if (direct.is(SurveillanceCameraPlaceholderModule.BLOCK.get())) {
            return new WorldTarget(TargetKind.CAMERA, hitPos.immutable());
        }

        BlockPos tesla = nearestMatching(minecraft, hitPos, 4,
                state -> TeslaGateStructure.isController(state));
        if (tesla != null) return new WorldTarget(TargetKind.TESLA, tesla);

        BlockPos door = nearestMatching(minecraft, hitPos, 3,
                FacilityModule::isFacilityDoor);
        if (door != null) return new WorldTarget(TargetKind.DOOR, door);
        return null;
    }

    private static BlockPos nearestMatching(Minecraft minecraft,
            BlockPos origin, int radius,
            java.util.function.Predicate<BlockState> predicate) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos cursor : BlockPos.betweenClosed(
                origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            BlockState state = minecraft.level.getBlockState(cursor);
            if (!predicate.test(state)) continue;
            double distance = cursor.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = cursor.immutable();
            }
        }
        return best;
    }

    private static void refreshNearbyCameras(Minecraft minecraft,
            Vec3 cameraPos) {
        long tick = minecraft.level.getGameTime();
        if (tick - lastCameraScanTick < 10L
                && cameraPos.distanceToSqr(lastCameraScanOrigin) < 4.0D) return;
        lastCameraScanTick = tick;
        lastCameraScanOrigin = cameraPos;

        BlockPos center = BlockPos.containing(cameraPos);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-16, -8, -16), center.offset(16, 8, 16))) {
            if (minecraft.level.getBlockState(cursor)
                    .is(SurveillanceCameraPlaceholderModule.BLOCK.get())) {
                found.add(cursor.immutable());
            }
        }
        nearbyCameras = List.copyOf(found);
    }

    private static ScreenPoint project(Vec3 point, ProjectionContext context) {
        Vec3 delta = point.subtract(context.cameraPos);
        float vx = (float) delta.dot(context.right);
        float vy = (float) delta.dot(context.up);
        float vz = (float) -delta.dot(context.look);
        Vector4f clip = context.projection.transform(new Vector4f(
                vx, vy, vz, 1.0F));
        float w = clip.w();
        if (!Float.isFinite(w) || w <= 1.0E-5F) return null;
        float nx = clip.x() / w;
        float ny = clip.y() / w;
        if (!Float.isFinite(nx) || !Float.isFinite(ny)
                || nx < -1.25F || nx > 1.25F
                || ny < -1.25F || ny > 1.25F) return null;
        return new ScreenPoint((nx * 0.5F + 0.5F) * context.width,
                (0.5F - ny * 0.5F) * context.height);
    }

    private static boolean visibleFromCamera(Minecraft minecraft, Vec3 camera,
            Vec3 target) {
        if (minecraft.level == null || minecraft.player == null) return false;
        BlockHitResult hit = minecraft.level.clip(new ClipContext(camera, target,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                minecraft.player));
        return hit.getType() != HitResult.Type.BLOCK
                || camera.distanceToSqr(hit.getLocation()) + 0.35D
                >= camera.distanceToSqr(target);
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
        Scp079UiTheme.draw(graphics, minecraft.font, value,
                right - width, y, scale, color);
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

    private enum TargetKind { NONE, DOOR, TESLA, CAMERA }

    private record ProjectionContext(Vec3 cameraPos, Vec3 right, Vec3 up,
            Vec3 look, Matrix4f projection, int width, int height) { }

    private record ScreenPoint(float x, float y) { }

    private record RecognitionBox(int x1, int y1, int x2, int y2,
            String label, int color) { }

    private record WorldTarget(TargetKind kind, BlockPos pos) { }

    private record InteractionPrompt(TargetKind kind, BlockPos pos,
            ScreenPoint screen) { }
}
