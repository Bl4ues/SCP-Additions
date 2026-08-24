package com.bl4ues.scpclassifieddirective.death;

import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.MineZeroDeathCoordinator;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.inventory.event.PlaceableHotbarSessionEvents;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Creates server-side player bodies for normal and MineZero logical deaths. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerCorpseManager {
    private static final int VANILLA_HOTBAR_SIZE = 9;

    private PlayerCorpseManager() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean logicalDeath = MineZeroDeathCoordinator.isLogicallyDead(player);
        // MineZero intentionally cancels LivingDeathEvent after converting the
        // player to its logical-death state. Other canceled deaths did not happen.
        if (event.isCanceled() && !logicalDeath) return;
        if (!ScpClassifiedDirectiveModulesConfig.get().deathBodies.enabled) return;

        PlayerCorpseEntity corpse = ScpClassifiedDirectiveModEntities.PLAYER_CORPSE.get()
                .create(player.serverLevel());
        if (corpse == null) return;
        corpse.initializeFrom(player, logicalDeath);

        // Do not remove items from the player until the corpse actually exists
        // in the level. A failed entity spawn must never become an item-deletion
        // mechanism.
        if (player.serverLevel().addFreshEntity(corpse)) {
            // PLACEABLE sessions own their real stack in the vanilla hotbar rather
            // than ActiveUsable. Return it to the canonical SCP Inventory before
            // corpse capture, otherwise the mirror-cleanup clearContent() would
            // discard the active block instead of putting it in the body.
            stowActivePlaceable(player);
            corpse.captureInventoryFrom(player);
        }
    }

    private static void stowActivePlaceable(ServerPlayer player) {
        if (player == null || !ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) {
            return;
        }
        int end = Math.min(VANILLA_HOTBAR_SIZE,
                player.getInventory().items.size());
        for (int slot = 0; slot < end; slot++) {
            if (!PlaceableHotbarSessionEvents.isTrackedPlaceableSlot(player, slot)) {
                continue;
            }
            PlaceableHotbarSessionEvents.returnTrackedPlaceableSession(player, slot);
            return;
        }
    }
}
