package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
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
    private static final long SWITCH_INTERFERENCE_MS = 520L;
    private static final UUID NIL = new UUID(0L, 0L);

    private static UUID targetId;
    private static String targetDisplayName = "No living personnel";
    private static ArmorStand cameraRig;
    private static ClientLevel cameraRigLevel;
    private static CameraType previousCameraType;
    private static boolean active;
    private static boolean requestPending;
    private static boolean serverStateKnown;
    private static int availableTargets;
    private static float orbitYaw;
    private static float orbitPitch = 10.0F;
    private static long switchedAt = -1L;

    private MineZeroSpectateClient() {
    }

    public static boolean active() { return active; }

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
            orbitYaw = 0.0F;
            orbitPitch = 10.0F;
        }
        // Mark the presentation active immediately so the death screen can start
        // sliding while the server moves this observer to the remote feed.
        active = true;
        requestPending = true;
        targetId = null;
        targetDisplayName = "Acquiring personnel feed...";
        kickInterference();
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new DeathSpectateRequestPacket(DeathSpectateCoordinator.ACTION_START));
    }

    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hadTransfer = active || requestPending;
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
        if (!active || Minecraft.getInstance().getConnection() == null) return;
        int action = direction < 0
                ? DeathSpectateCoordinator.ACTION_CYCLE_PREVIOUS
                : DeathSpectateCoordinator.ACTION_CYCLE_NEXT;
        kickInterference();
        targetId = null;
        targetDisplayName = "Acquiring personnel feed...";
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new DeathSpectateRequestPacket(action));
    }

    public static void orbit(double deltaX, double deltaY) {
        if (!active) return;
        orbitYaw += (float) deltaX * 0.38F;
        orbitPitch = net.minecraft.util.Mth.clamp(
                orbitPitch + (float) deltaY * 0.28F, -38.0F, 58.0F);
        updateCamera();
    }

    public static String targetName() {
        return targetDisplayName == null || targetDisplayName.isBlank()
                ? "Acquiring personnel feed..." : targetDisplayName;
    }

    /** Authoritative survivor count supplied by the server. */
    public static boolean hasTargets() {
        if (serverStateKnown) return availableTargets > 0;

        // Short-lived fallback while the server query is in flight. START is
        // still validated and selected by the server, so this cannot authorize
        // an invalid feed.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return false;
        UUID self = minecraft.player.getUUID();
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            if (!self.equals(id) && info.getGameMode() != GameType.SPECTATOR) return true;
        }
        return false;
    }

    /** Invoked reflectively by DeathSpectateStatePacket. */
    public static void receiveServerState(boolean serverActive, UUID id,
            String name, int targetCount) {
        serverStateKnown = true;
        availableTargets = Math.max(0, targetCount);
        requestPending = false;

        if (!serverActive || id == null || NIL.equals(id)) {
            targetId = null;
            targetDisplayName = "No living personnel";
            stopLocalCamera();
            return;
        }

        boolean changed = targetId == null || !targetId.equals(id);
        active = true;
        targetId = id;
        targetDisplayName = name == null || name.isBlank()
                ? "Acquiring personnel feed..." : name;
        if (changed) {
            orbitYaw = 0.0F;
            orbitPitch = 10.0F;
            kickInterference();
        }
        updateCamera();
    }

    /** 1 -> 0 burst used by the death screen when changing camera feeds. */
    public static float switchInterference() {
        if (switchedAt < 0L) return 0.0F;
        long elapsed = Math.max(0L, Util.getMillis() - switchedAt);
        if (elapsed >= SWITCH_INTERFERENCE_MS) return 0.0F;
        float t = net.minecraft.util.Mth.clamp(
                elapsed / (float) SWITCH_INTERFERENCE_MS, 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return 1.0F - t;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;

        // Cross-dimension teleports replace ClientLevel. Never keep an ArmorStand
        // camera created in the previous dimension.
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraRigLevel != null && cameraRigLevel != minecraft.level) {
            cameraRig = null;
            cameraRigLevel = null;
        }
        updateCamera();
    }

    private static void kickInterference() {
        switchedAt = Util.getMillis();
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

    private static void updateCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer target = target();
        if (minecraft.level == null || target == null) {
            // While chunks for a new dimension/feed are arriving, leave the
            // camera on the observer. The death screen masks everything except
            // the feed area until the target entity is tracked locally.
            if (minecraft.player != null && minecraft.getCameraEntity() != minecraft.player) {
                minecraft.setCameraEntity(minecraft.player);
            }
            return;
        }
        ensureRig();
        if (cameraRig == null) return;

        Vec3 focus = target.position().add(0.0D,
                Math.max(0.9D, target.getBbHeight() * 0.72D), 0.0D);
        float yaw = target.getYRot() + orbitYaw;
        Vec3 direction = Vec3.directionFromRotation(orbitPitch, yaw);
        Vec3 camera = focus.subtract(direction.scale(4.35D));

        Vec3 delta = focus.subtract(camera);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float lookYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x))
                - 90.0D);
        float lookPitch = (float) -Math.toDegrees(
                Math.atan2(delta.y, horizontal));

        cameraRig.setPos(camera.x, camera.y, camera.z);
        cameraRig.setYRot(lookYaw);
        cameraRig.setXRot(lookPitch);
        cameraRig.yRotO = lookYaw;
        cameraRig.xRotO = lookPitch;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.setCameraEntity(cameraRig);
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
        cameraRig = null;
        cameraRigLevel = null;
        switchedAt = -1L;
        if (minecraft.player != null) minecraft.setCameraEntity(minecraft.player);
        if (previousCameraType != null) minecraft.options.setCameraType(previousCameraType);
        previousCameraType = null;
    }
}
