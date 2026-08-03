package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.RoombaEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Starts at most one tracked loop per nearby Roomba. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoombaLoopSoundEvents {
    private static final double RANGE = 48.0D;
    private static final int SCAN_INTERVAL_TICKS = 8;
    private static final Map<Integer, RoombaLoopSound> LOOPS =
            new HashMap<>();

    private RoombaLoopSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopEverything();
            return;
        }
        if (minecraft.player.tickCount % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        Set<Integer> seen = new HashSet<>();
        AABB area = minecraft.player.getBoundingBox().inflate(RANGE);
        for (RoombaEntity roomba : minecraft.level.getEntitiesOfClass(
                RoombaEntity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved())) {
            int id = roomba.getId();
            seen.add(id);
            RoombaLoopSound existing = LOOPS.get(id);
            if (existing == null || existing.isStopped()) {
                RoombaLoopSound created = new RoombaLoopSound(roomba);
                LOOPS.put(id, created);
                minecraft.getSoundManager().play(created);
            }
        }

        for (Integer id : new ArrayList<>(LOOPS.keySet())) {
            RoombaLoopSound sound = LOOPS.get(id);
            if (!seen.contains(id) || sound == null || sound.isStopped()) {
                stopOne(sound);
                LOOPS.remove(id);
            }
        }
    }

    private static void stopOne(RoombaLoopSound sound) {
        if (sound != null && !sound.isStopped()) {
            sound.finish();
        }
    }

    private static void stopEverything() {
        LOOPS.values().forEach(RoombaLoopSoundEvents::stopOne);
        LOOPS.clear();
    }
}
