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
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/** Physical-host orbit, surveillance feed camera, CRT HUD and 079 input routing. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class Scp079PlayableClient {
    private static final ResourceLocation DOOR_ICON = resource(
            "textures/gui/scp079/icons/door.png");
    private static final ResourceLocation TESLA_ICON = resource(
            "textures/gui/scp079/icons/tesla_gate.png");
    private static final ResourceLocation CAMERA_ICON = resource(
            "textures/gui/scp079/icons/camera.png");
    private static final double FAR_DISTANCE = 2.85D;
    private static final double FRONT_DISTANCE = 1.02D;
    private static final double WALL_MARGIN = 0.12D;
    private static final long SWITCH_INTERFERENCE_NANOS = 260_000_000L;

    private static boolean active;
    private static ResourceLocation hostDimension = new ResourceLocation(
            "minecraft", "overworld");
    private static BlockPos hostPos = BlockPos.ZERO;
    private static int power;
    private static boolean auxiliaryOnline;
    private static boolean networkAvailable;
    private static UUID cameraId;
    private static String cameraName = "";
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static float baseYaw;
    private static float basePitch;
    private static float yawLimit;
    private static float minPitch = -55.0F;
    private static float maxPitch = 55.0F;
    private static float maxZoom = 1.0F;
    private static float zoom = 1.0F;
    private static long interferenceUntil;
    private static ArmorStand cameraRig;
    private static ClientLevel cameraRigLevel;
    private static CameraType previousCameraType;
    private static boolean cursorReleased;
    private static float frozenYaw;
    private static float frozenPitch;

    private Scp079PlayableClient() {
    }

    public static void receive(Scp079PlayableNetwork.State state) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean wasActive = active;
        UUID previousCamera = cameraId;
        active = state.active();
        hostDimension = state.dimension();
        hostPos = state.hostPos().immutable();
        power = Mth.clamp(state.power(), 0, 100);
        auxiliaryOnline = state.auxiliaryOnline();
        networkAvailable = state.networkAvailable();
        cameraId = state.cameraId();
        cameraName = state.cameraName();
        cameraPosition = new Vec3(state.cameraX(), state.cameraY(), state.cameraZ());
        baseYaw = state.baseYaw();
        basePitch = state.basePitch();
        yawLimit = state.yawLimit();
        minPitch = state.minPitch();
        maxPitch = state.maxPitch();
        maxZoom = Math.max(1.0F, state.maxZoom());

        if (active && !wasActive) {
            previousCameraType = minecraft.options.getCameraType();
        }
        if (!active && wasActive) {
            Scp079TrackingClientState.clear();
            stopCamera(minecraft);
            return;
        }
        if (cameraId != null && !cameraId.equals(previousCamera)) {
            interferenceUntil = System.nanoTime() + SWITCH_INTERFERENCE_NANOS;
            zoom = 1.0F;
            if (minecraft.player != null) {
                minecraft.player.setYRot(baseYaw);
                minecraft.player.setXRot(basePitch);
                minecraft.player.yRotO = baseYaw;
                minecraft.player.xRotO = basePitch;
            }
            if (minecraft.screen instanceof Scp079FacilityMapScreen) {
                minecraft.setScreen(null);
            }
        }
        if (cameraId == null && previousCamera != null) {
            zoom = 1.0F;
        }
    }

    public static boolean active() { return active; }
    public static boolean cameraMode() { return active && cameraId != null; }
    public static boolean networkAvailable() { return active && networkAvailable; }
    public static ResourceLocation hostDimension() { return hostDimension; }
    public static BlockPos hostPos() { return hostPos; }
    public static int power() { return power; }
    public static Vec3 viewPosition() {
        return cameraMode() ? cameraPosition : Vec3.atCenterOf(hostPos);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active) return;
        updateCamera();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!active) return;
        if (minecraft.player == null || minecraft.level == null
                || minecraft.getConnection() == null) {
            active = false;
            stopCamera(minecraft);
            return;
        }
        handleShiftCursor(minecraft);
        if (cameraMode() && minecraft.screen == null && !cursorReleased) {
            consumeCameraActions(minecraft);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!active || event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getKey() == GLFW.GLFW_KEY_TAB && minecraft.screen == null
                && networkAvailable) {
            Scp079FacilityMapScreen.open();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!cameraMode() || Minecraft.getInstance().screen != null) return;
        if (event.getScrollDelta() == 0.0D) return;
        zoom = Mth.clamp(zoom + (event.getScrollDelta() > 0.0D ? 0.18F : -0.18F),
                1.0F, maxZoom);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (cameraMode() && zoom > 1.0F) {
            event.setFOV(event.getFOV() / zoom);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideVanillaHud(RenderGuiOverlayEvent.Pre event) {
        if (!active) return;
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            if (cameraMode()) renderCameraHud(event.getGuiGraphics());
            else renderLocalHud(event.getGuiGraphics());
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderRecognition(RenderLevelStageEvent event) {
        if (!cameraMode()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || entity == minecraft.player
                    || entity.distanceToSqr(camera) > 42.0D * 42.0D) continue;
            String label;
            float r;
            float g;
            float b;
            if (entity instanceof Player) {
                label = "HUMAN";
                r = 0.18F;
                g = 0.78F;
                b = 1.0F;
            } else {
                int number = scpNumber(entity);
                if (number < 0) continue;
                label = "SCP-" + String.format("%03d", number);
                r = 1.0F;
                g = 0.18F;
                b = 0.16F;
            }
            Vec3 head = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            if (!visibleFromCamera(minecraft, camera, head)) continue;
            double half = Math.max(0.22D, entity.getBbWidth() * 0.42D);
            AABB face = new AABB(entity.getX() - half,
                    entity.getEyeY() - 0.18D, entity.getZ() - half,
                    entity.getX() + half, entity.getEyeY() + 0.34D,
                    entity.getZ() + half);
            LevelRenderer.renderLineBox(pose, lines, face, r, g, b, 0.95F);
            renderLabel(pose, buffers, event, entity, label,
                    color(r, g, b));
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderLabel(PoseStack pose,
            MultiBufferSource.BufferSource buffers, RenderLevelStageEvent event,
            Entity entity, String label, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(entity.getX(), entity.getEyeY() + 0.48D,
                entity.getZ());
        pose.mulPose(event.getCamera().rotation());
        pose.scale(-0.014F, -0.014F, 0.014F);
        float x = -minecraft.font.width(label) * 0.5F;
        Matrix4f matrix = pose.last().pose();
        minecraft.font.drawInBatch(ScpFonts.roboto(label), x, 0.0F,
                color, false, matrix, buffers,
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH,
                0x50000000, 15728880);
        pose.popPose();
    }

    private static boolean visibleFromCamera(Minecraft minecraft, Vec3 camera,
            Vec3 target) {
        BlockHitResult hit = minecraft.level.clip(new ClipContext(camera,
                target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                minecraft.player));
        return hit.getType() != HitResult.Type.BLOCK
                || camera.distanceToSqr(hit.getLocation()) + 0.30D
                >= camera.distanceToSqr(target);
    }

    private static void consumeCameraActions(Minecraft minecraft) {
        BlockPos target = aimedBlock(minecraft);
        if (target == null) return;
        while (minecraft.options.keyAttack.consumeClick()) {
            Scp079PlayableNetwork.requestAction(
                    Scp079PlayableManager.ManualAction.PRIMARY, target);
        }
        while (minecraft.options.keyUse.consumeClick()) {
            Scp079PlayableNetwork.requestAction(
                    Scp079PlayableManager.ManualAction.LOCK, target);
        }
    }

    private static void handleShiftCursor(Minecraft minecraft) {
        if (!cameraMode() || minecraft.screen != null) {
            if (cursorReleased) cursorReleased = false;
            return;
        }
        boolean shift = minecraft.options.keyShift.isDown();
        if (shift && !cursorReleased) {
            frozenYaw = minecraft.player.getYRot();
            frozenPitch = minecraft.player.getXRot();
            minecraft.mouseHandler.releaseMouse();
            GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),
                    GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            cursorReleased = true;
        } else if (!shift && cursorReleased) {
            minecraft.player.setYRot(frozenYaw);
            minecraft.player.setXRot(frozenPitch);
            cursorReleased = false;
            if (minecraft.isWindowActive()) minecraft.mouseHandler.grabMouse();
        }
        if (cursorReleased) {
            minecraft.player.setYRot(frozenYaw);
            minecraft.player.setXRot(frozenPitch);
            minecraft.player.yRotO = frozenYaw;
            minecraft.player.xRotO = frozenPitch;
        }
    }

    private static void updateCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        ensureRig(minecraft);
        if (cameraRig == null) return;
        if (cameraMode()) updateFeedCamera(minecraft);
        else updateLocalCamera(minecraft);
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (minecraft.getCameraEntity() != cameraRig) {
            minecraft.setCameraEntity(cameraRig);
        }
    }

    private static void updateFeedCamera(Minecraft minecraft) {
        float playerYaw = minecraft.player.getYRot();
        float deltaYaw = Mth.wrapDegrees(playerYaw - baseYaw);
        float yaw = baseYaw + Mth.clamp(deltaYaw, -yawLimit, yawLimit);
        float pitch = Mth.clamp(minecraft.player.getXRot(), minPitch, maxPitch);
        if (!cursorReleased) {
            minecraft.player.setYRot(yaw);
            minecraft.player.setXRot(pitch);
            minecraft.player.yRotO = yaw;
            minecraft.player.xRotO = pitch;
        } else {
            yaw = frozenYaw;
            pitch = frozenPitch;
        }
        cameraRig.setPos(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        cameraRig.setYRot(yaw);
        cameraRig.setXRot(pitch);
        cameraRig.yRotO = yaw;
        cameraRig.xRotO = pitch;
    }

    private static void updateLocalCamera(Minecraft minecraft) {
        if (!minecraft.level.dimension().location().equals(hostDimension)) return;
        BlockState state = minecraft.level.getBlockState(hostPos);
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        Vec3 focus = Vec3.atCenterOf(hostPos).add(0.0D, 0.12D, 0.0D);
        float yaw = minecraft.player.getYRot();
        float pitch = Mth.clamp(minecraft.player.getXRot(), -62.0F, 62.0F);
        Vec3 view = Vec3.directionFromRotation(pitch * 0.62F, yaw);
        Vec3 outward = view.scale(-1.0D).normalize();
        Vec3 flatOutward = new Vec3(outward.x, 0.0D, outward.z);
        if (flatOutward.lengthSqr() > 1.0E-6D) flatOutward = flatOutward.normalize();
        Vec3 front = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());
        double frontness = Math.max(0.0D, flatOutward.dot(front));
        frontness *= frontness;
        double desiredDistance = Mth.lerp(frontness,
                FAR_DISTANCE, FRONT_DISTANCE);
        Vec3 desired = focus.add(outward.scale(desiredDistance))
                .add(0.0D, 0.18D, 0.0D);
        Vec3 actual = avoidWalls(minecraft, focus, desired);
        cameraRig.setPos(actual.x, actual.y, actual.z);
        Vec3 look = focus.subtract(actual);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float rigYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float rigPitch = (float) -Math.toDegrees(Math.atan2(look.y, horizontal));
        cameraRig.setYRot(rigYaw);
        cameraRig.setXRot(rigPitch);
        cameraRig.yRotO = rigYaw;
        cameraRig.xRotO = rigPitch;
    }

    private static Vec3 avoidWalls(Minecraft minecraft, Vec3 focus,
            Vec3 desired) {
        BlockHitResult hit = minecraft.level.clip(new ClipContext(focus,
                desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                minecraft.player));
        if (hit.getType() != HitResult.Type.BLOCK) return desired;
        Vec3 towardFocus = focus.subtract(hit.getLocation());
        if (towardFocus.lengthSqr() < 1.0E-6D) return hit.getLocation();
        return hit.getLocation().add(towardFocus.normalize().scale(WALL_MARGIN));
    }

    private static void ensureRig(Minecraft minecraft) {
        if (minecraft.level == null) return;
        if (cameraRig != null && cameraRigLevel == minecraft.level) return;
        ArmorStand rig = EntityType.ARMOR_STAND.create(minecraft.level);
        if (rig == null) return;
        rig.setInvisible(true);
        rig.setNoGravity(true);
        cameraRig = rig;
        cameraRigLevel = minecraft.level;
    }

    private static void stopCamera(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.getCameraEntity() == cameraRig) {
            minecraft.setCameraEntity(minecraft.player);
        }
        if (previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
        }
        if (cursorReleased && minecraft.screen == null
                && minecraft.isWindowActive()) {
            minecraft.mouseHandler.grabMouse();
        }
        cursorReleased = false;
        cameraRig = null;
        cameraRigLevel = null;
        previousCameraType = null;
        cameraId = null;
        zoom = 1.0F;
    }

    private static void renderLocalHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int white = 0xFFE7F5FF;
        int cyan = 0xFF9CDCF7;
        int muted = 0xFF8AA2B1;
        graphics.drawString(minecraft.font, ScpFonts.titillium("SCP-079"),
                18, 18, white, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto("LOCAL HOST"),
                18, 31, cyan, false);
        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                hostDimension, hostPos);
        if (room != null) {
            if (!room.floorShortLabel().isBlank()) {
                graphics.drawString(minecraft.font,
                        ScpFonts.roboto(room.floorShortLabel()), 18, 45,
                        muted, false);
            }
            if (!room.name().isBlank()) {
                graphics.drawString(minecraft.font, ScpFonts.roboto(room.name()),
                        18, 57, white, false);
            }
        }
        renderPower(graphics, width, height, cyan, white);
        if (networkAvailable) {
            graphics.drawString(minecraft.font,
                    ScpFonts.roboto("[TAB] FACILITY MAP"),
                    width - 168, height - 57, cyan, false);
        } else if (!auxiliaryOnline) {
            graphics.drawString(minecraft.font,
                    ScpFonts.roboto("NETWORK OFFLINE"),
                    width - 168, height - 57, 0xFFB86A6A, false);
        }
    }

    private static void renderCameraHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        renderLowLightEnhancement(graphics, minecraft, width, height);
        renderCrt(graphics, width, height);

        FacilityRoomSnapshot room = FacilityMappingClientState.roomAt(
                hostDimension, BlockPos.containing(cameraPosition));
        graphics.drawString(minecraft.font,
                ScpFonts.roboto("CAMERA FEED ONLINE"), 34, 27,
                0xFFCDEFFF, false);
        int titleY = 43;
        if (room != null) {
            String floor = room.floorShortLabel().isBlank()
                    ? "UNASSIGNED" : room.floorShortLabel();
            graphics.drawString(minecraft.font, ScpFonts.roboto(floor),
                    34, titleY, 0xFF8FB8C8, false);
            titleY += 13;
            graphics.drawString(minecraft.font,
                    ScpFonts.titillium(room.name().toUpperCase()),
                    34, titleY, 0xFFF0FAFF, false);
        } else {
            graphics.drawString(minecraft.font,
                    ScpFonts.titillium(cameraName.toUpperCase()),
                    34, titleY, 0xFFF0FAFF, false);
        }

        int counterY = 82;
        graphics.drawString(minecraft.font, ScpFonts.roboto(
                        "TOTAL LIFEFORMS: "
                                + Scp079TrackingClientState.totalLifeforms()),
                34, counterY, 0xFFBDEEFF, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(
                        "TARGETS: " + Scp079TrackingClientState.targets()),
                34, counterY + 12, 0xFFBDEEFF, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(
                        "SCP SUBJECTS: "
                                + Scp079TrackingClientState.scpSubjects()),
                34, counterY + 24, 0xFFBDEEFF, false);

        graphics.drawString(minecraft.font,
                ScpFonts.roboto("OPEN FACILITY MAP  [TAB]"),
                width - 180, 28, 0xFFBDEEFF, false);
        graphics.drawString(minecraft.font,
                ScpFonts.roboto("HOLD SHIFT  CURSOR"),
                width - 180, 42, 0xFF7FA5B4, false);

        TargetKind target = aimedTarget(minecraft);
        if (target != TargetKind.NONE) {
            ResourceLocation icon = target == TargetKind.TESLA
                    ? TESLA_ICON : DOOR_ICON;
            int size = 30;
            int ix = width / 2 - size / 2;
            int iy = height / 2 - size / 2;
            RenderSystem.setShaderColor(0.75F, 0.94F, 1.0F, 1.0F);
            graphics.blit(icon, ix, iy, 0.0F, 0.0F, size, size, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            String primary = target == TargetKind.DOOR
                    ? "OPEN / CLOSE  [LMB]  " + cost(5.0D)
                    : "SUPPRESS  [LMB]  " + cost(12.0D);
            graphics.drawString(minecraft.font, ScpFonts.roboto(primary),
                    width - 190, height / 2 - 4, 0xFFEAF8FF, false);
            if (target == TargetKind.DOOR) {
                graphics.drawString(minecraft.font,
                        ScpFonts.roboto("LOCK  [RMB]  " + cost(12.0D)),
                        width - 190, height / 2 + 10,
                        0xFFEAF8FF, false);
            }
        } else {
            int size = 18;
            RenderSystem.setShaderColor(0.58F, 0.78F, 0.88F, 0.75F);
            graphics.blit(CAMERA_ICON, width / 2 - size / 2, 23,
                    0.0F, 0.0F, size, size, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        renderPower(graphics, width, height, 0xFFBDEEFF, 0xFFF0FAFF);
    }

    private static void renderPower(GuiGraphics graphics, int width, int height,
            int accent, int text) {
        Minecraft minecraft = Minecraft.getInstance();
        int x = width - 168;
        int y = height - 38;
        graphics.drawString(minecraft.font, ScpFonts.roboto("AUXILIARY POWER"),
                x, y - 12, text, false);
        graphics.fill(x, y, x + 142, y + 7, 0xB4142732);
        graphics.fill(x + 1, y + 1,
                x + 1 + Math.round(140 * power / 100.0F), y + 6, accent);
        graphics.drawString(minecraft.font,
                ScpFonts.roboto(power + " / 100"), x + 92, y - 12,
                text, false);
    }

    private static void renderLowLightEnhancement(GuiGraphics graphics,
            Minecraft minecraft, int width, int height) {
        BlockPos pos = BlockPos.containing(cameraPosition);
        if (minecraft.level == null
                || minecraft.level.getMaxLocalRawBrightness(pos) >= 5) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        graphics.fill(0, 0, width, height, 0x26256D80);
        RenderSystem.defaultBlendFunc();
        graphics.drawString(minecraft.font,
                ScpFonts.roboto("LOW-LIGHT ENHANCEMENT"), 34, height - 33,
                0xFF79DDF3, false);
    }

    private static void renderCrt(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0x10003B55);
        for (int y = 0; y < height; y += 3) {
            graphics.fill(0, y, width, y + 1, 0x19000000);
        }
        int steps = 11;
        for (int i = 0; i < steps; i++) {
            int alpha = 7 + i * 5;
            int color = alpha << 24;
            graphics.fill(i, 0, i + 1, height, color);
            graphics.fill(width - i - 1, 0, width - i, height, color);
            graphics.fill(0, i, width, i + 1, color);
            graphics.fill(0, height - i - 1, width, height - i, color);
        }
        // Stepped black corners suggest the curved CRT face without a static PNG.
        for (int i = 0; i < 8; i++) {
            int run = 25 - i * 3;
            graphics.fill(0, i, Math.max(0, run), i + 1, 0xD9000000);
            graphics.fill(width - Math.max(0, run), i, width, i + 1,
                    0xD9000000);
            graphics.fill(0, height - i - 1, Math.max(0, run), height - i,
                    0xD9000000);
            graphics.fill(width - Math.max(0, run), height - i - 1,
                    width, height - i, 0xD9000000);
        }
        int bracket = 21;
        int margin = 18;
        int white = 0xDDE9FAFF;
        graphics.fill(margin, margin, margin + bracket, margin + 2, white);
        graphics.fill(margin, margin, margin + 2, margin + bracket, white);
        graphics.fill(width - margin - bracket, margin, width - margin,
                margin + 2, white);
        graphics.fill(width - margin - 2, margin, width - margin,
                margin + bracket, white);
        graphics.fill(margin, height - margin - 2, margin + bracket,
                height - margin, white);
        graphics.fill(margin, height - margin - bracket, margin + 2,
                height - margin, white);
        graphics.fill(width - margin - bracket, height - margin - 2,
                width - margin, height - margin, white);
        graphics.fill(width - margin - 2, height - margin - bracket,
                width - margin, height - margin, white);

        if (System.nanoTime() < interferenceUntil) {
            long frame = System.nanoTime() / 8_000_000L;
            for (int i = 0; i < 9; i++) {
                int y = Math.floorMod((int) (frame * 17 + i * 41),
                        Math.max(1, height));
                int h = 1 + Math.floorMod((int) (frame + i * 3), 5);
                int offset = Math.floorMod((int) (frame * 13 + i * 29), 61)
                        - 30;
                graphics.fill(Math.max(0, offset), y,
                        Math.min(width, width + offset), Math.min(height, y + h),
                        i % 2 == 0 ? 0x8AC7F4FF : 0x8A07151E);
            }
        }
    }

    private static TargetKind aimedTarget(Minecraft minecraft) {
        BlockPos hit = aimedBlock(minecraft);
        if (hit == null || minecraft.level == null) return TargetKind.NONE;
        for (BlockPos cursor : BlockPos.betweenClosed(hit.offset(-4, -4, -4),
                hit.offset(4, 4, 4))) {
            BlockState state = minecraft.level.getBlockState(cursor);
            if (TeslaGateStructure.isController(state)) return TargetKind.TESLA;
        }
        for (BlockPos cursor : BlockPos.betweenClosed(hit.offset(-3, -3, -3),
                hit.offset(3, 3, 3))) {
            if (FacilityModule.isFacilityDoor(
                    minecraft.level.getBlockState(cursor))) return TargetKind.DOOR;
        }
        return TargetKind.NONE;
    }

    private static BlockPos aimedBlock(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        return hit instanceof BlockHitResult blockHit
                && hit.getType() == HitResult.Type.BLOCK
                ? blockHit.getBlockPos() : null;
    }

    private static String cost(double base) {
        Minecraft minecraft = Minecraft.getInstance();
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
                : String.format(java.util.Locale.ROOT, "%.1f AP", value);
    }

    private static int scpNumber(Entity entity) {
        if (entity instanceof Scp106Entity) return 106;
        if (entity instanceof Scp131AEntity || entity instanceof Scp131BEntity) return 131;
        if (entity instanceof Scp173Entity) return 173;
        if (entity instanceof Scp939Entity) return 939;
        return -1;
    }

    private static int color(float r, float g, float b) {
        return 0xFF000000 | ((int) (r * 255.0F) << 16)
                | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private enum TargetKind { NONE, DOOR, TESLA }
}
