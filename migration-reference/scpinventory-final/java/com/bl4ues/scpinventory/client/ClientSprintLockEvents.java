package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class ClientSprintLockEvents {

    private ClientSprintLockEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            enforceExhaustedSprintLock();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            enforceExhaustedSprintLock();
        }
    }

    private static void enforceExhaustedSprintLock() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (PlayerVitalsClient.getStamina() > 0.0F) {
            return;
        }

        player.setSprinting(false);
        if (mc.options.keySprint.isDown()) {
            mc.options.keySprint.setDown(false);
        }
    }
}
