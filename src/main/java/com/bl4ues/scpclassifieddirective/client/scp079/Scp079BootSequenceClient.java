package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.Scp079InitialFeedNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One boot sequence per playable SCP-079 control session. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079BootSequenceClient {
    private static boolean observedActive;
    private static boolean completed;
    private static boolean awaitingInitialFeed;

    private Scp079BootSequenceClient() {
    }

    public static boolean completed() {
        return completed;
    }

    public static void open() {
        if (!Scp079PlayableClient.active()
                || !Scp079PlayableClient.networkAvailable()
                || completed) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof Scp079BootSequenceScreen)) {
            minecraft.setScreen(new Scp079BootSequenceScreen());
        }
    }

    static void finishSequence() {
        if (completed || awaitingInitialFeed
                || !Scp079PlayableClient.active()) return;
        completed = true;
        awaitingInitialFeed = true;
        Scp079InitialFeedNetwork.request();
    }

    public static void receiveInitialFeed(boolean cameraOpened) {
        if (!awaitingInitialFeed) return;
        awaitingInitialFeed = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079PlayableClient.active()) {
            minecraft.setScreen(null);
            return;
        }

        if (cameraOpened) {
            minecraft.setScreen(null);
            return;
        }

        minecraft.setScreen(null);
        if (Scp079PlayableClient.networkAvailable()) {
            Scp079FacilityMapScreen.open();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean active = Scp079PlayableClient.active();
        if (active != observedActive) {
            observedActive = active;
            completed = false;
            awaitingInitialFeed = false;
            if (!active && Minecraft.getInstance().screen
                    instanceof Scp079BootSequenceScreen) {
                Minecraft.getInstance().setScreen(null);
            }
        }
    }
}
