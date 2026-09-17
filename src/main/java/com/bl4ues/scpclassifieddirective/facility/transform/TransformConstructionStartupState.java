package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Keeps expensive transformed-construction indexing out of Minecraft's spawn
 * preparation phase. Existing proxy blocks are already persisted in chunks, so
 * startup only needs to let vanilla load them. The transform manager may repair
 * or rebuild proxies normally once the integrated/dedicated server has started.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformConstructionStartupState {
    private static final Set<MinecraftServer> READY =
            Collections.newSetFromMap(new WeakHashMap<>());

    private TransformConstructionStartupState() {
    }

    public static synchronized boolean isReady(MinecraftServer server) {
        return server != null && READY.contains(server);
    }

    @SubscribeEvent
    public static synchronized void onServerStarted(ServerStartedEvent event) {
        READY.add(event.getServer());
    }

    @SubscribeEvent
    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        READY.remove(event.getServer());
    }
}
