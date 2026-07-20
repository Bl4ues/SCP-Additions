package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public final class Scp131LoopSoundEvents {
    private static final double RANGE = 64.0D;
    private static final int SCAN_INTERVAL_TICKS = 4;
    private static final double MOVEMENT_EPSILON_SQR = 0.0025D * 0.0025D;
    private static final Map<Integer, Scp131LoopSound> IDLE = new HashMap<>();
    private static final Map<Integer, Scp131LoopSound> MOVING = new HashMap<>();
    private static final Map<Integer, Vec3> LAST_POSITIONS = new HashMap<>();

    private Scp131LoopSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopEverything();
            return;
        }
        if (minecraft.player.tickCount % SCAN_INTERVAL_TICKS != 0) return;

        Set<Integer> seen = new HashSet<>();
        AABB area = minecraft.player.getBoundingBox().inflate(RANGE);
        for (AbstractScp131Entity pod : minecraft.level.getEntitiesOfClass(
                AbstractScp131Entity.class, area, entity -> entity.isAlive() && !entity.isRemoved())) {
            int id = pod.getId();
            seen.add(id);
            startLoop(minecraft, IDLE, pod, false);

            Vec3 current = pod.position();
            Vec3 previous = LAST_POSITIONS.put(id, current);
            boolean moved = previous != null
                    && previous.distanceToSqr(current) > MOVEMENT_EPSILON_SQR;
            if (moved) {
                startLoop(minecraft, MOVING, pod, true);
            } else {
                stopOne(MOVING.remove(id));
            }
        }

        prune(IDLE, seen);
        prune(MOVING, seen);
        LAST_POSITIONS.keySet().removeIf(id -> !seen.contains(id));
    }

    private static void startLoop(Minecraft minecraft, Map<Integer, Scp131LoopSound> loops,
                                  AbstractScp131Entity entity, boolean movementLoop) {
        Scp131LoopSound existing = loops.get(entity.getId());
        if (existing != null && !existing.isStopped()) return;

        Scp131LoopSound created = new Scp131LoopSound(entity, movementLoop);
        loops.put(entity.getId(), created);
        minecraft.getSoundManager().play(created);
    }

    private static void prune(Map<Integer, Scp131LoopSound> loops, Set<Integer> seen) {
        for (Integer id : new ArrayList<>(loops.keySet())) {
            Scp131LoopSound sound = loops.get(id);
            if (!seen.contains(id) || sound == null || sound.isStopped()) {
                stopOne(sound);
                loops.remove(id);
            }
        }
    }

    private static void stopOne(Scp131LoopSound sound) {
        if (sound != null && !sound.isStopped()) sound.finish();
    }

    private static void stopEverything() {
        IDLE.values().forEach(Scp131LoopSoundEvents::stopOne);
        MOVING.values().forEach(Scp131LoopSoundEvents::stopOne);
        IDLE.clear();
        MOVING.clear();
        LAST_POSITIONS.clear();
    }
}
