package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import com.bl4ues.scpinventory.sound.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class Scp173StoneDragEvents {
    private static final double TRACK_RANGE = 48.0D;
    private static final double TRACK_RANGE_SQR = TRACK_RANGE * TRACK_RANGE;
    private static final double MIN_DISTANCE_SQR = 1.2D * 1.2D;
    private static final double STUCK_MOVE_EPSILON_SQR = 0.025D * 0.025D;
    private static final int SCRAP_SOUND_COOLDOWN_TICKS = 8;
    private static final int SCRAP_RANDOM_COOLDOWN_TICKS = 8;
    private static final int DRAG_START_STUCK_TICKS = 8;
    private static final int DRAG_SOUND_COOLDOWN_TICKS = 38;
    private static final int DRAG_RANDOM_COOLDOWN_TICKS = 26;

    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> STUCK_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PROCESSED_TICK = new HashMap<>();
    private static final Map<UUID, Integer> NEXT_SCRAP_TICK = new HashMap<>();
    private static final Map<UUID, Integer> NEXT_DRAG_TICK = new HashMap<>();

    private Scp173StoneDragEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(TRACK_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= TRACK_RANGE_SQR)) {
            updateObstructedMovementSounds(scp173);
        }
    }

    private static void updateObstructedMovementSounds(Scp173Entity scp173) {
        UUID id = scp173.getUUID();
        if (LAST_PROCESSED_TICK.getOrDefault(id, -1) == scp173.tickCount) {
            return;
        }
        LAST_PROCESSED_TICK.put(id, scp173.tickCount);

        if (!isTryingToMoveWhileBlocked(scp173)) {
            LAST_POSITIONS.put(id, scp173.position());
            STUCK_TICKS.put(id, 0);
            return;
        }

        Vec3 current = scp173.position();
        Vec3 previous = LAST_POSITIONS.put(id, current);
        if (previous == null) {
            STUCK_TICKS.put(id, 0);
            return;
        }

        if (previous.distanceToSqr(current) > STUCK_MOVE_EPSILON_SQR) {
            STUCK_TICKS.put(id, 0);
            return;
        }

        int stuckTicks = STUCK_TICKS.getOrDefault(id, 0) + 1;
        STUCK_TICKS.put(id, stuckTicks);
        playStoneScrapImpact(scp173);
        if (stuckTicks >= DRAG_START_STUCK_TICKS) {
            playStoneDrag(scp173);
        }
    }

    private static void playStoneScrapImpact(Scp173Entity scp173) {
        UUID id = scp173.getUUID();
        int nextTick = NEXT_SCRAP_TICK.getOrDefault(id, 0);
        if (scp173.tickCount < nextTick) {
            return;
        }

        scp173.level().playSound(
                null,
                scp173.getX(),
                scp173.getY() + 0.35D,
                scp173.getZ(),
                ModSounds.STONE_SCRAP.get(),
                SoundSource.HOSTILE,
                0.42F,
                0.88F + (scp173.getRandom().nextFloat() * 0.18F)
        );
        NEXT_SCRAP_TICK.put(id, scp173.tickCount + SCRAP_SOUND_COOLDOWN_TICKS + scp173.getRandom().nextInt(SCRAP_RANDOM_COOLDOWN_TICKS + 1));
    }

    private static void playStoneDrag(Scp173Entity scp173) {
        UUID id = scp173.getUUID();
        int nextTick = NEXT_DRAG_TICK.getOrDefault(id, 0);
        if (scp173.tickCount < nextTick) {
            return;
        }

        scp173.level().playSound(
                null,
                scp173.getX(),
                scp173.getY() + 0.35D,
                scp173.getZ(),
                ModSounds.STONE_DRAG.get(),
                SoundSource.HOSTILE,
                0.45F,
                0.90F + (scp173.getRandom().nextFloat() * 0.12F)
        );
        NEXT_DRAG_TICK.put(id, scp173.tickCount + DRAG_SOUND_COOLDOWN_TICKS + scp173.getRandom().nextInt(DRAG_RANDOM_COOLDOWN_TICKS + 1));
    }

    private static boolean isTryingToMoveWhileBlocked(Scp173Entity scp173) {
        if (!scp173.isActivated() || !scp173.isScraping()) {
            return false;
        }

        Entity target = scp173.getTarget();
        if (!(target instanceof Player player)
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()
                || scp173.isObservedBy(player)) {
            return false;
        }

        double distance = scp173.distanceToSqr(player);
        return distance >= MIN_DISTANCE_SQR && distance <= TRACK_RANGE_SQR;
    }
}
