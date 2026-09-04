package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local physical-host camera and presentation for a player-controlled SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class Scp079PlayableClient {
    private static final double FAR_DISTANCE = 2.85D;
    private static final double FRONT_DISTANCE = 1.02D;
    private static final double WALL_MARGIN = 0.12D;

    private static boolean active;
    private static ResourceLocation hostDimension = LevelIds.OVERWORLD;
    private static BlockPos hostPos = BlockPos.ZERO;
    private static int power;
    private static boolean auxiliaryOnline;
    private static boolean networkAvailable;
    private static ArmorStand cameraRig;
    private static ClientLevel cameraRigLevel;
    private static CameraType previousCameraType;

    private Scp079PlayableClient() {
    }

    public static void receive(boolean enabled, ResourceLocation dimension,
            BlockPos pos, int currentPower, boolean auxiliary,
            boolean network) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean wasActive = active;
        active = enabled;
        hostDimension = dimension == null ? LevelIds.OVERWORLD : dimension;
        hostPos = pos == null ? BlockPos.ZERO : pos.immutable();
        power = Mth.clamp(currentPower, 0, 100);
        auxiliaryOnline = auxiliary;
        networkAvailable = network;
        if (active && !wasActive) {
            previousCameraType = minecraft.options.getCameraType();
        } else if (!active && wasActive) {
            stopCamera(minecraft);
        }
    }

    public static boolean active() {
        return active;
    }

    public static boolean networkAvailable() {
        return active && networkAvailable;
    }

    public static ResourceLocation hostDimension() {
        return hostDimension;
    }

    public static BlockPos hostPos() {
        return hostPos;
    }

    public static int power() {
        return power;
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
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideVanillaHud(RenderGuiOverlayEvent.Pre event) {
        if (!active) return;
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            renderLocalHud(event.getGuiGraphics());
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent event) {
        if (active) event.setCanceled(true);
    }

    private static void updateCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.level.dimension().location().equals(hostDimension)) {
            return;
        }
        ensureRig(minecraft);
        if (cameraRig == null) return;

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
        cameraRig.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        cameraRig.setXRot((float) -Math.toDegrees(Math.atan2(look.y, horizontal)));
        cameraRig.yRotO = cameraRig.getYRot();
        cameraRig.xRotO = cameraRig.getXRot();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (minecraft.getCameraEntity() != cameraRig) {
            minecraft.setCameraEntity(cameraRig);
        }
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
        cameraRig = null;
        cameraRigLevel = null;
        previousCameraType = null;
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

        String ap = "AUXILIARY POWER  " + power + " / 100";
        int boxWidth = 148;
        int x = width - boxWidth - 20;
        int y = height - 42;
        graphics.drawString(minecraft.font, ScpFonts.roboto(ap), x, y,
                white, false);
        graphics.fill(x, y + 13, x + boxWidth, y + 19, 0xA9182731);
        graphics.fill(x + 1, y + 14,
                x + 1 + Math.round((boxWidth - 2) * power / 100.0F),
                y + 18, auxiliaryOnline ? cyan : 0xFF65727A);
        if (networkAvailable) {
            graphics.drawString(minecraft.font,
                    ScpFonts.roboto("[TAB] FACILITY MAP"),
                    x, y - 15, cyan, false);
        } else if (!auxiliaryOnline) {
            graphics.drawString(minecraft.font,
                    ScpFonts.roboto("NETWORK OFFLINE"),
                    x, y - 15, 0xFFB86A6A, false);
        }
    }

    /** Avoids a client-only dependency on ResourceKey constants in static init. */
    private static final class LevelIds {
        private static final ResourceLocation OVERWORLD =
                new ResourceLocation("minecraft", "overworld");
    }
}
