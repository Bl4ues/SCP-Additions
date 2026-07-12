package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.AbstractScp131Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class EyePodSounds {
    private static final double RANGE = 64.0D;
    private static final double EPSILON = 0.00000625D;
    private static final Map<Integer, Scp131LoopSound> IDLE = new HashMap<>();
    private static final Map<Integer, Scp131LoopSound> MOVING = new HashMap<>();
    private static final Map<Integer, Vec3> LAST = new HashMap<>();

    private EyePodSounds() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stopEverything();
            return;
        }

        Set<Integer> seen = new HashSet<>();
        AABB area = mc.player.getBoundingBox().inflate(RANGE);
        for (AbstractScp131Entity pod : mc.level.getEntitiesOfClass(AbstractScp131Entity.class, area)) {
            if (!pod.isAlive() || pod.isRemoved()) {
                continue;
            }
            int id = pod.getId();
            seen.add(id);
            startLoop(mc, IDLE, pod, false);
            Vec3 now = pod.position();
            Vec3 last = LAST.put(id, now);
            boolean moved = last != null && last.distanceToSqr(now) > EPSILON;
            if (moved) {
                startLoop(mc, MOVING, pod, true);
            } else {
                stopOne(MOVING.remove(id));
            }
        }
        prune(IDLE, seen);
        prune(MOVING, seen);
        for (Integer id : new ArrayList<>(LAST.keySet())) {
            if (!seen.contains(id)) {
                LAST.remove(id);
            }
        }
    }

    private static void startLoop(Minecraft mc, Map<Integer, Scp131LoopSound> map, AbstractScp131Entity pod, boolean moving) {
        Scp131LoopSound current = map.get(pod.getId());
        if (current != null && !current.isStopped()) {
            return;
        }
        Scp131LoopSound created = new Scp131LoopSound(pod, moving);
        map.put(pod.getId(), created);
        mc.getSoundManager().play(created);
    }

    private static void prune(Map<Integer, Scp131LoopSound> map, Set<Integer> seen) {
        for (Integer id : new ArrayList<>(map.keySet())) {
            Scp131LoopSound sound = map.get(id);
            if (!seen.contains(id) || sound == null || sound.isStopped()) {
                stopOne(sound);
                map.remove(id);
            }
        }
    }

    private static void stopOne(Scp131LoopSound sound) {
        if (sound != null && !sound.isStopped()) {
            sound.finish();
        }
    }

    private static void stopEverything() {
        for (Scp131LoopSound sound : IDLE.values()) {
            stopOne(sound);
        }
        for (Scp131LoopSound sound : MOVING.values()) {
            stopOne(sound);
        }
        IDLE.clear();
        MOVING.clear();
        LAST.clear();
    }
}
