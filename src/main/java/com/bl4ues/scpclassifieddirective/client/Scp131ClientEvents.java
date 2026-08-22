package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp131StopPacket;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp131ClientEvents {
    private static final int HOLD_TICKS = 20;
    private static int heldTicks;
    private static boolean sent;

    private Scp131ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean down = false;
        if (player != null && minecraft.level != null && minecraft.screen == null
                && !player.isSpectator()) {
            down = Scp131Keybinds.DISMISS.isDown();
        }
        if (!down) {
            heldTicks = 0;
            sent = false;
            return;
        }
        heldTicks++;
        if (!sent && heldTicks >= HOLD_TICKS) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(new Scp131StopPacket());
            sent = true;
        }
    }
}
