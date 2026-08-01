package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.List;

/** Compact selection order for the custom visual hotbar. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomHotbarInputEvents {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private CustomHotbarInputEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        double scrollDelta = event.getScrollDelta();
        if (scrollDelta == 0.0D) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!CustomHotbarOverlay.isActiveFor(player)) return;

        Inventory inventory = player.getInventory();
        List<Integer> occupied = occupiedSlots(inventory);
        if (occupied.isEmpty()) return;

        int direction = scrollDelta < 0.0D ? 1 : -1;
        int nextSlot = nextSlot(inventory, occupied, direction);
        if (nextSlot < 0 || nextSlot == inventory.selected) {
            event.setCanceled(true);
            return;
        }

        inventory.selected = nextSlot;
        inventory.setChanged();
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(
                    new ServerboundSetCarriedItemPacket(nextSlot));
        }
        event.setCanceled(true);
    }

    private static List<Integer> occupiedSlots(Inventory inventory) {
        List<Integer> slots = new ArrayList<>();
        int end = Math.min(HOTBAR_SLOT_COUNT, inventory.items.size());
        for (int slot = 0; slot < end; slot++) {
            if (!inventory.items.get(slot).isEmpty()) slots.add(slot);
        }
        return slots;
    }

    private static int nextSlot(Inventory inventory, List<Integer> occupied,
            int direction) {
        int current = inventory.selected;
        int occupiedIndex = occupied.indexOf(current);
        int emptySlot = firstEmptySlot(inventory);

        // Any selected empty slot represents the one intentional blank entry
        // between the end and beginning of the compact list.
        if (occupiedIndex < 0) {
            return direction > 0
                    ? occupied.get(0)
                    : occupied.get(occupied.size() - 1);
        }

        if (direction > 0) {
            if (occupiedIndex + 1 < occupied.size()) {
                return occupied.get(occupiedIndex + 1);
            }
            return emptySlot >= 0 ? emptySlot : occupied.get(0);
        }

        if (occupiedIndex > 0) {
            return occupied.get(occupiedIndex - 1);
        }
        return emptySlot >= 0
                ? emptySlot : occupied.get(occupied.size() - 1);
    }

    private static int firstEmptySlot(Inventory inventory) {
        int end = Math.min(HOTBAR_SLOT_COUNT, inventory.items.size());
        for (int slot = 0; slot < end; slot++) {
            if (inventory.items.get(slot).isEmpty()) return slot;
        }
        return -1;
    }
}
