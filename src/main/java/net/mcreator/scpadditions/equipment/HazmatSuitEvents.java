package net.mcreator.scpadditions.equipment;

import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class HazmatSuitEvents {
    private HazmatSuitEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && !event.player.level().isClientSide
                && event.player instanceof ServerPlayer player) {
            HazmatSuitManager.serverTick(player);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !HazmatSuitAccess.isInternalPiece(
                        event.getEntity().getItem())
                || (!HazmatSuitManager.isKnownEquipped(player)
                        && !HazmatSuitAccess.isFullyEquipped(player))) {
            return;
        }

        event.setCanceled(true);
        HazmatSuitManager.requestUnequip(player);
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!HazmatSuitAccess.isFullyEquipped(event.getEntity())) {
            return;
        }
        if (event.getEntity().isCrouching() || isFoodOrDrink(event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(
            PlayerInteractEvent.RightClickItem event) {
        if (!HazmatSuitAccess.isFullyEquipped(event.getEntity())) {
            return;
        }

        if (event.getEntity().isCrouching()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (!isFoodOrDrink(event.getItemStack())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof ServerPlayer player) {
            showSealedMaskMessage(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event) {
        if (!HazmatSuitAccess.isFullyEquipped(event.getEntity())) {
            return;
        }

        if (event.getEntity().isCrouching()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (!(event.getLevel().getBlockState(event.getPos()).getBlock()
                instanceof CakeBlock)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof ServerPlayer player) {
            showSealedMaskMessage(player);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event) {
        if (HazmatSuitAccess.isFullyEquipped(event.getEntity())
                && event.getEntity().isCrouching()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AtomicBoolean removedInternalPiece = new AtomicBoolean(false);
        event.getDrops().removeIf(drop -> {
            boolean internal = HazmatSuitAccess.isInternalPiece(drop.getItem());
            if (internal) {
                removedInternalPiece.set(true);
            }
            return internal;
        });

        if (removedInternalPiece.get()
                && HazmatSuitManager.shouldReturnPublicItem(player)) {
            event.getDrops().add(new ItemEntity(
                    player.level(), player.getX(), player.getY(), player.getZ(),
                    new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT.get())));
        }
        HazmatSuitManager.clearTransientState(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HazmatSuitManager.clearTransientState(event.getEntity());
    }

    /**
     * Treats the SCP Inventory's authoritative CONSUMABLE classification as an
     * ingestion attempt even when a custom item does not expose a vanilla eat or
     * drink animation. Thrown potions remain USABLE and are not caught here.
     */
    public static boolean isFoodOrDrink(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (ScpItemClassifier.getType(stack) == ScpItemType.CONSUMABLE) {
            return true;
        }
        UseAnim animation = stack.getUseAnimation();
        return stack.isEdible()
                || animation == UseAnim.EAT
                || animation == UseAnim.DRINK;
    }

    public static void showSealedMaskMessage(ServerPlayer player) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "message.scp_additions.hazmat.sealed_consumption"), true);
        }
    }
}
