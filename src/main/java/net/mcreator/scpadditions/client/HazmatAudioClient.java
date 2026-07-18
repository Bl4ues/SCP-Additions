package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.equipment.HazmatSuitManager;
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
    private static boolean breathingAfterEquipSound;

    private HazmatAudioClient() {
    }

    /** The timed-equipment bar currently serves the two Hazmat action lengths. */
    public static void beginForDuration(int durationTicks) {
        if (durationTicks == HazmatSuitManager.EQUIP_DURATION_TICKS) {
            beginEquip();
        } else if (durationTicks == HazmatSuitManager.UNEQUIP_DURATION_TICKS) {
            beginRemove();
        }
    }

    public static void beginEquip() {
        stopActionSound();
        stopBreathing();
        breathingAfterEquipSound = false;
        action = Action.EQUIP;
        actionSound = SimpleSoundInstance.forUI(
                ScpAdditionsModSounds.HAZMAT_EQUIP.get(), 1.0F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(actionSound);
    }

    public static void beginRemove() {
        stopActionSound();
        stopBreathing();
        breathingAfterEquipSound = false;
        action = Action.REMOVE;
        actionSound = SimpleSoundInstance.forUI(
                ScpAdditionsModSounds.HAZMAT_REMOVE.get(), 1.0F, 1.0F);
        Minecraft.getInstance().getSoundManager().play(actionSound);
    }

    /**
     * A successful action may leave a short authored audio tail. Cancellation is
     * immediate, but normal completion lets that tail finish naturally.
     */
    public static void completeAction() {
        Action completed = action;
        action = Action.NONE;
        if (completed == Action.EQUIP) {
            breathingAfterEquipSound = true;
        } else if (completed == Action.REMOVE) {
            breathingAfterEquipSound = false;
            stopBreathing();
        }
    }

    public static void cancelAction() {
        Action canceled = action;
        stopActionSound();
        action = Action.NONE;
        breathingAfterEquipSound = false;
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

        // Keep the instance reference for the complete timed action so any
        // cancellation can always stop it, even while the sound engine is still
        // starting or loading the OGG on its first client ticks.
        if (action == Action.EQUIP || action == Action.REMOVE) {
            if (action == Action.REMOVE) {
                stopBreathing();
            }
            return;
        }

        if (actionSound != null
                && !minecraft.getSoundManager().isActive(actionSound)) {
            actionSound = null;
        }

        if (breathingAfterEquipSound) {
            if (actionSound != null) {
                return;
            }
            breathingAfterEquipSound = false;
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
        breathingAfterEquipSound = false;
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
