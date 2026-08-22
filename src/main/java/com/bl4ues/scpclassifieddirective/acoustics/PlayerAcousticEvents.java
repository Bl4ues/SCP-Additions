package com.bl4ues.scpclassifieddirective.acoustics;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.scp939.Scp939BreathSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Vanilla gameplay producers for the shared acoustic system.
 *
 * These events deliberately describe evidence, not aggro. A crouching player
 * still makes a tiny close-range footstep stimulus, while sprinting, jumping,
 * landing and physical interactions are progressively easier to hear.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerAcousticEvents {
    private static final Map<MinecraftServer, Map<UUID, MotionState>> STATES =
            new WeakHashMap<>();

    private PlayerAcousticEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (!player.isAlive() || player.isSpectator()) {
            removeState(server, player.getUUID());
            return;
        }

        long now = player.serverLevel().getGameTime();
        MotionState state = state(server, player.getUUID());
        Vec3 current = player.position();
        if (state.lastPosition == null) {
            state.lastPosition = current;
            state.wasOnGround = player.onGround();
            state.nextBreathTick = now + 45L + player.getRandom().nextInt(25);
            return;
        }

        double dx = current.x - state.lastPosition.x;
        double dz = current.z - state.lastPosition.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (state.wasOnGround && !player.onGround()
                && player.getDeltaMovement().y > 0.10D) {
            AcousticStimulusSystem.emit(player.serverLevel(), current,
                    AcousticCategory.JUMP, 0.70F, player);
        }

        if (!state.wasOnGround && player.onGround() && state.airTicks >= 3) {
            float intensity = Mth.clamp(0.55F + state.airTicks * 0.03F,
                    0.60F, 1.40F);
            AcousticStimulusSystem.emit(player.serverLevel(), current,
                    AcousticCategory.LAND, intensity, player);
        }

        if (player.onGround() && horizontalDistance > 0.005D
                && horizontalDistance < 2.0D) {
            state.stepDistance += horizontalDistance;
            // A sprinting player is a continuous stream of loud footfalls to a
            // predator whose primary sense is sound. A 0.90-block evidence gap
            // left the 939 steering toward old nodes between footsteps and made
            // a straight sprint look strangely difficult to follow.
            double stride = player.isSprinting() ? 0.58D
                    : player.isCrouching() ? 0.75D : 0.70D;
            if (state.stepDistance >= stride) {
                state.stepDistance %= stride;
                AcousticCategory category = player.isSprinting()
                        ? AcousticCategory.SPRINT : AcousticCategory.FOOTSTEP;
                float intensity = player.isSprinting() ? 1.05F
                        : player.isCrouching() ? 0.12F : 0.55F;
                AcousticStimulusSystem.emit(player.serverLevel(), current,
                        category, intensity, player);
            }
        } else if (horizontalDistance >= 2.0D) {
            // Teleports, respawns and dimension synchronization are not footsteps.
            state.stepDistance = 0.0D;
        }

        if (!player.onGround()) {
            state.airTicks = Math.min(200, state.airTicks + 1);
        } else {
            state.airTicks = 0;
        }

        if (now >= state.nextBreathTick && !player.isInWaterOrBubble()) {
            // When SCP-939 is close, the dedicated reserve system owns breathing
            // completely. This is what makes Hold Breath actually silent instead
            // of leaving this ambient producer to betray the player anyway.
            if (!Scp939BreathSystem.isActive(player)) {
                AcousticStimulusSystem.emit(player.serverLevel(), current.add(0.0D,
                        player.getBbHeight() * 0.75D, 0.0D),
                        AcousticCategory.BREATH, 1.00F, player);
            }
            state.nextBreathTick = now + 45L + player.getRandom().nextInt(30);
        }

        state.lastPosition = current;
        state.wasOnGround = player.onGround();
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockState blockState = level.getBlockState(event.getPos());
        AcousticCategory category;
        float intensity;
        if (blockState.getBlock() instanceof DoorBlock
                || blockState.getBlock() instanceof TrapDoorBlock) {
            category = AcousticCategory.DOOR;
            intensity = 0.95F;
        } else if (blockState.getBlock() instanceof ButtonBlock
                || blockState.getBlock() instanceof LeverBlock) {
            category = AcousticCategory.BUTTON;
            intensity = 0.70F;
        } else {
            category = AcousticCategory.INTERACTION;
            intensity = 0.35F;
        }
        AcousticStimulusSystem.emit(level, Vec3.atCenterOf(event.getPos()),
                category, intensity, player);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AcousticStimulusSystem.emit(level, Vec3.atCenterOf(event.getPos()),
                AcousticCategory.BLOCK, 0.35F, player);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        AcousticStimulusSystem.emit(level, Vec3.atCenterOf(event.getPos()),
                AcousticCategory.BLOCK, 1.10F, event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        UUID playerId = event.getEntity().getUUID();
        removeState(server, playerId);
        AcousticStimulusSystem.forgetPlayer(server, playerId);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server != null) removeState(server, event.getEntity().getUUID());
    }

    private static MotionState state(MinecraftServer server, UUID playerId) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(server, ignored -> new HashMap<>())
                    .computeIfAbsent(playerId, ignored -> new MotionState());
        }
    }

    private static void removeState(MinecraftServer server, UUID playerId) {
        synchronized (STATES) {
            Map<UUID, MotionState> states = STATES.get(server);
            if (states != null) states.remove(playerId);
        }
    }

    private static final class MotionState {
        private Vec3 lastPosition;
        private boolean wasOnGround;
        private int airTicks;
        private double stepDistance;
        private long nextBreathTick;
    }
}
