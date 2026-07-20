package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.Scp131StopPacket;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp131StopPacket());
            sent = true;
        }
    }
}
