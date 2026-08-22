package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Keeps the death live-feed roster fresh and preserves the custom death UI while
 * the server hands a dead observer between dimensions to stream a remote target.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathSpectateClientEvents {
    private static ScpDeathScreen rememberedScreen;
    private static ScpDeathScreen lastQueriedScreen;
    private static int restoreDelayTicks;

    private DeathSpectateClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof ScpDeathScreen deathScreen) {
            rememberedScreen = deathScreen;
            restoreDelayTicks = 0;
            if (lastQueriedScreen != deathScreen) {
                lastQueriedScreen = deathScreen;
                MineZeroSpectateClient.refreshServerState();
            }
            return;
        }

        boolean stillDead = minecraft.player != null
                && (!minecraft.player.isAlive() || MineZeroClientState.active());
        boolean canRestore = stillDead && minecraft.level != null
                && MineZeroSpectateClient.transferActive();

        // A cross-dimension ClientboundRespawnPacket may briefly replace/clear
        // screens while ClientLevel is swapped. Never fight a real intermediate
        // screen; restore ours only once Minecraft has returned to no screen.
        if (canRestore && minecraft.screen == null && rememberedScreen != null) {
            if (++restoreDelayTicks >= 2) {
                minecraft.setScreen(rememberedScreen);
                restoreDelayTicks = 0;
            }
            return;
        }
        restoreDelayTicks = 0;

        if (!stillDead || minecraft.level == null) {
            rememberedScreen = null;
            lastQueriedScreen = null;
        }
    }
}
