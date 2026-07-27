package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Maintains one positional electrical loop for each nearby powered ceiling lamp. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CeilingLampAudioClient {
    private static final Map<LampKey, CeilingLampLoopSound> LOOPS =
            new HashMap<>();
    private static final int DISCOVERY_INTERVAL_TICKS = 20;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;

    private static int discoveryTicks;

    private CeilingLampAudioClient() {
    }

    public static void ensureLoop(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        LampKey key = new LampKey(clientLevel.dimension(), pos.asLong());
        CeilingLampLoopSound existing = LOOPS.get(key);
        if (existing != null && !existing.isFinished()) return;

        CeilingLampLoopSound sound = new CeilingLampLoopSound(clientLevel, pos);
        LOOPS.put(key, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();

        Iterator<Map.Entry<LampKey, CeilingLampLoopSound>> iterator =
                LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LampKey, CeilingLampLoopSound> entry = iterator.next();
            CeilingLampLoopSound sound = entry.getValue();
            if (minecraft.level == null
                    || !entry.getKey().dimension().equals(
                    minecraft.level.dimension())) {
                sound.finish();
            }
            if (sound.isFinished()) iterator.remove();
        }

        if (minecraft.level == null || minecraft.player == null) {
            discoveryTicks = 0;
            return;
        }

        discoveryTicks++;
        if (discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;
        discoverNearbyPoweredLamps(minecraft.level,
                minecraft.player.blockPosition());
    }

    private static void discoverNearbyPoweredLamps(ClientLevel level,
            BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -VERTICAL_DISCOVERY_RADIUS;
                y <= VERTICAL_DISCOVERY_RADIUS; y++) {
            for (int x = -HORIZONTAL_DISCOVERY_RADIUS;
                    x <= HORIZONTAL_DISCOVERY_RADIUS; x++) {
                for (int z = -HORIZONTAL_DISCOVERY_RADIUS;
                        z <= HORIZONTAL_DISCOVERY_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y,
                            center.getZ() + z);
                    if (!level.hasChunkAt(cursor)) continue;
                    if (CeilingLampLoopSound.shouldPlayFor(
                            level.getBlockState(cursor))) {
                        ensureLoop(level, cursor);
                    }
                }
            }
        }
    }

    private record LampKey(ResourceKey<Level> dimension, long pos) {
    }
}
