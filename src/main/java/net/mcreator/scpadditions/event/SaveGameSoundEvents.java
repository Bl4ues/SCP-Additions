package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.sound.GameplaySounds;

/** Plays save_game.ogg whenever a player's respawn point is successfully set. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveGameSoundEvents {
    private SaveGameSoundEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.isCanceled() || event.getNewSpawn() == null
                || !(event.getEntity() instanceof ServerPlayer player)
                || !ScpAdditionsModulesConfig.get().audio
                .saveGameSoundEnabled) {
            return;
        }

        player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
