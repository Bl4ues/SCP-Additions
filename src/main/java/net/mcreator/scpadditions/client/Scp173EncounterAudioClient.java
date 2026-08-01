package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.Scp173Entity;

/** Owns the two head-relative SCP-173 encounter layers for the local player. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp173EncounterAudioClient {
    private static final ResourceLocation ENCOUNTER = new ResourceLocation(
            ScpAdditionsMod.MODID, "scp_173_encounter");
    private static final ResourceLocation PARANOIA = new ResourceLocation(
            ScpAdditionsMod.MODID, "scp_173_paranoia");

    private static final float ENCOUNTER_BACKGROUND_VOLUME = 0.035F;
    private static final float ENCOUNTER_FOREGROUND_VOLUME = 0.55F;
    private static final float PARANOIA_BACKGROUND_VOLUME = 0.04F;
    private static final float PARANOIA_FOREGROUND_VOLUME = 0.82F;

    // The server already keeps the Blink HUD alive for ten seconds after the
    // last confirmed threat. This additional delay moves the musical swap near
    // the end of the HUD's 70-tick visual fade instead of changing instantly.
    private static final int PARANOIA_SWAP_DELAY_TICKS = 35;
    private static final int POST_DESPAWN_LINGER_TICKS = 600;
    private static final double LOCAL_STATUE_CHECK_RANGE = 128.0D;

    private static Scp173EncounterLayerSound encounter;
    private static Scp173EncounterLayerSound paranoia;
    private static boolean threatActive;
    private static boolean startedByScare;
    private static boolean paranoiaForeground;
    private static int paranoiaSwapDelay;
    private static int inactiveTicks;
    private static int noLoadedStatueTicks;
    private static int ticksSinceStart;

    private Scp173EncounterAudioClient() {
    }

    /** Called only when the server authorizes an actual SCP-173 reveal scare. */
    public static void onScare() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!validPlayer(minecraft)) return;

        ensurePlaying(minecraft);
        startedByScare = true;
        threatActive = true;
        inactiveTicks = 0;
        noLoadedStatueTicks = 0;
        paranoiaSwapDelay = 0;
        setParanoiaForeground(false);
    }

    /** Mirrors the server-authoritative Blink encounter state. */
    public static void setThreatActive(boolean active) {
        if (threatActive == active) return;
        threatActive = active;
        if (active) {
            inactiveTicks = 0;
            noLoadedStatueTicks = 0;
            return;
        }
        if (startedByScare && encounter != null && paranoia != null) {
            paranoiaSwapDelay = PARANOIA_SWAP_DELAY_TICKS;
        }
    }

    public static boolean isPlaying() {
        return encounter != null || paranoia != null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!validPlayer(minecraft)
                || !ScpAdditionsModulesConfig.get().scp173.enabled
                || !ScpAdditionsModulesConfig.get().blink.enabled) {
            stopImmediately();
            return;
        }
        if (minecraft.isPaused() || encounter == null || paranoia == null) {
            return;
        }

        ticksSinceStart++;
        if (ticksSinceStart > 1
                && !encounter.hasActiveAudio()
                && !paranoia.hasActiveAudio()) {
            resetReferences();
            return;
        }

        if (threatActive) {
            inactiveTicks = 0;
            noLoadedStatueTicks = 0;
            paranoiaSwapDelay = 0;
            return;
        }

        inactiveTicks++;
        if (paranoiaSwapDelay > 0) {
            paranoiaSwapDelay--;
            if (paranoiaSwapDelay == 0) {
                setParanoiaForeground(true);
            }
        }

        if (hasLoadedScp173(minecraft)) {
            noLoadedStatueTicks = 0;
        } else {
            noLoadedStatueTicks++;
        }

        if (paranoiaForeground
                && inactiveTicks >= POST_DESPAWN_LINGER_TICKS
                && noLoadedStatueTicks >= POST_DESPAWN_LINGER_TICKS) {
            beginFadeOut();
        }

        if (encounter.isFadingOut() && paranoia.isFadingOut()
                && !encounter.hasActiveAudio()
                && !paranoia.hasActiveAudio()) {
            resetReferences();
        }
    }

    private static void ensurePlaying(Minecraft minecraft) {
        boolean reusable = encounter != null && paranoia != null
                && !encounter.isFadingOut() && !paranoia.isFadingOut()
                && (encounter.hasActiveAudio() || paranoia.hasActiveAudio());
        if (reusable) return;

        stopImmediately();
        encounter = new Scp173EncounterLayerSound(ENCOUNTER,
                ENCOUNTER_BACKGROUND_VOLUME, ENCOUNTER_FOREGROUND_VOLUME);
        paranoia = new Scp173EncounterLayerSound(PARANOIA,
                PARANOIA_BACKGROUND_VOLUME, PARANOIA_FOREGROUND_VOLUME);
        encounter.setForeground(true);
        paranoia.setForeground(false);
        ticksSinceStart = 0;
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(encounter);
        minecraft.getSoundManager().play(paranoia);
    }

    private static void setParanoiaForeground(boolean value) {
        paranoiaForeground = value;
        if (encounter != null) encounter.setForeground(!value);
        if (paranoia != null) paranoia.setForeground(value);
    }

    private static void beginFadeOut() {
        if (encounter != null) encounter.beginFadeOut();
        if (paranoia != null) paranoia.beginFadeOut();
    }

    private static void stopImmediately() {
        if (encounter != null) encounter.stopImmediately();
        if (paranoia != null) paranoia.stopImmediately();
        resetReferences();
    }

    private static void resetReferences() {
        encounter = null;
        paranoia = null;
        threatActive = false;
        startedByScare = false;
        paranoiaForeground = false;
        paranoiaSwapDelay = 0;
        inactiveTicks = 0;
        noLoadedStatueTicks = 0;
        ticksSinceStart = 0;
    }

    private static boolean validPlayer(Minecraft minecraft) {
        return minecraft != null && minecraft.player != null
                && minecraft.level != null && minecraft.player.isAlive()
                && !minecraft.player.isCreative()
                && !minecraft.player.isSpectator();
    }

    private static boolean hasLoadedScp173(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return false;
        AABB area = minecraft.player.getBoundingBox().inflate(
                LOCAL_STATUE_CHECK_RANGE);
        return !minecraft.level.getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved()).isEmpty();
    }
}
