package net.mcreator.scpadditions.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common-side bridge for packets whose effects are entirely client-side.
 *
 * The target class is intentionally named as a string. This keeps every
 * net.minecraft.client type out of packet classes that must also be loaded by
 * dedicated servers during channel registration.
 */
public final class ClientPacketExecutor {
    private static final String TARGET =
            "net.mcreator.scpadditions.client.ClientPacketActions";
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private ClientPacketExecutor() {
    }

    public static void run(String action) {
        if (FMLEnvironment.dist != Dist.CLIENT || action == null || action.isBlank()) return;
        try {
            Method method = METHODS.computeIfAbsent(action, ClientPacketExecutor::resolve);
            if (method != null) method.invoke(null);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.error("Could not execute client packet action {}", action, exception);
        }
    }

    public static void setScp106ChaseMusic(UUID sourceId, boolean active) {
        if (FMLEnvironment.dist != Dist.CLIENT || sourceId == null) return;
        try {
            Method method = Class.forName(TARGET).getMethod(
                    "setScp106ChaseMusic", UUID.class, boolean.class);
            method.invoke(null, sourceId, active);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not update SCP-106 chase music", exception);
        }
    }

    private static Method resolve(String action) {
        try {
            return Class.forName(TARGET).getMethod(action);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.error("Could not resolve client packet action {}", action, exception);
            return null;
        }
    }
}
