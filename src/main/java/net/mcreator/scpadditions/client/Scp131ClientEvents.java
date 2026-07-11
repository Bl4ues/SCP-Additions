package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.Scp131StopPacket;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
                && !player.isCreative() && !player.isSpectator()) {
            down = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_G);
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
