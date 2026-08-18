package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Local orbit camera shared by the normal and MineZero death-screen live feeds. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class MineZeroSpectateClient {
    private static final long SWITCH_INTERFERENCE_MS = 520L;

    private static UUID targetId;
    private static ArmorStand cameraRig;
    private static Entity previousCamera;
    private static CameraType previousCameraType;
    private static boolean active;
    private static float orbitYaw;
    private static float orbitPitch = 10.0F;
    private static long switchedAt = -1L;

    private MineZeroSpectateClient() {
    }

    public static boolean active() { return active; }

    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        List<AbstractClientPlayer> targets = targets();
        if (targets.isEmpty()) return;
        if (!active) {
            previousCamera = minecraft.getCameraEntity();
            previousCameraType = minecraft.options.getCameraType();
            orbitYaw = 0.0F;
            orbitPitch = 10.0F;
        }
        active = true;
        targetId = targets.get(0).getUUID();
        kickInterference();
        ensureRig();
        updateCamera();
    }

    public static void stop() {
        if (!active && cameraRig == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        active = false;
        targetId = null;
        cameraRig = null;
        switchedAt = -1L;
        if (minecraft.player != null) {
            minecraft.setCameraEntity(previousCamera != null
                    ? previousCamera : minecraft.player);
        }
        if (previousCameraType != null) minecraft.options.setCameraType(previousCameraType);
        previousCamera = null;
        previousCameraType = null;
    }

    public static void cycle(int direction) {
        List<AbstractClientPlayer> targets = targets();
        if (targets.isEmpty()) {
            stop();
            return;
        }
        int current = -1;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getUUID().equals(targetId)) {
                current = i;
                break;
            }
        }
        int next = Math.floorMod((current < 0 ? 0 : current) + direction,
                targets.size());
        targetId = targets.get(next).getUUID();
        orbitYaw = 0.0F;
        orbitPitch = 10.0F;
        kickInterference();
        updateCamera();
    }

    public static void orbit(double deltaX, double deltaY) {
        if (!active) return;
        orbitYaw += (float) deltaX * 0.38F;
        orbitPitch = net.minecraft.util.Mth.clamp(
                orbitPitch + (float) deltaY * 0.28F, -38.0F, 58.0F);
        updateCamera();
    }

    public static String targetName() {
        AbstractClientPlayer target = target();
        return target == null ? "No living personnel" : target.getName().getString();
    }

    public static boolean hasTargets() { return !targets().isEmpty(); }

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
        if (target() == null) {
            List<AbstractClientPlayer> targets = targets();
            if (targets.isEmpty()) {
                stop();
                return;
            }
            targetId = targets.get(0).getUUID();
            kickInterference();
        }
        updateCamera();
    }

    private static void kickInterference() {
        switchedAt = Util.getMillis();
    }

    private static void ensureRig() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraRig != null || minecraft.level == null) return;
        ArmorStand created = EntityType.ARMOR_STAND.create(minecraft.level);
        if (created == null) return;
        cameraRig = created;
        cameraRig.setInvisible(true);
        cameraRig.setNoGravity(true);
    }

    private static void updateCamera() {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer target = target();
        if (minecraft.level == null || target == null) return;
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
        if (targetId == null) return null;
        for (AbstractClientPlayer player : targets()) {
            if (player.getUUID().equals(targetId)) return player;
        }
        return null;
    }

    private static List<AbstractClientPlayer> targets() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return List.of();
        UUID self = minecraft.player.getUUID();
        return minecraft.level.players().stream()
                .filter(player -> !player.getUUID().equals(self))
                .filter(Entity::isAlive)
                .filter(player -> !player.isSpectator())
                .sorted(Comparator.comparing(player ->
                        player.getName().getString().toLowerCase()))
                .toList();
    }
}
