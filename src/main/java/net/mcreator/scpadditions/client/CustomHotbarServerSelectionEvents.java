package net.mcreator.scpadditions.client;

import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Prevents usable-session maintenance from undoing custom hotbar navigation. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class CustomHotbarServerSelectionEvents {
    private static final Map<UUID, Integer> REQUESTED_SELECTIONS =
            new HashMap<>();

    private CustomHotbarServerSelectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureSelection(TickEvent.PlayerTickEvent event) {
        if (!applies(event)) return;
        ServerPlayer player = (ServerPlayer) event.player;
        REQUESTED_SELECTIONS.put(player.getUUID(),
                player.getInventory().selected);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreSelection(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        Integer requested = REQUESTED_SELECTIONS.remove(player.getUUID());
        if (!applies(event) || requested == null) return;

        Inventory inventory = player.getInventory();
        if (requested < 0 || requested >= 9
                || requested >= inventory.items.size()
                || inventory.selected == requested) {
            return;
        }

        inventory.selected = requested;
        player.connection.send(new ClientboundSetCarriedItemPacket(requested));
    }

    private static boolean applies(TickEvent.PlayerTickEvent event) {
        return event.phase == TickEvent.Phase.END
                && !event.player.level().isClientSide
                && event.player instanceof ServerPlayer player
                && !player.isCreative()
                && !player.isSpectator()
                && ScpAdditionsModulesConfig.get().inventory.enabled
                && ScpAdditionsModulesConfig.get().inventory.customHotbar;
    }
}
