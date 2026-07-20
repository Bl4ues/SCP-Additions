package net.mcreator.scpadditions.scp012;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.effect.Scp714ProtectionAccess;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative attraction and contact sequence for SCP-012. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME)
public final class Scp012InfluenceEvents {
    public static final int INFLUENCE_RADIUS = 10;
    private static final int TARGET_SEARCH_INTERVAL_TICKS = 10;
    private static final int TARGET_DISCOVERY_RADIUS = INFLUENCE_RADIUS + 3;
    private static final double DAMAGE_RADIUS = 1.25D;
    private static final int FATAL_CONTACT_TICKS = 15 * 20;
    private static final String BLEEDING_TAG = "ScpAdditionsScp012Bleeding";

    private static final Map<UUID, ContactState> CONTACT_STATES = new HashMap<>();
    private static final Map<UUID, BlockPos> ACTIVE_TARGETS = new HashMap<>();
    private static final Map<UUID, DiscoveredTarget> DISCOVERED_TARGETS =
            new HashMap<>();
    private static final Map<UUID, Long> NEXT_TARGET_SEARCH = new HashMap<>();

    private Scp012InfluenceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            clearAll(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos nearby = findNearby(level, player);
        if (nearby == null) {
            clearInfluence(player);
            return;
        }

        boolean systemControl = level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON);
        Vec3 attraction = Scp012Module.attractionPoint(level, nearby);
        double distance = player.position().distanceTo(attraction);

        // SCP-079 springs the trap as soon as the player enters SCP-012's
        // influence radius. A controlled heavy door on the route is commanded
        // first, then the containment box begins opening in the same tick.
        // These hostile facility actions remain active through SCP-714.
        if (systemControl) {
            Scp012Stage stage = Scp012Module.stageOf(level.getBlockState(nearby));
            boolean boxClosed = stage == Scp012Stage.CLOSED;
            boolean periodicDoorCheck =
                    (level.getGameTime() + player.getId()) % 10L == 0L;
            if (boxClosed || periodicDoorCheck) {
                Scp012DoorAccess.tryOpen(level, player, nearby);
            }
            if (boxClosed) {
                Scp012Module.open(level, nearby);
            }
        }

        // SCP-714 cancels only SCP-012's anomalous influence. It deliberately
        // does not close the box, undo SCP-079's door actions, or cure bleeding.
        if (Scp714ProtectionAccess.isProtected(player)) {
            clearInfluence(player);
            return;
        }
        if (!Scp012Module.isOpen(level.getBlockState(nearby))) {
            clearInfluence(player);
            return;
        }

        ACTIVE_TARGETS.put(player.getUUID(), nearby);
        float influence = influenceStrength(distance);
        pullToward(level, player, attraction, influence);

        boolean damageActive = distance <= DAMAGE_RADIUS
                || player.getBoundingBox().inflate(0.10D)
                .intersects(new AABB(nearby));
        float contactProgress;
        if (damageActive) {
            contactProgress = handleContact(level, player);
        } else {
            CONTACT_STATES.remove(player.getUUID());
            contactProgress = 0.0F;
        }
        sync(player, nearby, contactProgress, damageActive);
    }

    private static BlockPos findNearby(ServerLevel level, ServerPlayer player) {
        UUID id = player.getUUID();
        DiscoveredTarget cached = DISCOVERED_TARGETS.get(id);
        if (cached != null && cached.dimension().equals(level.dimension())) {
            BlockPos pos = cached.pos();
            double distanceSqr = Vec3.atCenterOf(pos)
                    .distanceToSqr(player.position());
            if (Scp012Module.isScp012(level.getBlockState(pos))
                    && distanceSqr <= TARGET_DISCOVERY_RADIUS
                    * TARGET_DISCOVERY_RADIUS) {
                return distanceSqr <= INFLUENCE_RADIUS * INFLUENCE_RADIUS
                        ? pos : null;
            }
        }
        DISCOVERED_TARGETS.remove(id);

        long gameTime = level.getGameTime();
        if (NEXT_TARGET_SEARCH.getOrDefault(id, 0L) > gameTime) {
            return null;
        }
        NEXT_TARGET_SEARCH.put(id, gameTime + TARGET_SEARCH_INTERVAL_TICKS);

        // Discover the box slightly before the player reaches its actual effect
        // radius. The cached target can then trigger on the exact crossing tick
        // without restoring the old full-cube scan on every player tick.
        BlockPos found = Scp012Module.findNearest(level, player.position(),
                TARGET_DISCOVERY_RADIUS, false);
        if (found == null) return null;

        DISCOVERED_TARGETS.put(id,
                new DiscoveredTarget(level.dimension(), found.immutable()));
        return Vec3.atCenterOf(found).distanceToSqr(player.position())
                <= INFLUENCE_RADIUS * INFLUENCE_RADIUS ? found : null;
    }

    private static float influenceStrength(double distance) {
        float proximity = Mth.clamp(
                1.0F - (float) (distance / INFLUENCE_RADIUS), 0.0F, 1.0F);
        return (float) Math.pow(proximity, 1.35D);
    }

    private static void pullToward(ServerLevel level, ServerPlayer player,
                                   Vec3 destination, float influence) {
        Vec3 waypoint = Scp012Pathfinder.nextWaypoint(level, player, destination);
        Vec3 toward = new Vec3(waypoint.x - player.getX(), 0.0D,
                waypoint.z - player.getZ());
        if (toward.lengthSqr() < 0.0001D) return;

        Vec3 direction = toward.normalize();
        Vec3 current = player.getDeltaMovement();
        double retention = Mth.lerp(influence, 0.96D, 0.58D);
        double pullSpeed = Mth.lerp(influence, 0.006D, 0.055D);
        Vec3 horizontal = new Vec3(current.x * retention + direction.x * pullSpeed,
                0.0D, current.z * retention + direction.z * pullSpeed);

        double maximum = Mth.lerp(influence, 0.060D, 0.085D);
        if (horizontal.lengthSqr() > maximum * maximum) {
            horizontal = horizontal.normalize().scale(maximum);
        }

        double vertical = current.y;
        if (waypoint.y > player.getY() + 0.35D && player.onGround()) {
            vertical = 0.28D;
        }
        player.setSprinting(false);
        player.setDeltaMovement(horizontal.x, vertical, horizontal.z);
        player.hurtMarked = true;
    }

    private static float handleContact(ServerLevel level, ServerPlayer player) {
        ContactState state = CONTACT_STATES.computeIfAbsent(player.getUUID(),
                ignored -> new ContactState(20 + level.getRandom().nextInt(21)));
        state.ticks++;

        if (state.ticks >= FATAL_CONTACT_TICKS) {
            player.hurt(Scp012Damage.source(level), 10000.0F);
            return 1.0F;
        }

        if (state.ticks >= state.nextDamageTick) {
            float before = player.getHealth();
            float damage = 1.5F + level.getRandom().nextFloat() * 1.5F;
            player.hurt(Scp012Damage.source(level), damage);
            float lost = Math.max(0.0F, before - player.getHealth());
            state.damageTaken += lost;
            state.nextDamageTick = state.ticks + 35
                    + level.getRandom().nextInt(31);

            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            if (!state.narrationShown
                    && state.damageTaken >= maxHealth * 0.25F) {
                state.narrationShown = true;
                player.displayClientMessage(Component.literal(
                        "You tear open your left wrist and start writing "
                                + "on the composition with your blood."), true);
            }
            if (state.damageTaken >= maxHealth * 0.40F) {
                applyBleeding(player);
            }
        }

        return Mth.clamp(state.ticks / (float) FATAL_CONTACT_TICKS,
                0.0F, 1.0F);
    }

    private static void applyBleeding(ServerPlayer player) {
        player.getPersistentData().putBoolean(BLEEDING_TAG, true);
        if (!player.hasEffect(ScpAdditionsModMobEffects.BLEEDING.get())) {
            player.addEffect(new MobEffectInstance(
                    ScpAdditionsModMobEffects.BLEEDING.get(),
                    Integer.MAX_VALUE, 0, false, false, true));
        }
    }

    private static void sync(ServerPlayer player, BlockPos target,
                             float contactProgress, boolean damageActive) {
        if ((player.tickCount + player.getId()) % 3 == 0) {
            ScpEntityNetwork.syncScp012Influence(player, true, target,
                    contactProgress, damageActive);
        }
    }

    private static void clearInfluence(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean wasActive = ACTIVE_TARGETS.remove(id) != null;
        CONTACT_STATES.remove(id);
        Scp012Pathfinder.clear(player);
        if (wasActive) {
            ScpEntityNetwork.syncScp012Influence(player, false,
                    BlockPos.ZERO, 0.0F, false);
        }
    }

    private static void clearAll(ServerPlayer player) {
        UUID id = player.getUUID();
        clearInfluence(player);
        DISCOVERED_TARGETS.remove(id);
        NEXT_TARGET_SEARCH.remove(id);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clearAll(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clearAll(player);
    }

    private record DiscoveredTarget(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final class ContactState {
        private int ticks;
        private int nextDamageTick;
        private float damageTaken;
        private boolean narrationShown;

        private ContactState(int nextDamageTick) {
            this.nextDamageTick = nextDamageTick;
        }
    }
}
