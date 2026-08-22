package com.bl4ues.scpclassifieddirective.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Makes an eligible player who attacks SCP-106 an immediate retaliation target. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106TargetingEvents {
    private static final int RETALIATION_TICKS = 3 * 20;
    private static final Field HUNTED_PLAYER = huntedPlayerField();
    private static final Map<Scp106Entity, Retaliation> RETALIATION =
            new WeakHashMap<>();

    private Scp106TargetingEvents() {
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Scp106Entity scp106)
                || scp106.level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player)
                || !isEligible(scp106, player)) {
            return;
        }

        long expiresAt = scp106.level().getGameTime() + RETALIATION_TICKS;
        RETALIATION.put(scp106,
                new Retaliation(player.getUUID(), expiresAt,
                        new WeakReference<>(player)));
        scp106.setTarget(player);
        setHuntedPlayer(scp106, player);
    }

    public static Player preferredTarget(Scp106Entity scp106) {
        if (scp106 == null || scp106.level().isClientSide
                || !(scp106.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Retaliation retaliation = RETALIATION.get(scp106);
        if (retaliation == null) return null;
        if (serverLevel.getGameTime() > retaliation.expiresAt) {
            RETALIATION.remove(scp106);
            return null;
        }

        Player player = retaliation.player.get();
        if (player == null || !retaliation.playerId.equals(player.getUUID())) {
            player = serverLevel.getPlayerByUUID(retaliation.playerId);
        }
        if (!isEligible(scp106, player)) {
            RETALIATION.remove(scp106);
            return null;
        }
        return player;
    }

    private static boolean isEligible(Scp106Entity scp106, Player player) {
        return player != null && player.isAlive() && !player.isRemoved()
                && !player.isCreative() && !player.isSpectator()
                && player.level() == scp106.level();
    }

    private static void setHuntedPlayer(Scp106Entity scp106, Player player) {
        if (HUNTED_PLAYER == null) return;
        try {
            HUNTED_PLAYER.set(scp106, player.getUUID());
        } catch (IllegalAccessException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not retarget SCP-106 to its attacker", exception);
        }
    }

    private static Field huntedPlayerField() {
        try {
            Field field = Scp106Entity.class.getDeclaredField("huntedPlayerId");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record Retaliation(UUID playerId, long expiresAt,
                               WeakReference<Player> player) {
    }
}
