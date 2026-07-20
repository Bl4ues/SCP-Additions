package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.event.ScpInventoryMaintenanceEvents;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.equipment.HazmatSuitEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MainUseActionPacket {

    private final int slot;

    public MainUseActionPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(MainUseActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
    }

    public static MainUseActionPacket decode(FriendlyByteBuf buf) {
        return new MainUseActionPacket(buf.readInt());
    }

    public static void handle(MainUseActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (!inventory.isValidMainSlot(msg.slot)) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                ItemStack stack = inventory.getInventoryItem(msg.slot);
                if (stack.isEmpty()) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                ScpItemType type = ScpItemClassifier.getType(stack);
                if (type == ScpItemType.USABLE || type == ScpItemType.PLACEABLE) {
                    ScpInventoryMaintenanceEvents.activateUsableSession(player, inventory, msg.slot);
                    return;
                }

                if (type == ScpItemType.CONSUMABLE) {
                    // The primary-use button has its own packet, separate from
                    // InventoryActionPacket. Enforce the sealed mask here before
                    // either direct consumption or a custom usable session can
                    // remove the authoritative SCP Inventory stack.
                    if (HazmatSuitAccess.isFullyEquipped(player)) {
                        HazmatSuitEvents.showSealedMaskMessage(player);
                        ModNetwork.syncTo(player, inventory);
                        return;
                    }

                    if (isVanillaConsumable(stack)) consume(player, inventory, msg.slot, stack);
                    else ScpInventoryMaintenanceEvents.activateUsableSession(player, inventory, msg.slot);
                    ModNetwork.syncTo(player, inventory);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isVanillaConsumable(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return stack.isEdible() || animation == UseAnim.EAT || animation == UseAnim.DRINK;
    }

    private static void consume(ServerPlayer player, IScpInventory inventory, int slot, ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        ItemStack usedStack = stack.copy();
        usedStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(usedStack);

        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                animation == UseAnim.DRINK ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS,
                0.8F,
                0.9F + player.getRandom().nextFloat() * 0.2F
        );

        ItemStack result = usedStack.finishUsingItem(player.level(), player);
        stack.shrink(1);
        inventory.setInventoryItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

        if (!result.isEmpty()) routeUseRemainder(player, inventory, result);
    }

    private static void routeUseRemainder(ServerPlayer player, IScpInventory inventory, ItemStack remainder) {
        ItemStack leftover = remainder.copy();
        ScpPickupRouter.stripNoMergeMarker(leftover);
        int accepted = ScpPickupRouter.accept(inventory, player, leftover);
        if (accepted > 0) leftover.shrink(accepted);
        if (!leftover.isEmpty()) player.drop(leftover, false);
    }
}
