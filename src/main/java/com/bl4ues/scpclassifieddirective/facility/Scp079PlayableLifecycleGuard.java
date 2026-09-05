package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restores the human player state before Minecraft persists a playable-079 logout. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079PlayableLifecycleGuard {
    private Scp079PlayableLifecycleGuard() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !Scp079PlayableManager.isController(player)) {
            return;
        }
        // The role internally uses Spectator to detach the viewpoint. Restore
        // the saved origin game mode/position before the player data is written.
        Scp079PlayableManager.release(player);
    }
}
