package net.mcreator.scpadditions.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.List;

/**
 * Restores the standalone Eye Pod interaction contract before Entity#interact
 * reaches the transitional actionbar implementation in the migrated entity.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp131InteractionEvents {
    private static final double GROUP_RANGE = 24.0D;
    private static final double GROUP_RANGE_SQR = GROUP_RANGE * GROUP_RANGE;

    private Scp131InteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractScp131Entity trigger)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(trigger.level().isClientSide));
        if (trigger.level().isClientSide) {
            return;
        }

        Player player = event.getEntity();
        AABB area = trigger.getBoundingBox().inflate(GROUP_RANGE);
        List<AbstractScp131Entity> group = trigger.level().getEntitiesOfClass(AbstractScp131Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(trigger) <= GROUP_RANGE_SQR);
        if (!group.contains(trigger)) {
            group.add(trigger);
        }

        // Clicking starts the group. Stopping is intentionally exclusive to hold-G.
        if (group.stream().anyMatch(entity -> entity.isFollowingPlayer(player))) {
            return;
        }

        for (AbstractScp131Entity entity : group) {
            entity.startFollowing(player);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ScpEntityNetwork.showScp131Notice(serverPlayer, true);
        }
        trigger.level().playSound(null, trigger.getX(), trigger.getY() + 0.35D, trigger.getZ(),
                Scp131Sounds.EYE_POD_VOICE.get(), SoundSource.NEUTRAL, 1.0F,
                0.86F + (trigger.getRandom().nextFloat() * 0.30F));
    }
}
