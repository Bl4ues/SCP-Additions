package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
     * The server packet may arrive while Minecraft is still showing the 0-100%
     * receiving-level screen. Defer the transition until an actual world frame
     * has completed rendering instead of guessing from screen/player fields.
     */
    public static void play() {
        pendingWorldEntryCue = true;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!pendingWorldEntryCue
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;

        pendingWorldEntryCue = false;
        MainMenuMusicClient.onWorldEntryCue();

        if (InventoryModuleRuntimeState.enterSoundEnabledForClient()) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(Scp106Sounds.ENTER.get(),
                            1.0F, 1.0F));
        }
    }

    /** Clear an abandoned cue if the connection closes before a world renders. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && pendingWorldEntryCue
                && Minecraft.getInstance().getConnection() == null) {
            pendingWorldEntryCue = false;
        }
    }
}
