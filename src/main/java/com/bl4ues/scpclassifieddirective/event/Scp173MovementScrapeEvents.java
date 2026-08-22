package com.bl4ues.scpclassifieddirective.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Sounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plays the short scrape variants only when SCP-173 turns or stops. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173MovementScrapeEvents {
    private static final double TRACK_RANGE = 64.0D;
    private static final double TRACK_RANGE_SQR = TRACK_RANGE * TRACK_RANGE;
    private static final double MIN_MOVEMENT_SQR = 0.003D * 0.003D;
    private static final double TURN_DOT_THRESHOLD = 0.9238795325112867D;
    private static final int TURN_SOUND_COOLDOWN_TICKS = 5;
    private static final int STOP_CONFIRM_TICKS = 2;
    private static final long STATE_EXPIRY_TICKS = 200L;

    private static final Map<UUID, MovementState> STATES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PROCESSED_TICK = new HashMap<>();

    private Scp173MovementScrapeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(TRACK_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(
                Scp173Entity.class, area,
                entity -> entity.isAlive()
                        && entity.distanceToSqr(player) <= TRACK_RANGE_SQR)) {
            updateMovementAccent(scp173);
        }

        long gameTime = player.serverLevel().getGameTime();
        if (gameTime % STATE_EXPIRY_TICKS == 0L) {
            STATES.entrySet().removeIf(entry ->
                    gameTime - entry.getValue().lastSeenGameTime
                            > STATE_EXPIRY_TICKS);
            LAST_PROCESSED_TICK.keySet().removeIf(id -> !STATES.containsKey(id));
        }
    }

    private static void updateMovementAccent(Scp173Entity scp173) {
        UUID id = scp173.getUUID();
        if (LAST_PROCESSED_TICK.getOrDefault(id, -1) == scp173.tickCount) {
            return;
        }
        LAST_PROCESSED_TICK.put(id, scp173.tickCount);

        Vec3 current = scp173.position();
        MovementState state = STATES.computeIfAbsent(id,
                ignored -> new MovementState(current));
        state.lastSeenGameTime = scp173.level().getGameTime();

        Vec3 displacement = current.subtract(state.lastPosition);
        state.lastPosition = current;
        Vec3 horizontal = new Vec3(displacement.x, 0.0D, displacement.z);
        boolean moved = scp173.isActivated() && scp173.isScraping()
                && horizontal.lengthSqr() > MIN_MOVEMENT_SQR;

        if (moved) {
            Vec3 direction = horizontal.normalize();
            if (state.moving && state.lastDirection != null
                    && state.lastDirection.dot(direction) < TURN_DOT_THRESHOLD
                    && scp173.tickCount >= state.nextTurnSoundTick) {
                playAccent(scp173);
                state.nextTurnSoundTick = scp173.tickCount
                        + TURN_SOUND_COOLDOWN_TICKS;
            }
            state.moving = true;
            state.stillTicks = 0;
            state.lastDirection = direction;
            return;
        }

        if (!state.moving) return;
        state.stillTicks++;
        boolean confirmedStop = !scp173.isActivated() || !scp173.isScraping()
                || state.stillTicks >= STOP_CONFIRM_TICKS;
        if (!confirmedStop) return;

        state.moving = false;
        state.stillTicks = 0;
        state.lastDirection = null;
        playAccent(scp173);
    }

    private static void playAccent(Scp173Entity scp173) {
        scp173.level().playSound(null, scp173.getX(), scp173.getY() + 0.35D,
                scp173.getZ(), Scp173Sounds.STONE_SCRAP.get(),
                SoundSource.HOSTILE, 0.72F,
                0.94F + scp173.getRandom().nextFloat() * 0.12F);
    }

    private static final class MovementState {
        private Vec3 lastPosition;
        private Vec3 lastDirection;
        private boolean moving;
        private int stillTicks;
        private int nextTurnSoundTick;
        private long lastSeenGameTime;

        private MovementState(Vec3 position) {
            this.lastPosition = position;
        }
    }
}
