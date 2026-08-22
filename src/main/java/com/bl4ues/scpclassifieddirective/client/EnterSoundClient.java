package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.Scp106Sounds;

/** Plays the optional non-positional sound used when entering playable world state. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class EnterSoundClient {
    private static boolean pendingWorldEntryCue;

    private EnterSoundClient() {
    }

    /** Queue the cue until a genuine playable world frame is available. */
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
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive()
                || minecraft.screen instanceof ScpDeathScreen) {
            return;
        }

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
