package com.bl4ues.scpclassifieddirective.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;
import com.bl4ues.scpclassifieddirective.scp330.Scp330Hands;

/** Conservative checkpoint gate used only while MineZero compatibility is active. */
public final class MineZeroSaveSafety {
    private static final double CHASE_SCAN_RADIUS = 72.0D;

    private MineZeroSaveSafety() {
    }

    /** A MineZero save is global, so one endangered or dead player blocks it. */
    public static boolean canSave(MinecraftServer server) {
        if (server == null || MineZeroDeathCoordinator.sessionActive()) {
            return false;
        }
        ScpClassifiedDirectiveModVariables.MapVariables map =
                ScpClassifiedDirectiveModVariables.MapVariables.get(server.overworld());
        if (map.Scp914refining) return false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            if (!isPlayerSafe(player)) return false;
        }
        return true;
    }

    public static boolean isPlayerSafe(ServerPlayer player) {
        if (player == null || !player.isAlive()) return false;
        if (Scp330Hands.isDisabled(player)) return false;
        if (player.isOnFire() || player.isInLava() || player.isFreezing()) {
            return false;
        }
        if (player.fallDistance > 3.0F || player.isFallFlying()) return false;
        if (player.getAirSupply() < player.getMaxAirSupply() / 2) return false;
        if (player.getHealth() <= player.getMaxHealth() * 0.35F) return false;
        if (player.hurtTime > 0) return false;

        boolean harmfulEffect = player.getActiveEffects().stream()
                .anyMatch(effect -> effect.getEffect().getCategory()
                        == MobEffectCategory.HARMFUL);
        if (harmfulEffect) return false;

        AABB search = player.getBoundingBox().inflate(CHASE_SCAN_RADIUS);
        return player.serverLevel().getEntitiesOfClass(Mob.class, search,
                mob -> mob.isAlive() && mob.getTarget() == player).isEmpty();
    }
}
