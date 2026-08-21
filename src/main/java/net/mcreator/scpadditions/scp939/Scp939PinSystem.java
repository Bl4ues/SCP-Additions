package net.mcreator.scpadditions.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.network.Scp939Network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative pounce/pin struggle. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp939PinSystem {
    private static final int WINDOW_TICKS = 20;
    private static final int SUCCESS_REQUIRED = 3;
    private static final int FAILURE_LIMIT = 3;
    private static final Map<UUID, PinState> PINS = new HashMap<>();

    private Scp939PinSystem() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getSource().getEntity() instanceof Scp939Entity scp939)
                || scp939.getAction() != Scp939Entity.ACTION_POUNCE
                || player.isCreative() || player.isSpectator()
                || PINS.containsKey(player.getUUID())) {
            return;
        }
        event.setAmount(Math.min(event.getAmount(), 5.0F));
        begin(player, scp939);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        PinState pin = PINS.get(player.getUUID());
        if (pin == null) return;
        tickPin(player, pin);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) release(player,
                false);
        if (event.getEntity() instanceof Scp939Entity scp939) {
            UUID id = scp939.getUUID();
            for (UUID playerId : PINS.entrySet().stream()
                    .filter(entry -> entry.getValue().scp939Id.equals(id))
                    .map(Map.Entry::getKey).toList()) {
                ServerPlayer player = scp939.level() instanceof ServerLevel level
                        ? level.getServer().getPlayerList().getPlayer(playerId)
                        : null;
                if (player != null) release(player, false);
                else PINS.remove(playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) release(player,
                false);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PINS.clear();
    }

    public static void acceptInput(ServerPlayer player, int input) {
        PinState pin = player == null ? null : PINS.get(player.getUUID());
        if (pin == null || (input != 0 && input != 1)) return;
        if (input == pin.expectedKey) {
            pin.progress++;
            if (pin.progress >= SUCCESS_REQUIRED) {
                release(player, true);
                return;
            }
        } else {
            pin.failures++;
            if (pin.failures >= FAILURE_LIMIT) {
                fail(player, pin);
                return;
            }
        }
        nextPrompt(player, pin);
    }

    public static Snapshot snapshot(ServerPlayer player) {
        PinState pin = player == null ? null : PINS.get(player.getUUID());
        if (pin == null) return Snapshot.EMPTY;
        return new Snapshot(true, pin.progress, pin.failures,
                pin.expectedKey, pin.windowTicks);
    }

    public static boolean isPinning(Scp939Entity entity) {
        if (entity == null) return false;
        UUID id = entity.getUUID();
        return PINS.values().stream().anyMatch(pin -> pin.scp939Id.equals(id));
    }

    private static void begin(ServerPlayer player, Scp939Entity scp939) {
        PinState pin = new PinState(scp939.getUUID(),
                scp939.getRandom().nextBoolean() ? 1 : 0);
        PINS.put(player.getUUID(), pin);
        scp939.getNavigation().stop();
        scp939.setNoAi(true);
        scp939.setDeltaMovement(Vec3.ZERO);
        player.setForcedPose(Pose.SWIMMING);
        player.refreshDimensions();
        Scp939Network.sync(player);
    }

    private static void tickPin(ServerPlayer player, PinState pin) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            release(player, false);
            return;
        }
        Entity entity = player.serverLevel().getEntity(pin.scp939Id);
        if (!(entity instanceof Scp939Entity scp939) || !scp939.isAlive()) {
            release(player, false);
            return;
        }

        scp939.getNavigation().stop();
        scp939.setDeltaMovement(Vec3.ZERO);
        player.setForcedPose(Pose.SWIMMING);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        Vec3 forward = scp939.getLookAngle();
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < 0.0001D) horizontal = new Vec3(0, 0, 1);
        Vec3 pinPos = scp939.position().add(horizontal.normalize().scale(0.72D));
        if (scp939.tickCount % 2 == 0) {
            player.connection.teleport(pinPos.x, scp939.getY() + 0.08D,
                    pinPos.z, scp939.getYRot() + 180.0F, 8.0F);
        }

        if (scp939.tickCount % 12 == 0) {
            player.hurt(player.damageSources().mobAttack(scp939), 1.5F);
            if (!player.isAlive()) {
                release(player, false);
                return;
            }
        }

        if (--pin.windowTicks <= 0) {
            pin.failures++;
            if (pin.failures >= FAILURE_LIMIT) {
                fail(player, pin);
                return;
            }
            nextPrompt(player, pin);
        } else if (pin.windowTicks % 5 == 0) {
            Scp939Network.sync(player);
        }
    }

    private static void fail(ServerPlayer player, PinState pin) {
        Entity entity = player.serverLevel().getEntity(pin.scp939Id);
        if (entity instanceof Scp939Entity scp939 && player.isAlive()) {
            player.hurt(player.damageSources().mobAttack(scp939), 40.0F);
        }
        release(player, false);
    }

    private static void nextPrompt(ServerPlayer player, PinState pin) {
        pin.expectedKey = pin.expectedKey == 0 ? 1 : 0;
        if (player.getRandom().nextFloat() < 0.35F) {
            pin.expectedKey = player.getRandom().nextBoolean() ? 1 : 0;
        }
        pin.windowTicks = WINDOW_TICKS;
        Scp939Network.sync(player);
    }

    private static void release(ServerPlayer player, boolean kickedOff) {
        if (player == null) return;
        PinState pin = PINS.remove(player.getUUID());
        player.setForcedPose(null);
        player.refreshDimensions();
        if (pin != null) {
            Entity entity = player.serverLevel().getEntity(pin.scp939Id);
            if (entity instanceof Scp939Entity scp939) {
                scp939.setNoAi(false);
                if (kickedOff) {
                    Vec3 away = scp939.position().subtract(player.position());
                    if (away.horizontalDistanceSqr() > 0.0001D) {
                        Vec3 push = new Vec3(away.x, 0.0D, away.z)
                                .normalize().scale(0.65D);
                        scp939.setDeltaMovement(push.x, 0.20D, push.z);
                        scp939.hasImpulse = true;
                    }
                }
            }
        }
        Scp939Network.sync(player);
    }

    private static final class PinState {
        private final UUID scp939Id;
        private int progress;
        private int failures;
        private int expectedKey;
        private int windowTicks = WINDOW_TICKS;

        private PinState(UUID scp939Id, int expectedKey) {
            this.scp939Id = scp939Id;
            this.expectedKey = expectedKey;
        }
    }

    public record Snapshot(boolean pinned, int progress, int failures,
            int expectedKey, int windowTicks) {
        public static final Snapshot EMPTY = new Snapshot(false, 0, 0, 0, 0);
    }
}
