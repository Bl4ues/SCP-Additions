package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp079CameraNavigationNetwork;
import com.bl4ues.scpclassifieddirective.network.Scp079CameraNavigationNetwork.CameraNode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Client copy of the surveillance topology exposed only to playable SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraNetworkClientState {
    private static final int REFRESH_TICKS = 40;

    private static List<CameraNode> nodes = List.of();
    private static int refreshTicks;

    private Scp079CameraNetworkClientState() {
    }

    public static void update(List<CameraNode> next) {
        nodes = next == null ? List.of() : List.copyOf(next);
    }

    public static void clear() {
        nodes = List.of();
        refreshTicks = 0;
    }

    public static List<CameraNode> nodes() {
        return nodes;
    }

    public static CameraNode camera(UUID cameraId) {
        if (cameraId == null) return null;
        for (CameraNode node : nodes) {
            if (cameraId.equals(node.cameraId())) return node;
        }
        return null;
    }

    public static Set<UUID> cameraRoomIds() {
        Set<UUID> result = new LinkedHashSet<>();
        for (CameraNode node : nodes) result.add(node.roomId());
        return Set.copyOf(result);
    }

    public static boolean hasCamera(UUID roomId) {
        if (roomId == null) return false;
        for (CameraNode node : nodes) {
            if (roomId.equals(node.roomId())) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Scp079PlayableClient.active()) {
            if (!nodes.isEmpty() || refreshTicks != 0) clear();
            return;
        }
        if (refreshTicks > 0) {
            refreshTicks--;
            return;
        }
        refreshTicks = REFRESH_TICKS;
        Scp079CameraNavigationNetwork.requestTopology();
    }
}
