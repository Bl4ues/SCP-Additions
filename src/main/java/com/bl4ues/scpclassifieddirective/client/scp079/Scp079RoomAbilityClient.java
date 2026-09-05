package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomAbilityManager;
import com.bl4ues.scpclassifieddirective.network.Scp079RoomAbilityNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Direct room-ability input routing while SCP-079 is inside a camera feed. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079RoomAbilityClient {
    private Scp079RoomAbilityClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !Scp079PlayableClient.cameraMode()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;

        while (Scp079Keybinds.BLACKOUT.consumeClick()) {
            Scp079RoomAbilityNetwork.request(
                    Scp079RoomAbilityManager.Ability.BLACKOUT);
        }
        while (Scp079Keybinds.LOCKDOWN.consumeClick()) {
            Scp079RoomAbilityNetwork.request(
                    Scp079RoomAbilityManager.Ability.LOCKDOWN);
        }
    }
}
