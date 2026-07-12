package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class ClientSprintStateEvents {

    private ClientSprintStateEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        boolean moving = Math.abs(player.input.forwardImpulse) > 0.01F || Math.abs(player.input.leftImpulse) > 0.01F;
        if (!moving) {
            if (minecraft.options.keySprint.isDown()) {
                minecraft.options.keySprint.setDown(false);
            }
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
        }
    }
}
