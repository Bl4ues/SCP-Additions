package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173Sounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173MovementScrapeEvents {
    private static final double TRACK_RANGE = 48.0D;
    private static final double TRACK_RANGE_SQR = TRACK_RANGE * TRACK_RANGE;
    private static final double MIN_MOVEMENT_SQR = 0.006D * 0.006D;
    private static final int SCRAPE_INTERVAL_TICKS = 3;
    private static final int SCRAPE_RANDOM_INTERVAL_TICKS = 2;
    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PROCESSED_TICK = new HashMap<>();
    private static final Map<UUID, Integer> NEXT_SCRAPE_TICK = new HashMap<>();

    private Scp173MovementScrapeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        AABB area = player.getBoundingBox().inflate(TRACK_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= TRACK_RANGE_SQR)) updateMovementScrape(scp173);
    }

    private static void updateMovementScrape(Scp173Entity scp173) {
        UUID id = scp173.getUUID();
        if (LAST_PROCESSED_TICK.getOrDefault(id, -1) == scp173.tickCount) return;
        LAST_PROCESSED_TICK.put(id, scp173.tickCount);
        Vec3 current = scp173.position();
        Vec3 previous = LAST_POSITIONS.put(id, current);
        if (!scp173.isActivated() || !scp173.isScraping() || previous == null
                || previous.distanceToSqr(current) <= MIN_MOVEMENT_SQR) return;
        int nextTick = NEXT_SCRAPE_TICK.getOrDefault(id, 0);
        if (scp173.tickCount < nextTick) return;
        scp173.level().playSound(null, scp173.getX(), scp173.getY() + 0.35D, scp173.getZ(),
                Scp173Sounds.STONE_SCRAP.get(), SoundSource.HOSTILE, 0.50F,
                0.92F + scp173.getRandom().nextFloat() * 0.16F);
        NEXT_SCRAPE_TICK.put(id, scp173.tickCount + SCRAPE_INTERVAL_TICKS
                + scp173.getRandom().nextInt(SCRAPE_RANDOM_INTERVAL_TICKS + 1));
    }
}
