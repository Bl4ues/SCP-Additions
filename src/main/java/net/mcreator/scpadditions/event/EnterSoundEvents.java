package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

/** Sends the optional world-entry cue after the client finishes joining. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnterSoundEvents {
    private EnterSoundEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !ScpAdditionsModulesConfig.get().audio.enterSoundEnabled) {
            return;
        }

        ScpAdditionsMod.queueServerWork(20, () -> {
            if (!player.hasDisconnected()
                    && ScpAdditionsModulesConfig.get().audio.enterSoundEnabled) {
                ScpEntityNetwork.playEnterSound(player);
            }
        });
    }
}
