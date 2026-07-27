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

/** Maintains at most one subtle looping sound for each visible lit lamp. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CeilingLampAudioClient {
    private static final Map<LampKey, CeilingLampLoopSound> LOOPS =
            new HashMap<>();

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
    }

    private record LampKey(ResourceKey<Level> dimension, long pos) {
    }
}
