package net.mcreator.scpadditions.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.HashSet;
import java.util.Set;

/**
 * Gives a newly wound SCP-914 a short window to detect a valid intake.
 * A valid input that is already present starts immediately; otherwise the
 * machine polls for up to two seconds so a solo player can enter the intake.
 */
public final class Scp914ActivationWaiter {
    private static final int INPUT_WAIT_TICKS = 40;
    private static final Set<PendingActivation> PENDING_ACTIVATIONS = new HashSet<>();

    private Scp914ActivationWaiter() {
    }

    public static void request(LevelAccessor world, double x, double y, double z, Entity user,
                               Scp914RecipeManager.Setting setting) {
        if (!(world instanceof ServerLevel level) || isRefining(level)) {
            return;
        }

        BlockPos keyPos = BlockPos.containing(x, y, z).immutable();
        PendingActivation pending = new PendingActivation(level.getServer(), level.dimension(), keyPos);
        if (!PENDING_ACTIVATIONS.add(pending)) {
            return;
        }

        tryActivate(level, keyPos, user, setting, pending, INPUT_WAIT_TICKS);
    }

    private static void tryActivate(ServerLevel level, BlockPos keyPos, Entity user,
                                    Scp914RecipeManager.Setting setting,
                                    PendingActivation pending, int ticksRemaining) {
        if (isRefining(level) || !level.hasChunkAt(keyPos)) {
            PENDING_ACTIVATIONS.remove(pending);
            return;
        }

        Scp914Processor.process(level, keyPos.getX(), keyPos.getY(), keyPos.getZ(), user, setting);
        if (isRefining(level)) {
            PENDING_ACTIVATIONS.remove(pending);
            return;
        }

        if (ticksRemaining <= 0) {
            PENDING_ACTIVATIONS.remove(pending);
            return;
        }

        ScpAdditionsMod.queueServerWork(1,
                () -> tryActivate(level, keyPos, user, setting, pending, ticksRemaining - 1));
    }

    private static boolean isRefining(LevelAccessor world) {
        return ScpAdditionsModVariables.MapVariables.get(world).Scp914refining;
    }

    private record PendingActivation(MinecraftServer server, ResourceKey<Level> dimension, BlockPos keyPos) {
    }
}
