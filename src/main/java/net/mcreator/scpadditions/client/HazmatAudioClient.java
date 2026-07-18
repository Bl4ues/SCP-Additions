package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Owns all local Hazmat Suit audio so interrupted actions stop immediately. */
public final class HazmatAudioClient {
    private enum Action {
        NONE,
        EQUIP,
        REMOVE
    }

    private static Action action = Action.NONE;
    private static SimpleSoundInstance actionSound;
    private static HazmatBreathingSound breathingSound;

    private HazmatAudioClient() {
    }

    public static void beginEquip() {
        stopActionSound();
        stopBreathing();
        action = Action.EQUIP;
        actionSound = SimpleSoundInstance.forUI(
                ScpAdditionsModSounds.HAZMAT_EQUIP.get(), 1.0F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(actionSound);
    }

    public static void beginRemove() {
        stopActionSound();
        stopBreathing();
        action = Action.REMOVE;
        actionSound = SimpleSoundInstance.forUI(
                ScpAdditionsModSounds.HAZMAT_REMOVE.get(), 1.0F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(actionSound);
    }

    public static void completeAction() {
        Action completed = action;
        stopActionSound();
        action = Action.NONE;
        if (completed == Action.EQUIP) {
            startBreathing();
        } else if (completed == Action.REMOVE) {
            stopBreathing();
        }
    }

    public static void cancelAction() {
        Action canceled = action;
        stopActionSound();
        action = Action.NONE;
        if (canceled == Action.REMOVE) {
            startBreathingIfEquipped();
        }
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.isAlive()) {
            stopAll();
            return;
        }

        if (action == Action.EQUIP || action == Action.REMOVE) {
            if (action == Action.REMOVE) {
                stopBreathing();
            }
            return;
        }

        if (HazmatSuitAccess.isFullyEquipped(minecraft.player)) {
            startBreathing();
        } else {
            stopBreathing();
        }
    }

    public static void stopAll() {
        stopActionSound();
        stopBreathing();
        action = Action.NONE;
    }

    private static void startBreathingIfEquipped() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && HazmatSuitAccess.isFullyEquipped(minecraft.player)) {
            startBreathing();
        }
    }

    private static void startBreathing() {
        if (breathingSound != null) {
            return;
        }
        breathingSound = new HazmatBreathingSound();
        Minecraft.getInstance().getSoundManager().play(breathingSound);
    }

    private static void stopActionSound() {
        if (actionSound == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().stop(actionSound);
        actionSound = null;
    }

    private static void stopBreathing() {
        if (breathingSound == null) {
            return;
        }
        breathingSound.finish();
        Minecraft.getInstance().getSoundManager().stop(breathingSound);
        breathingSound = null;
    }
}
