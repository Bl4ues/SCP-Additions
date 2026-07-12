package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.vitals.HorrorMovementNetwork;
import net.mcreator.scpadditions.vitals.VitalsModule;

/** Sends only sprint-input changes; speed and sprint state remain server-authoritative. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class HorrorMovementClientEvents {
    private static boolean lastRequestedSprint;

    private HorrorMovementClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            lastRequestedSprint = false;
            return;
        }

        boolean requestedSprint = VitalsModule.horrorMovementEnabled()
                && minecraft.screen == null
                && !player.isCreative()
                && !player.isSpectator()
                && minecraft.options.keySprint.isDown()
                && player.input.forwardImpulse > 0.0F
                && !player.isCrouching()
                && !player.isPassenger()
                && !player.getAbilities().flying
                && player.getFoodData().getFoodLevel() > 6
                && PlayerVitalsClient.canSprint();

        if (requestedSprint != lastRequestedSprint) {
            HorrorMovementNetwork.sendSprintInput(requestedSprint);
            lastRequestedSprint = requestedSprint;
        }
    }
}
