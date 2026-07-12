package net.mcreator.scpadditions.inventory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.inventory.ScpInventoryRequestSyncPacket;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpInventoryClientEvents {
    private ScpInventoryClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !ScpAdditionsModulesConfig.get().inventory.enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ScpWorldPromptClient.tick();

        while (ScpInventoryKeybinds.OPEN.consumeClick()) {
            if (minecraft.screen == null) {
                ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                        new ScpInventoryRequestSyncPacket());
                minecraft.setScreen(new ScpInventoryScreen());
            }
        }
    }

    /**
     * Prevent vanilla block/item use when a configured world prompt owns the
     * right click. Forge fires this once per hand, so only main hand sends the
     * packet while both events are cancelled.
     */
    @SubscribeEvent
    public static void onInteractionInput(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()
                || !ScpAdditionsModulesConfig.get().inventory.enabled
                || !ScpWorldPromptClient.ownsRightClick()) {
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            ScpWorldPromptClient.triggerRightClick();
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
