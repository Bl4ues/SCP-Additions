package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork.DoorMapEntry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Compact server-authoritative door topology used by the SCP-079 map. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079DoorMapClientState {
    private static List<DoorMapEntry> entries = List.of();
    private static int refreshTicks;

    private Scp079DoorMapClientState() {
    }

    public static List<DoorMapEntry> entries() {
        return entries;
    }

    public static void update(List<DoorMapEntry> next) {
        entries = next == null ? List.of() : List.copyOf(next);
    }

    public static void clear() {
        entries = List.of();
        refreshTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Scp079PlayableClient.active()) {
            if (!entries.isEmpty() || refreshTicks != 0) clear();
            return;
        }
        if (refreshTicks > 0) {
            refreshTicks--;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        refreshTicks = minecraft.screen instanceof Scp079FacilityMapScreen
                ? 10 : 40;
        Scp079PlayableNetwork.requestDoorMap(
                Scp079PlayableClient.hostDimension());
    }
}
