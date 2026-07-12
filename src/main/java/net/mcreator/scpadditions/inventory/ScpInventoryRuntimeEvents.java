package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative pickup routing and capability synchronization. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpInventoryRuntimeEvents {
    private static final long FULL_NOTICE_COOLDOWN_TICKS = 70L;
    private static final Map<UUID, Long> LAST_FULL_NOTICE =
            new ConcurrentHashMap<>();

    private ScpInventoryRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.isCreative() || player.isSpectator()) {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        ItemStack groundStack = itemEntity.getItem();
        if (groundStack.isEmpty() || ScpPickupRouter.isUsableSession(groundStack)
                || ScpPickupRouter.isInternalMirror(groundStack)) {
            return;
        }

        // While the custom inventory is active, a failed custom pickup must not
        // silently fall through into vanilla inventory. Leave the remainder on
        // the ground when the relevant SCP storage section is full.
        event.setCanceled(true);

        int requested = groundStack.getCount();
        final int[] accepted = {0};
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
                accepted[0] = ScpPickupRouter.accept(inventory, player, groundStack));
        if (accepted[0] <= 0) {
            notifyFullIfReady(player);
            return;
        }

        ItemStack statStack = groundStack.copy();
        groundStack.shrink(accepted[0]);
        player.take(itemEntity, accepted[0]);
        player.awardStat(Stats.ITEM_PICKED_UP.get(statStack.getItem()), accepted[0]);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat())
                        * 0.7F + 1.0F) * 2.0F);

        if (groundStack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(groundStack);
        }

        ScpInventoryNetwork.sync(player);
        if (accepted[0] < requested) {
            notifyFullIfReady(player);
        }
    }

    private static void notifyFullIfReady(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = LAST_FULL_NOTICE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
        if (now - last < FULL_NOTICE_COOLDOWN_TICKS) {
            return;
        }
        LAST_FULL_NOTICE.put(player.getUUID(), now);
        ScpInventoryNetwork.notifyFull(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScpAdditionsMod.queueServerWork(1, () -> ScpInventoryNetwork.sync(player));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScpAdditionsMod.queueServerWork(1, () -> ScpInventoryNetwork.sync(player));
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScpAdditionsMod.queueServerWork(1, () -> ScpInventoryNetwork.sync(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_FULL_NOTICE.remove(event.getEntity().getUUID());
    }
}
