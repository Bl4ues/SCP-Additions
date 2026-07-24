package net.mcreator.scpadditions.roamer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-side gameplay areas matching SCP-106's visible corrosion puddles. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106CorrosionFieldManager {
    public static final int CORROSION_LIFETIME_TICKS = 9 * 20;
    private static final int SLOWNESS_REFRESH_TICKS = 3 * 20;
    private static final int MAX_FIELDS_PER_LEVEL = 1024;
    private static final Map<ServerLevel, List<Field>> FIELDS =
            new WeakHashMap<>();

    private Scp106CorrosionFieldManager() {
    }

    public static void addTrail(ServerLevel level, Vec3 position) {
        add(level, position, 0.55D, CORROSION_LIFETIME_TICKS);
    }

    public static void addRanged(ServerLevel level, Vec3 position) {
        add(level, position, 0.68D, CORROSION_LIFETIME_TICKS);
    }

    public static void add(ServerLevel level, Vec3 position,
            double radius, int durationTicks) {
        if (level == null || position == null || radius <= 0.0D
                || durationTicks <= 0) {
            return;
        }

        long expiresAt = level.getGameTime() + durationTicks;
        synchronized (FIELDS) {
            List<Field> fields = FIELDS.computeIfAbsent(level,
                    ignored -> new ArrayList<>());

            for (Field field : fields) {
                if (Math.abs(field.position.y - position.y) <= 0.35D
                        && field.position.distanceToSqr(position) <= 0.04D) {
                    field.position = position;
                    field.radius = Math.max(field.radius, radius);
                    field.expiresAt = Math.max(field.expiresAt, expiresAt);
                    return;
                }
            }

            if (fields.size() >= MAX_FIELDS_PER_LEVEL) {
                fields.remove(0);
            }
            fields.add(new Field(position, radius, expiresAt));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 5L != 0L) {
            return;
        }

        List<Field> fields;
        synchronized (FIELDS) {
            fields = FIELDS.get(level);
            if (fields == null || fields.isEmpty()) return;

            long now = level.getGameTime();
            Iterator<Field> iterator = fields.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().expiresAt <= now) iterator.remove();
            }
            if (fields.isEmpty()) {
                FIELDS.remove(level);
                return;
            }
            fields = List.copyOf(fields);
        }

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative()
                    || player.isSpectator()) {
                continue;
            }

            AABB playerBox = player.getBoundingBox();
            for (Field field : fields) {
                if (playerBox.maxY < field.position.y - 0.10D
                        || playerBox.minY > field.position.y + 0.38D) {
                    continue;
                }

                double dx = player.getX() - field.position.x;
                double dz = player.getZ() - field.position.z;
                if (dx * dx + dz * dz > field.radius * field.radius) {
                    continue;
                }

                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        SLOWNESS_REFRESH_TICKS, 0,
                        false, false, true));
                break;
            }
        }
    }

    private static final class Field {
        private Vec3 position;
        private double radius;
        private long expiresAt;

        private Field(Vec3 position, double radius, long expiresAt) {
            this.position = position;
            this.radius = radius;
            this.expiresAt = expiresAt;
        }
    }
}
