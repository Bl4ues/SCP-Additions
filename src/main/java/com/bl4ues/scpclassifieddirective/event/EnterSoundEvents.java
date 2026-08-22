package com.bl4ues.scpclassifieddirective.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;

/** Sends the world-entry cue request after the client finishes joining. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnterSoundEvents {
    private EnterSoundEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ScpClassifiedDirectiveMod.queueServerWork(20, () -> {
            if (!player.hasDisconnected()) {
                ScpEntityNetwork.playEnterSound(player);
            }
        });
    }
}
