package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork.DoorMapEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compact server-authoritative door topology used by the SCP-079 map. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079DoorMapClientState {
    private static List<DoorMapEntry> entries = List.of();
    private static Map<Long, DoorMapEntry> byPosition = Map.of();
    private static int refreshTicks;

    private Scp079DoorMapClientState() {
    }

    public static List<DoorMapEntry> entries() {
        return entries;
    }

    public static void update(List<DoorMapEntry> next) {
        entries = next == null ? List.of() : List.copyOf(next);
        Map<Long, DoorMapEntry> indexed = new HashMap<>();
        for (DoorMapEntry entry : entries) {
            indexed.put(entry.pos().asLong(), entry);
        }
        byPosition = Map.copyOf(indexed);
    }

    public static DoorMapEntry at(BlockPos pos) {
        return pos == null ? null : byPosition.get(pos.asLong());
    }

    public static void clear() {
        entries = List.of();
        byPosition = Map.of();
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
