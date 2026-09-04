package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Server-side role selection bridge for the currently playable SCP-079 role. */
public final class Scp079RoleSelection {
    private Scp079RoleSelection() {
    }

    public static boolean canOpenSelector(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        if (Scp079PlayableManager.isController(player)) return true;
        if (player.isCreative() && player.canUseGameMasterBlocks()) return true;
        player.displayClientMessage(Component.literal(
                "The SCP Role Selector is available to Creative operators only."),
                true);
        return false;
    }

    public static boolean selectScp079(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        if (Scp079PlayableManager.isController(player)) return true;
        if (!player.isCreative() || !player.canUseGameMasterBlocks()) {
            player.displayClientMessage(Component.literal(
                    "The SCP Role Selector is available to Creative operators only."),
                    true);
            return false;
        }

        BlockPos host = nearestHost(player);
        if (host == null) {
            player.displayClientMessage(Component.literal(
                    "No SCP-079 computer is registered in this dimension."), true);
            return false;
        }

        boolean assumed = Scp079PlayableManager.assume(player, host);
        if (assumed) {
            player.displayClientMessage(Component.literal(
                    "Playable SCP-079 control acquired."), true);
        }
        return assumed;
    }

    public static boolean release(ServerPlayer player) {
        if (player == null || !Scp079PlayableManager.isController(player)) {
            return false;
        }
        Scp079PlayableManager.release(player);
        player.displayClientMessage(Component.literal(
                "Released playable SCP-079 control."), true);
        return true;
    }

    /** Temporary compatibility path for the physical-host debug gesture. */
    public static boolean toggle(ServerPlayer player) {
        return Scp079PlayableManager.isController(player)
                ? release(player) : selectScp079(player);
    }

    private static BlockPos nearestHost(ServerPlayer player) {
        String dimension = player.level().dimension().location().toString();
        Vec3 origin = player.position();
        return Scp079FacilityAccessSavedData.get(player.getServer()).hosts().stream()
                .filter(host -> dimension.equals(host.dimension()))
                .map(host -> BlockPos.of(host.packedPos()))
                .filter(pos -> isHost(player.serverLevel().getBlockState(pos)))
                .min(Comparator.comparingDouble(pos ->
                        Vec3.atCenterOf(pos).distanceToSqr(origin)))
                .map(BlockPos::immutable)
                .orElse(null);
    }

    private static boolean isHost(BlockState state) {
        return state != null
                && (state.is(ScpClassifiedDirectiveModBlocks.SCP_079ON.get())
                || state.is(ScpClassifiedDirectiveModBlocks.SCP_079OFF.get()));
    }
}
