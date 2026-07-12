package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/** Maintains one disposable vanilla mirror for each equipped capability item. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpEquipmentMirrorEvents {
    private ScpEquipmentMirrorEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || !ScpAdditionsModulesConfig.get().inventory.enabled) return;

        player.getCapability(ScpInventoryCapability.INSTANCE)
                .ifPresent(inventory -> {
                    for (ScpEquipmentSlot slot : activeSlots())
                        reconcile(player, inventory, slot);
                });
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!ScpPickupRouter.isInternalMirror(stack)) return;
        event.getEntity().discard();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof ItemEntity item
                && ScpPickupRouter.isInternalMirror(item.getItem())) {
            event.setCanceled(true);
        }
    }

    private static void reconcile(ServerPlayer player, IScpInventory inventory,
            ScpEquipmentSlot slot) {
        ItemStack source = inventory.getEquipment(slot);
        ItemStack expected = expectedMirror(player, slot);

        if (source.isEmpty()) {
            removeMirrors(player, slot);
            return;
        }

        if (ScpPickupRouter.isEquipmentMirror(expected, slot)) {
            // Damage, enchantment or NBT changes made by vanilla gameplay are
            // written back to the authoritative source before the next sync.
            ItemStack updated = expected.copy();
            ScpPickupRouter.stripInternalMarkers(updated);
            updated.setCount(1);
            inventory.setEquipment(slot, updated);
            removeMirrorsExceptExpected(player, slot, expected);
            return;
        }

        removeMirrors(player, slot);
        ItemStack mirror = source.copy();
        mirror.setCount(1);
        ScpPickupRouter.markEquipmentMirror(mirror, slot);
        installExpectedMirror(player, slot, mirror);
    }

    private static ItemStack expectedMirror(ServerPlayer player,
            ScpEquipmentSlot slot) {
        EquipmentSlot vanilla = vanillaSlot(slot);
        if (vanilla != null) return player.getItemBySlot(vanilla);
        if (slot == ScpEquipmentSlot.ACCESSORY) return player.getOffhandItem();
        if (slot == ScpEquipmentSlot.WEAPON) {
            Inventory inventory = player.getInventory();
            return inventory.selected >= 0 && inventory.selected < inventory.items.size()
                    ? inventory.items.get(inventory.selected) : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static void installExpectedMirror(ServerPlayer player,
            ScpEquipmentSlot slot, ItemStack mirror) {
        EquipmentSlot vanilla = vanillaSlot(slot);
        if (vanilla != null) {
            player.setItemSlot(vanilla, mirror);
        } else if (slot == ScpEquipmentSlot.ACCESSORY) {
            player.setItemInHand(InteractionHand.OFF_HAND, mirror);
        } else if (slot == ScpEquipmentSlot.WEAPON) {
            Inventory inventory = player.getInventory();
            inventory.setItem(inventory.selected, mirror);
            inventory.setChanged();
        }
    }

    private static void removeMirrors(ServerPlayer player,
            ScpEquipmentSlot slot) {
        EquipmentSlot vanilla = vanillaSlot(slot);
        if (vanilla != null
                && ScpPickupRouter.isEquipmentMirror(player.getItemBySlot(vanilla), slot))
            player.setItemSlot(vanilla, ItemStack.EMPTY);
        if (slot == ScpEquipmentSlot.ACCESSORY
                && ScpPickupRouter.isEquipmentMirror(player.getOffhandItem(), slot))
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.items.size(); index++) {
            if (ScpPickupRouter.isEquipmentMirror(inventory.items.get(index), slot))
                inventory.items.set(index, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void removeMirrorsExceptExpected(ServerPlayer player,
            ScpEquipmentSlot slot, ItemStack expected) {
        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.items.size(); index++) {
            ItemStack candidate = inventory.items.get(index);
            if (candidate == expected) continue;
            if (ScpPickupRouter.isEquipmentMirror(candidate, slot))
                inventory.items.set(index, ItemStack.EMPTY);
        }
        if (slot != ScpEquipmentSlot.ACCESSORY
                && ScpPickupRouter.isEquipmentMirror(player.getOffhandItem(), slot))
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        inventory.setChanged();
    }

    private static EquipmentSlot vanillaSlot(ScpEquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static ScpEquipmentSlot[] activeSlots() {
        return new ScpEquipmentSlot[]{ScpEquipmentSlot.HEAD,
                ScpEquipmentSlot.CHEST, ScpEquipmentSlot.LEGS,
                ScpEquipmentSlot.FEET, ScpEquipmentSlot.ACCESSORY,
                ScpEquipmentSlot.WEAPON};
    }
}
