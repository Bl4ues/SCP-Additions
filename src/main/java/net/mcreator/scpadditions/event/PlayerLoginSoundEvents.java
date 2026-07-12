package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerLoginSoundEvents {
    private PlayerLoginSoundEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ScpAdditionsMod.queueServerWork(10, () -> {
            if (!player.isRemoved() && player.connection != null) {
                ScpEntityNetwork.playEnterSound(player);
            }
        });
    }
}
