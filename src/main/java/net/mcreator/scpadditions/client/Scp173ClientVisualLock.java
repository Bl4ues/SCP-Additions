package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Rejects late interpolation while the local player is visibly watching 173. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp173ClientVisualLock {
    private static final Map<UUID, VisualSnapshot> LOCKS = new HashMap<>();
    private static final Method CLIENT_OBSERVED = method(
            "isClientObservedByLocalPlayer");
    private static boolean reflectionWarningLogged;

    private Scp173ClientVisualLock() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            LOCKS.clear();
            return;
        }

        Set<UUID> present = new HashSet<>();
        for (Scp173Entity statue : minecraft.level.getEntitiesOfClass(
                Scp173Entity.class,
                minecraft.player.getBoundingBox().inflate(128.0D))) {
            present.add(statue.getUUID());
            enforce(statue);
        }
        LOCKS.keySet().removeIf(id -> !present.contains(id));
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Scp173Entity statue) {
            enforce(statue);
        }
    }

    private static void enforce(Scp173Entity statue) {
        boolean observed = isLocallyObserved(statue);
        if (!observed) {
            LOCKS.remove(statue.getUUID());
            return;
        }

        VisualSnapshot lock = LOCKS.computeIfAbsent(statue.getUUID(),
                ignored -> new VisualSnapshot(statue.getX(), statue.getY(),
                        statue.getZ(), statue.getYRot()));
        statue.absMoveTo(lock.x(), lock.y(), lock.z(), lock.yaw(), 0.0F);
        statue.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean isLocallyObserved(Scp173Entity statue) {
        if (BlinkClient.isBlinkClosedLocally()) return false;
        if (CLIENT_OBSERVED != null) {
            try {
                return (boolean) CLIENT_OBSERVED.invoke(statue);
            } catch (ReflectiveOperationException exception) {
                warnReflection(exception);
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && statue.isObservedBy(minecraft.player);
    }

    private static Method method(String name, Class<?>... parameters) {
        try {
            Method method = Scp173Entity.class.getDeclaredMethod(
                    name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void warnReflection(Exception exception) {
        if (reflectionWarningLogged) return;
        reflectionWarningLogged = true;
        ScpAdditionsMod.LOGGER.warn(
                "SCP-173 client visual lock lost observation access",
                exception);
    }

    private record VisualSnapshot(double x, double y, double z, float yaw) {
    }
}
