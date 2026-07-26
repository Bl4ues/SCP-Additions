package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Starts one positional fading Wither loop for every affected player. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WitherAudioClient {
    private static final Map<Integer, WitherLoopSound> LOOPS =
            new HashMap<>();

    private WitherAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopEverything(minecraft);
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (!player.isAlive() || player.isRemoved()
                    || !player.hasEffect(MobEffects.WITHER)) {
                continue;
            }
            WitherLoopSound existing = LOOPS.get(player.getId());
            if (existing != null && !existing.isStopped()) continue;

            WitherLoopSound created = new WitherLoopSound(player);
            LOOPS.put(player.getId(), created);
            minecraft.getSoundManager().play(created);
        }

        Iterator<WitherLoopSound> iterator = LOOPS.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isStopped()) iterator.remove();
        }
    }

    private static void stopEverything(Minecraft minecraft) {
        LOOPS.values().forEach(sound -> {
            sound.finishImmediately();
            minecraft.getSoundManager().stop(sound);
        });
        LOOPS.clear();
    }
}
