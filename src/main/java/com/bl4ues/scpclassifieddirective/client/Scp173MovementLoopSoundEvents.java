package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Starts stone_scrap.ogg on movement and stops it when movement ends. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173MovementLoopSoundEvents {
    private static final double RANGE = 64.0D;
    private static final double MOVEMENT_EPSILON_SQR = 0.003D * 0.003D;
    private static final int STOP_CONFIRM_TICKS = 2;

    private static final Map<Integer, Scp173MovementLoopSound> LOOPS =
            new HashMap<>();
    private static final Map<Integer, MotionState> STATES = new HashMap<>();

    private Scp173MovementLoopSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopEverything();
            return;
        }

        Set<Integer> seen = new HashSet<>();
        AABB area = minecraft.player.getBoundingBox().inflate(RANGE);
        for (Scp173Entity scp173 : minecraft.level.getEntitiesOfClass(
                Scp173Entity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved())) {
            int id = scp173.getId();
            seen.add(id);
            MotionState state = STATES.computeIfAbsent(id,
                    ignored -> new MotionState(scp173.position()));

            Vec3 current = scp173.position();
            Vec3 displacement = current.subtract(state.lastPosition);
            state.lastPosition = current;
            boolean moved = scp173.isActivated() && scp173.isScraping()
                    && new Vec3(displacement.x, 0.0D, displacement.z)
                    .lengthSqr() > MOVEMENT_EPSILON_SQR;

            if (moved) {
                state.stillTicks = 0;
                startLoop(minecraft, scp173);
            } else if (LOOPS.containsKey(id)) {
                state.stillTicks++;
                if (!scp173.isActivated() || !scp173.isScraping()
                        || state.stillTicks >= STOP_CONFIRM_TICKS) {
                    stopOne(LOOPS.remove(id));
                    state.stillTicks = 0;
                }
            }
        }

        prune(seen);
    }

    private static void startLoop(Minecraft minecraft, Scp173Entity entity) {
        Scp173MovementLoopSound existing = LOOPS.get(entity.getId());
        if (existing != null && !existing.isStopped()) return;

        Scp173MovementLoopSound created =
                new Scp173MovementLoopSound(entity);
        LOOPS.put(entity.getId(), created);
        minecraft.getSoundManager().play(created);
    }

    private static void prune(Set<Integer> seen) {
        for (Integer id : new ArrayList<>(LOOPS.keySet())) {
            Scp173MovementLoopSound sound = LOOPS.get(id);
            if (!seen.contains(id) || sound == null || sound.isStopped()) {
                stopOne(sound);
                LOOPS.remove(id);
            }
        }
        STATES.keySet().removeIf(id -> !seen.contains(id));
    }

    private static void stopOne(Scp173MovementLoopSound sound) {
        if (sound != null && !sound.isStopped()) sound.finish();
    }

    private static void stopEverything() {
        LOOPS.values().forEach(Scp173MovementLoopSoundEvents::stopOne);
        LOOPS.clear();
        STATES.clear();
    }

    private static final class MotionState {
        private Vec3 lastPosition;
        private int stillTicks;

        private MotionState(Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }
    }
}
