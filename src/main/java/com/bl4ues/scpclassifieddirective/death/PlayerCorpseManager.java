package com.bl4ues.scpclassifieddirective.death;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.MineZeroDeathCoordinator;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemType;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
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
            // than ActiveUsable. Move that canonical stack back into SCP storage
            // before corpse capture. This path deliberately works for MineZero's
            // already-spectating logical dead player too.
            stowActivePlaceableForCapture(player);
            corpse.captureInventoryFrom(player);
        }
    }

    private static void stowActivePlaceableForCapture(ServerPlayer player) {
        if (player == null || !ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) {
            return;
        }

        IScpInventory scp = player.getCapability(ScpInventoryCapability.INSTANCE)
                .resolve().orElse(null);
        if (scp == null) return;

        int end = Math.min(VANILLA_HOTBAR_SIZE,
                player.getInventory().items.size());
        for (int slot = 0; slot < end; slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.isEmpty()
                    || !ScpPickupRouter.isUsableSession(stack)
                    || ScpItemClassifier.getType(stack) != ScpItemType.PLACEABLE) {
                continue;
            }

            ItemStack restored = stack.copy();
            restored.setCount(1);
            ScpPickupRouter.stripUsableSession(restored);
            ScpPickupRouter.stripNoMergeMarker(restored);
            ScpPickupRouter.stripCoinMirror(restored);
            ScpPickupRouter.stripHarmfulMirror(restored);

            player.getInventory().items.set(slot, ItemStack.EMPTY);
            player.getInventory().setChanged();
            if (!scp.addInventoryItem(restored)) {
                // The active placeable normally guarantees one vacant SCP slot
                // because it was extracted from that inventory. If another system
                // filled it in the meantime, dropping is still safer than deletion.
                player.drop(restored, false);
            }
            ModNetwork.syncTo(player, scp);
            return;
        }
    }
}
