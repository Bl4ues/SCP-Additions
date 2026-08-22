package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.MainMenuPlayPanelsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Keep custom multiplayer status probes away from the render/input thread.
 * A remote timeout must degrade into a normal "cannot connect" state rather
 * than escape through Screen#mouseClicked and terminate the client.
 */
@Mixin(value = MainMenuPlayPanelsClient.class, remap = false)
public abstract class MainMenuServerListSafetyMixin {
    @Inject(method = "refreshServers", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpClassifiedDirective$refreshServersOffThread(
            @Coerce Object state, CallbackInfo callback) {
        callback.cancel();
        try {
            ServerStatusPinger previous = field(state, "serverPinger",
                    ServerStatusPinger.class);
            if (previous != null) {
                try {
                    previous.removeAll();
                } catch (Exception ignored) {
                }
            }

            Map<?, ?> icons = field(state, "serverIcons", Map.class);
            if (icons != null) {
                for (Object value : icons.values()) {
                    if (value instanceof FaviconTexture favicon) {
                        try {
                            favicon.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
                icons.clear();
            }

            Set<String> dirtyIcons = stringSet(state, "dirtyServerIcons");
            if (dirtyIcons != null) dirtyIcons.clear();

            ServerList serverList = new ServerList(Minecraft.getInstance());
            serverList.load();
            List<ServerData> servers = new ArrayList<>();
            for (int index = 0; index < serverList.size(); index++) {
                servers.add(serverList.get(index));
            }

            ServerStatusPinger pinger = new ServerStatusPinger();
            setField(state, "serverList", serverList);
            setField(state, "servers", servers);
            setField(state, "serverPinger", pinger);
            setField(state, "scrollOffset", 0);
            setField(state, "contextIndex", -1);

            for (ServerData server : servers) {
                server.pinged = true;
                CompletableFuture.runAsync(() -> pingSafely(
                        pinger, server, dirtyIcons));
            }
        } catch (Throwable throwable) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not load the custom multiplayer server list safely",
                    throwable);
            try {
                setField(state, "servers", new ArrayList<ServerData>());
                setField(state, "serverPinger", null);
                setField(state, "scrollOffset", 0);
                setField(state, "contextIndex", -1);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpClassifiedDirective$tickPingerSafely(
            net.minecraftforge.event.TickEvent.ClientTickEvent event,
            CallbackInfo callback) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof com.bl4ues.scpclassifieddirective.client.CustomMainMenuScreen screen)) {
            return;
        }
        try {
            Object state = states().get(screen);
            if (state == null || !isMultiplayerOpen(state)) return;
            ServerStatusPinger pinger = field(state, "serverPinger",
                    ServerStatusPinger.class);
            if (pinger == null) return;
            callback.cancel();
            try {
                pinger.tick();
            } catch (Exception exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Ignoring failed multiplayer status-pinger tick",
                        exception);
                try {
                    pinger.removeAll();
                } catch (Exception ignored) {
                }
                setField(state, "serverPinger", null);
            }
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Could not access custom multiplayer pinger state",
                    exception);
        }
    }

    private static boolean isMultiplayerOpen(Object state)
            throws ReflectiveOperationException {
        Object open = rawField(state, "open");
        Object mode = rawField(state, "mode");
        return Boolean.TRUE.equals(open) && mode != null
                && "MULTIPLAYER".equals(mode.toString());
    }

    private static void pingSafely(ServerStatusPinger pinger,
            ServerData server, Set<String> dirtyIcons) {
        try {
            pinger.pingServer(server, () -> {
                try {
                    ServerList.saveSingleServer(server);
                } catch (Exception exception) {
                    ScpClassifiedDirectiveMod.LOGGER.debug(
                            "Could not persist ping result for {}", server.ip,
                            exception);
                }
                if (dirtyIcons != null) dirtyIcons.add(server.ip);
            });
        } catch (Exception exception) {
            markUnavailable(server);
            if (dirtyIcons != null) dirtyIcons.add(server.ip);
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Server ping failed for {} without crashing the menu",
                    server.ip, exception);
        }
    }

    private static void markUnavailable(ServerData server) {
        server.motd = Component.translatable("multiplayer.status.cannot_connect");
        server.status = CommonComponents.EMPTY;
        server.ping = -1L;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> states() throws ReflectiveOperationException {
        Field field = MainMenuPlayPanelsClient.class.getDeclaredField("STATES");
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> stringSet(Object target, String name)
            throws ReflectiveOperationException {
        Object value = rawField(target, name);
        return value instanceof Set<?> set ? (Set<String>) set : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name, Class<T> type)
            throws ReflectiveOperationException {
        Object value = rawField(target, name);
        return type.isInstance(value) ? (T) value : null;
    }

    private static Object rawField(Object target, String name)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
