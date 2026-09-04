package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp131AEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp131BEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
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
import java.util.List;
import java.util.Locale;

/** Visual/input replacement layer for playable SCP-079. */
public final class Scp079PlayableVisuals {
    private static final ResourceLocation DOOR_ICON = resource(
            "textures/gui/scp079/icons/door.png");
    private static final ResourceLocation TESLA_ICON = resource(
            "textures/gui/scp079/icons/tesla_gate.png");
    private static final List<RecognitionBox> RECOGNITION = new ArrayList<>();
    private static long interferenceSeed;

    private Scp079PlayableVisuals() { }

    /** Replaces the normal 079 inventory-key handler. Shift + Inventory always exits. */
    public static void handleInventoryKey(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !Scp079PlayableClient.active()) return;
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

    public static void renderLocalExtras(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        String label = "SHIFT + " + mapKeyLabel(minecraft) + "  LEAVE SCP ROLE";
        int textW = minecraft.font.width(ScpFonts.roboto(label));
        graphics.drawString(minecraft.font, ScpFonts.roboto(label),
                width - textW - 18, 18, 0xFF8AA2B1, false);
    }

    /** Captures 2D screen-space face boxes instead of rendering a 3D world AABB. */
    public static void captureRecognition(RenderLevelStageEvent event) {
        RECOGNITION.clear();
        if (!Scp079PlayableClient.cameraMode()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 look = new Vec3(camera.getLookVector()).normalize();
        Vec3 up = new Vec3(camera.getUpVector()).normalize();
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0D).normalize();
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || entity == minecraft.player
                    || entity.position().distanceToSqr(cameraPos) > 42.0D * 42.0D) continue;

            int color;
            String label;
            if (entity instanceof Player) {
                color = 0xFF39C8F1;
                label = "HUMAN";
            } else {
                int number = scpNumber(entity);
                if (number < 0) continue;
                color = 0xFFFF4B42;
                label = "SCP-" + String.format(Locale.ROOT, "%03d", number);
            }

            Vec3 center = new Vec3(entity.getX(), entity.getEyeY() + 0.04D,
                    entity.getZ());
            if (!visibleFromCamera(minecraft, cameraPos, center)) continue;

            double halfW = Mth.clamp(entity.getBbWidth() * 0.36D, 0.20D, 0.46D);
            double halfH = Mth.clamp(entity.getBbHeight() * 0.17D, 0.24D, 0.42D);
            ScreenPoint tl = project(center.add(right.scale(-halfW)).add(up.scale(halfH)),
                    cameraPos, right, up, look, projection, guiW, guiH);
            ScreenPoint tr = project(center.add(right.scale(halfW)).add(up.scale(halfH)),
                    cameraPos, right, up, look, projection, guiW, guiH);
            ScreenPoint bl = project(center.add(right.scale(-halfW)).add(up.scale(-halfH)),
                    cameraPos, right, up, look, projection, guiW, guiH);
            ScreenPoint br = project(center.add(right.scale(halfW)).add(up.scale(-halfH)),
                    cameraPos, right, up, look, projection, guiW, guiH);
            if (tl == null || tr == null || bl == null || br == null) continue;

            int minX = Mth.floor(Math.min(Math.min(tl.x, tr.x), Math.min(bl.x, br.x)));
            int maxX = Mth.ceil(Math.max(Math.max(tl.x, tr.x), Math.max(bl.x, br.x)));
            int minY = Mth.floor(Math.min(Math.min(tl.y, tr.y), Math.min(bl.y, br.y)));
            int maxY = Mth.ceil(Math.max(Math.max(tl.y, tr.y), Math.max(bl.y, br.y)));
            if (maxX < 0 || minX > guiW || maxY < 0 || minY > guiH) continue;
            int minSize = 13;
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
            RECOGNITION.add(new RecognitionBox(minX, minY, maxX, maxY, label, color));
            if (RECOGNITION.size() >= 64) break;
        }
    }

    public static void renderCameraHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        renderCrtLens(graphics, width, height);

        Vec3 view = Scp079PlayableClient.viewPosition();
        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(), BlockPos.containing(view));
        graphics.drawString(minecraft.font, ScpFonts.roboto("CAMERA FEED ONLINE"),
                34, 27, 0xFFD8F5FF, false);
        int titleY = 44;
        if (room != null) {
            if (!room.floorShortLabel().isBlank()) {
                graphics.drawString(minecraft.font, ScpFonts.roboto(room.floorShortLabel()),
                        34, titleY, 0xFF8FB8C8, false);
                titleY += 13;
            }
            if (!room.name().isBlank()) {
                graphics.drawString(minecraft.font,
                        ScpFonts.titillium(room.name().toUpperCase(Locale.ROOT)),
                        34, titleY, 0xFFF0FAFF, false);
            }
        }

        int counterY = 84;
        graphics.drawString(minecraft.font, ScpFonts.roboto(
                        "TOTAL LIFEFORMS: " + Scp079TrackingClientState.totalLifeforms()),
                34, counterY, 0xFFBDEEFF, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(
                        "TARGETS: " + Scp079TrackingClientState.targets()),
                34, counterY + 12, 0xFFBDEEFF, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(
                        "SCP SUBJECTS: " + Scp079TrackingClientState.scpSubjects()),
                34, counterY + 24, 0xFFBDEEFF, false);

        String map = "OPEN FACILITY MAP  [" + mapKeyLabel(minecraft) + "]";
        int rightX = width - minecraft.font.width(ScpFonts.roboto(map)) - 34;
        graphics.drawString(minecraft.font, ScpFonts.roboto(map),
                rightX, 28, 0xFFD8F5FF, false);
        String leave = "SHIFT + " + mapKeyLabel(minecraft) + "  LEAVE SCP ROLE";
        graphics.drawString(minecraft.font, ScpFonts.roboto(leave),
                width - minecraft.font.width(ScpFonts.roboto(leave)) - 34,
                42, 0xFF7697A4, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto("HOLD SHIFT  CURSOR"),
                width - minecraft.font.width(ScpFonts.roboto("HOLD SHIFT  CURSOR")) - 34,
                56, 0xFF7697A4, false);

        renderRecognitionBoxes(graphics, minecraft);
        TargetKind target = aimedTarget(minecraft);
        if (target != TargetKind.NONE) {
            ResourceLocation icon = target == TargetKind.TESLA ? TESLA_ICON : DOOR_ICON;
            int size = 32;
            int ix = width / 2 - size / 2;
            int iy = height / 2 - size / 2;
            RenderSystem.setShaderColor(0.78F, 0.95F, 1.0F, 1.0F);
            graphics.blit(icon, ix, iy, 0.0F, 0.0F, size, size, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            String primary = target == TargetKind.DOOR
                    ? "OPEN / CLOSE  [LMB]  " + cost(5.0D, minecraft)
                    : "SUPPRESS  [LMB]  " + cost(12.0D, minecraft);
            graphics.drawString(minecraft.font, ScpFonts.roboto(primary),
                    width - 202, height / 2 - 4, 0xFFEAF8FF, false);
            if (target == TargetKind.DOOR) {
                graphics.drawString(minecraft.font,
                        ScpFonts.roboto("LOCK  [RMB]  " + cost(12.0D, minecraft)),
                        width - 202, height / 2 + 10, 0xFFEAF8FF, false);
            }
        }
        renderPower(graphics, minecraft, width, height);
    }

    private static void renderRecognitionBoxes(GuiGraphics graphics, Minecraft minecraft) {
        for (RecognitionBox box : RECOGNITION) {
            int x1 = Mth.clamp(box.x1, 3, minecraft.getWindow().getGuiScaledWidth() - 4);
            int y1 = Mth.clamp(box.y1, 14, minecraft.getWindow().getGuiScaledHeight() - 4);
            int x2 = Mth.clamp(box.x2, x1 + 2, minecraft.getWindow().getGuiScaledWidth() - 3);
            int y2 = Mth.clamp(box.y2, y1 + 2, minecraft.getWindow().getGuiScaledHeight() - 3);
            int c = box.color;
            int corner = Math.max(4, Math.min(10, Math.min(x2 - x1, y2 - y1) / 3));
            // Thin screen-space rectangle with stronger corners, like a camera
            // recognition reticle rather than a world-space hitbox.
            graphics.fill(x1, y1, x2, y1 + 1, (c & 0x00FFFFFF) | 0x70000000);
            graphics.fill(x1, y2 - 1, x2, y2, (c & 0x00FFFFFF) | 0x70000000);
            graphics.fill(x1, y1, x1 + 1, y2, (c & 0x00FFFFFF) | 0x70000000);
            graphics.fill(x2 - 1, y1, x2, y2, (c & 0x00FFFFFF) | 0x70000000);
            graphics.fill(x1, y1, x1 + corner, y1 + 2, c);
            graphics.fill(x1, y1, x1 + 2, y1 + corner, c);
            graphics.fill(x2 - corner, y1, x2, y1 + 2, c);
            graphics.fill(x2 - 2, y1, x2, y1 + corner, c);
            graphics.fill(x1, y2 - 2, x1 + corner, y2, c);
            graphics.fill(x1, y2 - corner, x1 + 2, y2, c);
            graphics.fill(x2 - corner, y2 - 2, x2, y2, c);
            graphics.fill(x2 - 2, y2 - corner, x2, y2, c);
            int labelW = minecraft.font.width(ScpFonts.roboto(box.label));
            int lx = (x1 + x2 - labelW) / 2;
            graphics.drawString(minecraft.font, ScpFonts.roboto(box.label),
                    lx, Math.max(3, y1 - 11), c, false);
        }
    }

    /** Shaderpack-safe post overlay: it never replaces the shaderpack framebuffer. */
    private static void renderCrtLens(GuiGraphics graphics, int width, int height) {
        long now = System.nanoTime();
        graphics.fill(0, 0, width, height, 0x1000374B);
        for (int y = 0; y < height; y += 3) {
            graphics.fill(0, y, width, y + 1, 0x1B000000);
        }

        // Moving phosphor/vertical-sync band.
        int band = Math.floorMod((int) (now / 16_000_000L), Math.max(1, height + 80)) - 40;
        graphics.fill(0, band, width, Math.min(height, band + 22), 0x0B9BEAFF);

        // True curved-face mask in screen space. This bends the visible aperture
        // without touching the shaderpack's own framebuffer or render pipeline.
        int corner = Math.min(34, Math.max(18, Math.min(width, height) / 18));
        for (int y = 0; y < corner; y++) {
            double normalized = (corner - y) / (double) corner;
            int inset = (int) Math.round(corner * Math.sqrt(Math.max(0.0D,
                    1.0D - (1.0D - normalized) * (1.0D - normalized))));
            graphics.fill(0, y, inset, y + 1, 0xF4000000);
            graphics.fill(width - inset, y, width, y + 1, 0xF4000000);
            graphics.fill(0, height - y - 1, inset, height - y, 0xF4000000);
            graphics.fill(width - inset, height - y - 1, width, height - y, 0xF4000000);
        }
        int edge = 15;
        for (int i = 0; i < edge; i++) {
            int alpha = 10 + i * 4;
            int color = alpha << 24;
            graphics.fill(i, corner / 2, i + 1, height - corner / 2, color);
            graphics.fill(width - i - 1, corner / 2, width - i, height - corner / 2, color);
            graphics.fill(corner / 2, i, width - corner / 2, i + 1, color);
            graphics.fill(corner / 2, height - i - 1, width - corner / 2, height - i, color);
        }

        // Small chromatic fringe at the glass edges, subtle enough not to fight
        // Oculus/Iris colour grading.
        graphics.fill(8, corner, 10, height - corner, 0x1237B9FF);
        graphics.fill(width - 10, corner, width - 8, height - corner, 0x12FF493B);

        renderFrame(graphics, width, height);
        if ((now / 220_000_000L) != interferenceSeed) {
            interferenceSeed = now / 220_000_000L;
        }
    }

    private static void renderFrame(GuiGraphics graphics, int width, int height) {
        int m = 18;
        int l = 22;
        int c = 0xDEE9FAFF;
        graphics.fill(m, m, m + l, m + 2, c);
        graphics.fill(m, m, m + 2, m + l, c);
        graphics.fill(width - m - l, m, width - m, m + 2, c);
        graphics.fill(width - m - 2, m, width - m, m + l, c);
        graphics.fill(m, height - m - 2, m + l, height - m, c);
        graphics.fill(m, height - m - l, m + 2, height - m, c);
        graphics.fill(width - m - l, height - m - 2, width - m, height - m, c);
        graphics.fill(width - m - 2, height - m - l, width - m, height - m, c);
    }

    private static void renderPower(GuiGraphics graphics, Minecraft minecraft,
            int width, int height) {
        int x = width - 168;
        int y = height - 38;
        graphics.drawString(minecraft.font, ScpFonts.roboto("AUXILIARY POWER"),
                x, y - 12, 0xFFF0FAFF, false);
        graphics.fill(x, y, x + 142, y + 7, 0xB4142732);
        graphics.fill(x + 1, y + 1,
                x + 1 + Math.round(140 * Scp079PlayableClient.power() / 100.0F),
                y + 6, 0xFFBDEEFF);
        graphics.drawString(minecraft.font,
                ScpFonts.roboto(Scp079PlayableClient.power() + " / 100"),
                x + 92, y - 12, 0xFFF0FAFF, false);
    }

    private static TargetKind aimedTarget(Minecraft minecraft) {
        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || minecraft.level == null) {
            return TargetKind.NONE;
        }
        BlockPos pos = hit.getBlockPos();
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-4, -4, -4),
                pos.offset(4, 4, 4))) {
            BlockState state = minecraft.level.getBlockState(cursor);
            if (TeslaGateStructure.isController(state)) return TargetKind.TESLA;
        }
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-3, -3, -3),
                pos.offset(3, 3, 3))) {
            if (FacilityModule.isFacilityDoor(minecraft.level.getBlockState(cursor))) {
                return TargetKind.DOOR;
            }
        }
        return TargetKind.NONE;
    }

    private static ScreenPoint project(Vec3 point, Vec3 cameraPos, Vec3 right,
            Vec3 up, Vec3 look, Matrix4f projection, int width, int height) {
        Vec3 delta = point.subtract(cameraPos);
        float vx = (float) delta.dot(right);
        float vy = (float) delta.dot(up);
        float vz = (float) -delta.dot(look);
        Vector4f clip = projection.transform(new Vector4f(vx, vy, vz, 1.0F));
        float w = clip.w();
        if (!Float.isFinite(w) || w <= 1.0E-5F) return null;
        float nx = clip.x() / w;
        float ny = clip.y() / w;
        if (!Float.isFinite(nx) || !Float.isFinite(ny)) return null;
        return new ScreenPoint((nx * 0.5F + 0.5F) * width,
                (0.5F - ny * 0.5F) * height);
    }

    private static boolean visibleFromCamera(Minecraft minecraft, Vec3 camera,
            Vec3 target) {
        BlockHitResult hit = minecraft.level.clip(new ClipContext(camera, target,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() != HitResult.Type.BLOCK
                || camera.distanceToSqr(hit.getLocation()) + 0.30D
                >= camera.distanceToSqr(target);
    }

    private static int scpNumber(Entity entity) {
        if (entity instanceof Scp106Entity) return 106;
        if (entity instanceof Scp131AEntity || entity instanceof Scp131BEntity) return 131;
        if (entity instanceof Scp173Entity) return 173;
        if (entity instanceof Scp939Entity) return 939;
        return -1;
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

    private enum TargetKind { NONE, DOOR, TESLA }
    private record ScreenPoint(float x, float y) { }
    private record RecognitionBox(int x1, int y1, int x2, int y2,
            String label, int color) { }
}
