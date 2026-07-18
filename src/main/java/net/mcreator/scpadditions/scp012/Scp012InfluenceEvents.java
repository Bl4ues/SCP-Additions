package net.mcreator.scpadditions.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.effect.Scp714ProtectionAccess;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative attraction and contact sequence for SCP-012. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp012InfluenceEvents {
    public static final int INFLUENCE_RADIUS = 10;
    private static final double PULL_SPEED = 0.035D;
    private static final int DAMAGE_INTERVAL_TICKS = 40;
    private static final int FULL_OVERLAY_TICKS = 100;
    private static final Map<UUID, Integer> CONTACT_TICKS = new HashMap<>();
    private static final Map<UUID, BlockPos> ACTIVE_TARGETS = new HashMap<>();

    private Scp012InfluenceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            clear(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        if (Scp714ProtectionAccess.isProtected(player)) {
            clear(player);
            return;
        }

        BlockPos nearby = Scp012Module.findNearest(level, player.position(),
                INFLUENCE_RADIUS, false);
        if (nearby == null) {
            clear(player);
            return;
        }

        boolean systemControl = level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON);
        if (!Scp012Module.isOpen(level.getBlockState(nearby))) {
            if (systemControl) Scp012Module.open(level, nearby);
            clear(player);
            return;
        }

        ACTIVE_TARGETS.put(player.getUUID(), nearby);
        if (systemControl && (level.getGameTime() + player.getId()) % 10L == 0L) {
            Scp012DoorAccess.tryOpen(level, player, nearby);
        }

        Vec3 attraction = Scp012Module.attractionPoint(level, nearby);
        boolean touching = player.getBoundingBox().inflate(0.15D)
                .intersects(new AABB(nearby));
        if (touching) {
            handleContact(level, player, nearby);
        } else {
            CONTACT_TICKS.remove(player.getUUID());
            pullToward(level, player, attraction);
            sync(player, nearby, 0.0F);
        }
    }

    private static void pullToward(ServerLevel level, ServerPlayer player,
                                   Vec3 destination) {
        Vec3 waypoint = Scp012Pathfinder.nextWaypoint(level, player, destination);
        Vec3 horizontal = new Vec3(waypoint.x - player.getX(), 0.0D,
                waypoint.z - player.getZ());
        if (horizontal.lengthSqr() < 0.0001D) return;
        Vec3 direction = horizontal.normalize();
        double vertical = player.getDeltaMovement().y;
        if (waypoint.y > player.getY() + 0.35D && player.onGround()) {
            vertical = 0.28D;
        }
        player.setSprinting(false);
        player.setDeltaMovement(direction.x * PULL_SPEED, vertical,
                direction.z * PULL_SPEED);
        player.hurtMarked = true;
    }

    private static void handleContact(ServerLevel level, ServerPlayer player,
                                      BlockPos target) {
        player.setSprinting(false);
        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        player.hurtMarked = true;
        Scp012Pathfinder.clear(player);

        int ticks = CONTACT_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (ticks == 20) {
            player.displayClientMessage(Component.translatable(
                    "message.scp_additions.scp_012_contact"), true);
        }
        if (ticks >= DAMAGE_INTERVAL_TICKS
                && ticks % DAMAGE_INTERVAL_TICKS == 0) {
            float damage = 3.0F + level.getRandom().nextFloat() * 2.0F;
            player.hurt(Scp012Damage.source(level), damage);
        }
        float contactProgress = Mth.clamp(ticks / (float) FULL_OVERLAY_TICKS,
                0.0F, 1.0F);
        sync(player, target, contactProgress);
    }

    private static void sync(ServerPlayer player, BlockPos target,
                             float contactProgress) {
        if ((player.tickCount + player.getId()) % 3 == 0) {
            ScpEntityNetwork.syncScp012Influence(player, true, target,
                    contactProgress);
        }
    }

    private static void clear(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean wasActive = ACTIVE_TARGETS.remove(id) != null;
        CONTACT_TICKS.remove(id);
        Scp012Pathfinder.clear(player);
        if (wasActive) ScpEntityNetwork.syncScp012Influence(player, false,
                BlockPos.ZERO, 0.0F);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }
}
