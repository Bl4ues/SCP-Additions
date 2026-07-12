package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.mcreator.scpadditions.vitals.StaminaBlockerAccess;
import net.mcreator.scpadditions.vitals.VitalsModule;

/** Client prediction used by the HUD and immediate sprint feedback. */
public final class PlayerVitalsClient {
    public static final float MAX_STAMINA = 100.0F;
    private static final float STAMINA_DRAIN_PER_TICK = MAX_STAMINA / (5.0F * 20.0F);
    private static final float STAMINA_REGEN_PER_TICK = MAX_STAMINA / (5.0F * 20.0F);
    private static final int REGEN_DELAY_TICKS = 20;
    private static final long DAMAGE_FLASH_DURATION_MS = 1000L;

    private static float stamina = MAX_STAMINA;
    private static int regenDelayTicks;
    private static float lastHealth = -1.0F;
    private static long damageFlashStartedAt = -1L;

    private PlayerVitalsClient() {
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            resetAll();
            return;
        }

        if (VitalsModule.healthHudEnabled()) {
            updateDamageFlash(player);
        } else {
            lastHealth = -1.0F;
            damageFlashStartedAt = -1L;
        }

        if (VitalsModule.staminaEnabled()) {
            updateStamina(minecraft, player);
        } else {
            resetStamina();
        }
    }

    private static void updateDamageFlash(LocalPlayer player) {
        float health = player.getHealth();
        if (lastHealth >= 0.0F && health < lastHealth - 0.01F) {
            damageFlashStartedAt = System.currentTimeMillis();
        }
        lastHealth = health;
    }

    private static void updateStamina(Minecraft minecraft, LocalPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            resetStamina();
            return;
        }

        if (StaminaBlockerAccess.isBlocked(player)) {
            stamina = 0.0F;
            regenDelayTicks = REGEN_DELAY_TICKS;
            forceStopSprint(minecraft, player);
            return;
        }

        boolean moving = Math.abs(player.input.forwardImpulse) > 0.01F
                || Math.abs(player.input.leftImpulse) > 0.01F;
        boolean sprintKeyHeld = minecraft.options.keySprint.isDown();
        boolean sprintingOrTrying = player.isSprinting() || sprintKeyHeld;
        boolean tryingToSprintWhileMoving = moving && sprintingOrTrying;

        if (tryingToSprintWhileMoving) {
            if (stamina > 0.0F) {
                stamina = Math.max(0.0F, stamina - STAMINA_DRAIN_PER_TICK);
            }
            regenDelayTicks = REGEN_DELAY_TICKS;
            if (stamina <= 0.0F) {
                stamina = 0.0F;
                forceStopSprint(minecraft, player);
            }
            return;
        }

        if (stamina <= 0.0F) {
            stamina = 0.0F;
            forceStopSprint(minecraft, player);
            if (moving && sprintKeyHeld) {
                regenDelayTicks = REGEN_DELAY_TICKS;
                return;
            }
        }

        if (!moving) {
            player.setSprinting(false);
        }

        if (regenDelayTicks > 0) {
            regenDelayTicks--;
        } else if (stamina < MAX_STAMINA) {
            stamina = Math.min(MAX_STAMINA, stamina + STAMINA_REGEN_PER_TICK);
        }
    }

    private static void forceStopSprint(Minecraft minecraft, LocalPlayer player) {
        player.setSprinting(false);
        if (minecraft.options.keySprint.isDown()) {
            minecraft.options.keySprint.setDown(false);
        }
    }

    private static void resetStamina() {
        stamina = MAX_STAMINA;
        regenDelayTicks = 0;
    }

    private static void resetAll() {
        resetStamina();
        lastHealth = -1.0F;
        damageFlashStartedAt = -1L;
    }

    public static float getStamina() {
        return stamina;
    }

    public static float getMaxStamina() {
        return MAX_STAMINA;
    }

    public static float getStaminaRatio() {
        return MAX_STAMINA <= 0.0F
                ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, stamina / MAX_STAMINA));
    }

    public static float getDamageFlashAlpha() {
        if (damageFlashStartedAt < 0L) {
            return 0.0F;
        }

        long elapsed = System.currentTimeMillis() - damageFlashStartedAt;
        if (elapsed >= DAMAGE_FLASH_DURATION_MS) {
            damageFlashStartedAt = -1L;
            return 0.0F;
        }

        float progress = elapsed / (float) DAMAGE_FLASH_DURATION_MS;
        float fade = progress < 0.18F
                ? progress / 0.18F
                : 1.0F - ((progress - 0.18F) / 0.82F);
        return Math.max(0.0F, Math.min(0.65F, fade * 0.65F));
    }
}
