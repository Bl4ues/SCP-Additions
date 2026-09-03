package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.Keybinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side hard lock for the final SCP-714 blackout/coma state.
 *
 * <p>The server remains authoritative, but suppressing input locally prevents a
 * sleeping player from briefly opening inventory screens, swapping the offhand
 * or moving their camera between server corrections.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class Scp714ComaLockClient {
    private static boolean locked;
    private static float lockedYaw;
    private static float lockedPitch;
    private static int lockedSelectedSlot;

    private Scp714ComaLockClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean shouldLock = player != null
                && player.isAlive()
                && Scp714ClientState.isImmobilized();

        if (!shouldLock) {
            locked = false;
            return;
        }

        if (!locked) {
            locked = true;
            lockedYaw = player.getYRot();
            lockedPitch = player.getXRot();
            lockedSelectedSlot = player.getInventory().selected;
        }

        if (event.phase == TickEvent.Phase.START) {
            suppressGameplayKeys(minecraft);
            Screen screen = minecraft.screen;
            if (screen != null && !allowedScreen(screen)) {
                minecraft.setScreen(null);
            }
        }

        applyLock(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isLocked()) return;
        event.setYaw(lockedYaw);
        event.setPitch(Mth.clamp(lockedPitch, -90.0F, 90.0F));
        event.setRoll(0.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!isLocked()) return;
        Screen next = event.getNewScreen();
        if (next != null && !allowedScreen(next)) {
            event.setCanceled(true);
        }
    }

    /**
     * Cancel every HUD layer while unconscious and repaint the frame black.
     * This runs for each overlay, so even overlays registered by other systems
     * cannot end up above the blackout.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!isLocked()) return;
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, width, height, 0xFF000000);
    }

    private static boolean isLocked() {
        Minecraft minecraft = Minecraft.getInstance();
        return locked && minecraft.player != null
                && minecraft.player.isAlive()
                && Scp714ClientState.isImmobilized();
    }

    private static void applyLock(LocalPlayer player) {
        player.setYRot(lockedYaw);
        player.setXRot(Mth.clamp(lockedPitch, -90.0F, 90.0F));
        player.setYHeadRot(lockedYaw);
        player.getInventory().selected = Mth.clamp(lockedSelectedSlot, 0, 8);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        player.setSprinting(false);
        player.setShiftKeyDown(false);
        player.xxa = 0.0F;
        player.zza = 0.0F;
    }

    private static void suppressGameplayKeys(Minecraft minecraft) {
        lockKey(minecraft.options.keyUp);
        lockKey(minecraft.options.keyDown);
        lockKey(minecraft.options.keyLeft);
        lockKey(minecraft.options.keyRight);
        lockKey(minecraft.options.keyJump);
        lockKey(minecraft.options.keyShift);
        lockKey(minecraft.options.keySprint);
        lockKey(minecraft.options.keyAttack);
        lockKey(minecraft.options.keyUse);
        lockKey(minecraft.options.keyDrop);
        lockKey(minecraft.options.keyPickItem);
        lockKey(minecraft.options.keySwapOffhand);
        lockKey(minecraft.options.keyInventory);
        lockKey(minecraft.options.keyChat);
        lockKey(minecraft.options.keyCommand);
        for (KeyMapping hotbar : minecraft.options.keyHotbarSlots) {
            lockKey(hotbar);
        }

        // Drain the remaining SCP: Classified Directive gameplay mappings too.
        // The server also rejects authoritative item actions, so a late or forged
        // packet cannot turn a coma into an inventory-management minigame.
        lockKey(Keybinds.CONTEXT_CONFIG_SELECT);
        lockKey(Keybinds.STOW_HELD_ITEM);
        lockKey(Keybinds.QUICK_SAVE);
        lockKey(Scp173Keybinds.BLINK);
        lockKey(Scp131Keybinds.DISMISS);
        lockKey(Scp939Keybinds.HOLD_BREATH);
    }

    private static void lockKey(KeyMapping mapping) {
        mapping.setDown(false);
        while (mapping.consumeClick()) {
            // Drain presses accumulated before this tick.
        }
    }

    private static boolean allowedScreen(Screen screen) {
        // Escape and terminal screens remain available as application controls;
        // no gameplay/inventory screen may open while the player is unconscious.
        return screen instanceof PauseScreen
                || screen instanceof DeathScreen
                || screen instanceof DisconnectedScreen;
    }
}
