package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Makes the visible SCP-079 world prompts authoritative for mouse input. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079InteractionClient {
    private Scp079InteractionClient() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079PlayableClient.cameraMode() || minecraft.screen != null
                || !event.isAttack() && !event.isUseItem()) return;

        Scp079PlayableVisuals.handleInteraction(
                event.isAttack(), event.isUseItem());
        // Spectator/player interaction is an implementation detail while 079 is
        // controlling a feed. Never let the same click leak into vanilla after
        // it has been interpreted as a facility action.
        event.setCanceled(true);
    }
}
