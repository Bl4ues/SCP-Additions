package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.death.DeathSpectateCoordinator;
import net.mcreator.scpadditions.network.DeathSpectateRequestPacket;

import java.util.UUID;

/**
 * Client orbit camera shared by the normal and MineZero death-screen live feeds.
 * Target selection and world/chunk streaming are server-authoritative so feeds
 * continue to work at arbitrary distance and across dimensions.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class MineZeroSpectateClient {
    private static final long SWITCH_HOLD_MS = 240L;
    private static final long SWITCH_FADE_MS = 760L;
    private static final long ACQUIRE_TIMEOUT_MS = 3200L;
    private static final long LOST_CONNECTION_MS = 1250L;
    private static final UUID NIL = new UUID(0L, 0L);
    private static final float DEFAULT_ORBIT_PITCH = 10.0F;
    private static final double CAMERA_DISTANCE = 4.35D;

    private static UUID targetId;
    private static String targetDisplayName = "No living personnel";
    private static ArmorStand cameraRig;
    private static ClientLevel cameraRigLevel;
    private static CameraType previousCameraType;
    private static boolean active;
    private static boolean requestPending;
    private static boolean serverStateKnown;
    private static boolean orbitInitialized;
    private static boolean connectionLost;
    private static int availableTargets;
    /** Absolute world-space yaw for the orbit, independent of target body rotation. */
    private static float orbitYaw;
    private static float orbitPitch = DEFAULT_ORBIT_PITCH;
    private static long switchedAt = -1L;
    private static long feedReadyAt = -1L;
    private static long lostConnectionAt = -1L;

    private MineZeroSpectateClient() {
    }

    public static boolean active() { return active; }

    public static boolean transferActive() {
        return active || requestPending;
    }

    public static boolean connectionLost() {
        return connectionLost;
    }

    /** Reset stale roster knowledge and ask the current server about survivors. */
    public static void refreshServerState() {
        serverStateKnown = false;
        availableTargets = 0;
        queryAvailability();
    }

    public static void queryAvailability() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new DeathSpectateRequestPacket(DeathSpectateCoordinator.ACTION_QUERY));
    }

    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) return;
        if (!active) {
            previousCameraType = minecraft.options.getCameraType();
            orbitInitialized = false;
            orbitPitch = DEFAULT_ORBIT_PITCH;
        }
        active = true;
        requestPending = true;
        targetId = null;
        targetDisplayName = "Acquiring personnel feed...";
        beginHandoff();
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new DeathSpectateRequestPacket(DeathSpectateCoordinator.ACTION_START));
    }

    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hadTransfer = (active || requestPending) && !connectionLost;
        if (minecraft.getConnection() != null && hadTransfer) {
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new DeathSpectateRequestPacket(DeathSpectateCoordinator.ACTION_STOP));
        }
        requestPending = false;
        targetId = null;
        targetDisplayName = "No living personnel";
        stopLocalCamera();
    }

    public static void cycle(int direction) {
        if (!active || connectionLost
                || Minecraft.getInstance().getConnection() == null
                || availableTargets <= 1) {
            return;
        }
        int action = direction < 0
                ? DeathSpectateCoordinator.ACTION_CYCLE_PREVIOUS
                : DeathSpectateCoordinator.ACTION_CYCLE_NEXT;
        targetId = null;
        targetDisplayName = "Acquiring personnel feed...";
        orbitInitialized = false;
        beginHandoff();
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new DeathSpectateRequestPacket(action));
    }

    public static void orbit(double deltaX, double deltaY) {
        if (!active || connectionLost) return;
        ensureOrbitInitialized(target());
        orbitYaw += (float) deltaX * 0.42F;
        orbitPitch = Mth.clamp(
                orbitPitch + (float) deltaY * 0.32F, -42.0F, 62.0F);
        updateCamera(1.0F);
    }

    public static String targetName() {
        return targetDisplayName == null || targetDisplayName.isBlank()
                ? "Acquiring personnel feed..." : targetDisplayName;
    }

    /** Authoritative survivor count supplied by the server. */
    public static boolean hasTargets() {
        // Keep the feed layout alive long enough to show CONNECTION LOST before
        // the death screen retracts it and removes Spectate.
        if (connectionLost) return true;
        if (serverStateKnown) return availableTargets > 0;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return false;
        UUID self = minecraft.player.getUUID();
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            if (!self.equals(id) && info.getGameMode() != GameType.SPECTATOR) return true;
        }
        return false;
    }

    /** Arrow controls are useful only if there is another live feed to switch to. */
    public static boolean hasMultipleTargets() {
        if (connectionLost) return false;
        if (serverStateKnown) return availableTargets > 1;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return false;
        UUID self = minecraft.player.getUUID();
        int count = 0;
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            if (!self.equals(id) && info.getGameMode() != GameType.SPECTATOR
                    && ++count > 1) {
                return true;
            }
        }
        return false;
    }

    public static int availableTargetCount() {
        return Math.max(0, availableTargets);
    }

    /** Invoked reflectively by DeathSpectateStatePacket. */
    public static void receiveServerState(boolean serverActive, UUID id,
            String name, int targetCount) {
        boolean wasActive = active || requestPending;
        serverStateKnown = true;
        availableTargets = Math.max(0, targetCount);
        requestPending = false;

        if (!serverActive || id == null || NIL.equals(id)) {
            targetId = null;
            if (wasActive && availableTargets <= 0) {
                beginLostConnection();
                return;
            }
            targetDisplayName = "No living personnel";
            stopLocalCamera();
            return;
        }

        boolean changed = targetId == null || !targetId.equals(id);
        active = true;
        connectionLost = false;
        lostConnectionAt = -1L;
        targetId = id;
        targetDisplayName = name == null || name.isBlank()
                ? "Acquiring personnel feed..." : name;
        if (changed) {
            orbitInitialized = false;
            orbitPitch = DEFAULT_ORBIT_PITCH;
            beginHandoff();
        }
        updateCamera(1.0F);
    }

    /** Reapply the camera every GUI frame so vanilla cannot leave it on the observer. */
    public static void updateForRender(float partialTick) {
        if (!active || connectionLost) return;
        updateCamera(Mth.clamp(partialTick, 0.0F, 1.0F));
    }

    /** Static/noise strength for the current camera hand-off. */
    public static float switchInterference() {
        if (connectionLost) return 1.0F;
        if (switchedAt < 0L) return 0.0F;
        long now = Util.getMillis();
        if (feedReadyAt < 0L) {
            return now - switchedAt < ACQUIRE_TIMEOUT_MS ? 1.0F : 0.92F;
        }
        long readyAge = Math.max(0L, now - feedReadyAt);
        if (readyAge <= SWITCH_HOLD_MS) return 1.0F;
        float t = Mth.clamp((readyAge - SWITCH_HOLD_MS)
                / (float) SWITCH_FADE_MS, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return 1.0F - t;
    }

    /** Opaque cover used to hide remote chunk/dimension acquisition. */
    public static float switchOcclusion() {
        if (connectionLost) return 1.0F;
        if (switchedAt < 0L) return 0.0F;
        long now = Util.getMillis();
        if (feedReadyAt < 0L) return 1.0F;
        long readyAge = Math.max(0L, now - feedReadyAt);
        if (readyAge <= SWITCH_HOLD_MS) return 1.0F;
        float t = Mth.clamp((readyAge - SWITCH_HOLD_MS)
                / (float) SWITCH_FADE_MS, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return 1.0F - t;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;

        if (connectionLost) {
            if (lostConnectionAt >= 0L
                    && Util.getMillis() - lostConnectionAt >= LOST_CONNECTION_MS) {
                targetDisplayName = "No living personnel";
                stopLocalCamera();
            }
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (cameraRigLevel != null && cameraRigLevel != minecraft.level) {
            cameraRig = null;
            cameraRigLevel = null;
        }
        updateCamera(1.0F);
    }

    private static void beginHandoff() {
        switchedAt = Util.getMillis();
        feedReadyAt = -1L;
        connectionLost = false;
        lostConnectionAt = -1L;
    }

    private static void beginLostConnection() {
        active = true;
        requestPending = false;
        connectionLost = true;
        lostConnectionAt = Util.getMillis();
        switchedAt = lostConnectionAt;
        feedReadyAt = -1L;
        orbitInitialized = false;
        targetDisplayName = "CONNECTION LOST";
        // Keep the last rig frozen underneath the opaque static for the short
        // disconnect presentation. It will be discarded when the timer expires.
    }

    private static void ensureRig() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (cameraRig != null && cameraRigLevel == minecraft.level) return;
        ArmorStand created = EntityType.ARMOR_STAND.create(minecraft.level);
        if (created == null) return;
        cameraRig = created;
        cameraRigLevel = minecraft.level;
        cameraRig.setInvisible(true);
        cameraRig.setNoGravity(true);
    }

    private static void ensureOrbitInitialized(AbstractClientPlayer target) {
        if (orbitInitialized) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (target != null) {
            orbitYaw = target.getYRot();
        } else if (minecraft.player != null) {
            orbitYaw = minecraft.player.getYRot();
        } else {
            orbitYaw = 0.0F;
        }
        orbitPitch = DEFAULT_ORBIT_PITCH;
        orbitInitialized = true;
    }

    private static void updateCamera(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        AbstractClientPlayer target = target();
        if (target != null && feedReadyAt < 0L) {
            feedReadyAt = Util.getMillis();
        }
        ensureOrbitInitialized(target);
        ensureRig();
        if (cameraRig == null) return;

        Vec3 focus;
        if (target != null) {
            double x = Mth.lerp(partialTick, target.xo, target.getX());
            double y = Mth.lerp(partialTick, target.yo, target.getY());
            double z = Mth.lerp(partialTick, target.zo, target.getZ());
            focus = new Vec3(x, y + Math.max(0.9D,
                    target.getBbHeight() * 0.72D), z);
        } else {
            // During a dimension hand-off the target entity can arrive a few
            // frames after the observer. Keep the camera rig attached to the
            // streamed anchor, but the UI fully occludes this acquisition state.
            double x = Mth.lerp(partialTick, minecraft.player.xo,
                    minecraft.player.getX());
            double y = Mth.lerp(partialTick, minecraft.player.yo,
                    minecraft.player.getY());
            double z = Mth.lerp(partialTick, minecraft.player.zo,
                    minecraft.player.getZ());
            focus = new Vec3(x, y + 1.15D, z);
        }

        Vec3 direction = Vec3.directionFromRotation(orbitPitch, orbitYaw);
        Vec3 camera = focus.subtract(direction.scale(CAMERA_DISTANCE));

        Vec3 delta = focus.subtract(camera);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float lookYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x))
                - 90.0D);
        float lookPitch = (float) -Math.toDegrees(
                Math.atan2(delta.y, horizontal));

        cameraRig.xo = camera.x;
        cameraRig.yo = camera.y;
        cameraRig.zo = camera.z;
        cameraRig.setPos(camera.x, camera.y, camera.z);
        cameraRig.yRotO = lookYaw;
        cameraRig.xRotO = lookPitch;
        cameraRig.setYRot(lookYaw);
        cameraRig.setXRot(lookPitch);

        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (minecraft.getCameraEntity() != cameraRig) {
            minecraft.setCameraEntity(cameraRig);
        }
    }

    private static AbstractClientPlayer target() {
        Minecraft minecraft = Minecraft.getInstance();
        if (targetId == null || minecraft.level == null) return null;
        Player player = minecraft.level.getPlayerByUUID(targetId);
        return player instanceof AbstractClientPlayer clientPlayer
                && clientPlayer.isAlive() && !clientPlayer.isSpectator()
                ? clientPlayer : null;
    }

    private static void stopLocalCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        active = false;
        requestPending = false;
        connectionLost = false;
        cameraRig = null;
        cameraRigLevel = null;
        orbitInitialized = false;
        switchedAt = -1L;
        feedReadyAt = -1L;
        lostConnectionAt = -1L;
        if (minecraft.player != null) minecraft.setCameraEntity(minecraft.player);
        if (previousCameraType != null) minecraft.options.setCameraType(previousCameraType);
        previousCameraType = null;
    }
}
