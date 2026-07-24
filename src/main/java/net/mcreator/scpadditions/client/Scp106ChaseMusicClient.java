package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Keeps the chase loop local to whichever player receives the packet. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT)
public final class Scp106ChaseMusicClient {
    private static final Set<UUID> ACTIVE_SOURCES = new HashSet<>();
    private static Scp106ChaseMusicSound chase;
    private static boolean playStopAfterFade;

    private Scp106ChaseMusicClient() {
    }

    public static synchronized void setActive(UUID sourceId,
            boolean active) {
        if (sourceId == null) return;
        if (active) {
            ACTIVE_SOURCES.add(sourceId);
            playStopAfterFade = false;
            startOrRestore();
        } else {
            ACTIVE_SOURCES.remove(sourceId);
            if (ACTIVE_SOURCES.isEmpty() && chase != null
                    && !chase.isFadingOut()) {
                playStopAfterFade = true;
                chase.beginFadeOut();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (chase != null
                && !minecraft.getSoundManager().isActive(chase)) {
            chase = null;
            if (playStopAfterFade && ACTIVE_SOURCES.isEmpty()
                    && minecraft.player != null
                    && minecraft.level != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(
                                ScpAdditionsModSounds.SCP_106_STOP.get(),
                                1.0F));
            }
            playStopAfterFade = false;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(
            ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        ACTIVE_SOURCES.clear();
        playStopAfterFade = false;
        if (chase != null) {
            minecraft.getSoundManager().stop(chase);
            chase = null;
        }
    }

    private static void startOrRestore() {
        Minecraft minecraft = Minecraft.getInstance();
        if (chase != null && !chase.isFadingOut()
                && minecraft.getSoundManager().isActive(chase)) {
            return;
        }
        if (chase != null) minecraft.getSoundManager().stop(chase);
        chase = new Scp106ChaseMusicSound();
        minecraft.getSoundManager().play(chase);
    }
}
