package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.Scp106Sounds;

/** Plays the optional non-positional sound used when joining a world. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class EnterSoundClient {
    private static boolean pendingWorldEntryCue;

    private EnterSoundClient() {
    }

    /**
     * The server packet can arrive while the receiving-level screen is still
     * visible. Record the request here and execute it only after the client has
     * actually handed control to gameplay.
     */
    public static void play() {
        pendingWorldEntryCue = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingWorldEntryCue) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            pendingWorldEntryCue = false;
            return;
        }

        boolean gameplayVisible = minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && minecraft.getOverlay() == null;
        if (!gameplayVisible) return;

        pendingWorldEntryCue = false;

        // This is the real transition point, regardless of whether enter.ogg
        // itself is enabled in the client's configuration.
        MainMenuMusicClient.onWorldEntryCue();

        if (!InventoryModuleRuntimeState.enterSoundEnabledForClient()) return;

        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(Scp106Sounds.ENTER.get(),
                        1.0F, 1.0F));
    }
}
