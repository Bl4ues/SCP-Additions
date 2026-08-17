package net.mcreator.scpadditions.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;

import java.lang.reflect.Field;

/** Makes an eligible player who attacks SCP-106 a valid immediate hunt target. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106TargetingEvents {
    private static final Field HUNTED_PLAYER = huntedPlayerField();

    private Scp106TargetingEvents() {
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Scp106Entity scp106)) return;
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player)
                || !player.isAlive() || player.isRemoved()
                || player.isCreative() || player.isSpectator()
                || player.level() != scp106.level()) {
            return;
        }

        scp106.setTarget(player);
        if (HUNTED_PLAYER == null) return;
        try {
            HUNTED_PLAYER.set(scp106, player.getUUID());
        } catch (IllegalAccessException exception) {
            ScpAdditionsMod.LOGGER.warn(
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
}
