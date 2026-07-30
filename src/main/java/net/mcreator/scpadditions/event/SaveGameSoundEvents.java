package net.mcreator.scpadditions.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.sound.GameplaySounds;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plays save_game.ogg after a player's effective respawn point changes. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveGameSoundEvents {
    private static final Map<UUID, SpawnSnapshot> LAST_SPAWNS = new HashMap<>();

    private SaveGameSoundEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SPAWNS.put(player.getUUID(), snapshot(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SPAWNS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;
        SpawnSnapshot current = snapshot(player);
        SpawnSnapshot previous = LAST_SPAWNS.put(player.getUUID(), current);
        if (previous == null || previous.equals(current)
                || current.position() == null
                || !ScpAdditionsModulesConfig.get().audio
                .saveGameSoundEnabled) return;
        player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static SpawnSnapshot snapshot(ServerPlayer player) {
        BlockPos position = player.getRespawnPosition();
        return new SpawnSnapshot(player.getRespawnDimension(),
                position == null ? null : position.immutable(),
                player.isRespawnForced());
    }

    private record SpawnSnapshot(ResourceKey<Level> dimension,
            @Nullable BlockPos position, boolean forced) {}
}
