package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.bl4ues.scpclassifieddirective.client.MineZeroClientState;
import com.bl4ues.scpclassifieddirective.client.MineZeroSpectateClient;
import com.bl4ues.scpclassifieddirective.client.ScpDeathScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the death-screen live personnel feed automatic while preserving the
 * cooperative MineZero rollback controls. This mixin deliberately injects only
 * at a method boundary owned by ScpDeathScreen; it contains no bytecode-call
 * redirects and therefore has no rebrand-sensitive method descriptor targets.
 */
@Mixin(value = ScpDeathScreen.class, remap = false)
public abstract class ScpDeathScreenLiveFeedMixin {
    @Shadow @Final private boolean hardcoreMode;
    @Shadow @Final private boolean mineZeroMode;
    @Shadow private Button mineZeroPrimaryButton;
    @Shadow private Button normalSpectateButton;
    @Shadow private Button previousSpectateButton;
    @Shadow private Button nextSpectateButton;
    @Shadow private boolean normalSpectating;
    @Shadow private long normalSpectateChangedAt;

    @Inject(method = "updateSpectateWidgets", at = @At("TAIL"), remap = false)
    private void scpClassifiedDirective$polishSpectateControls(CallbackInfo callback) {
        if (mineZeroMode) {
            if (MineZeroClientState.livingPlayers() > 0
                    && !MineZeroClientState.spectating()
                    && !MineZeroSpectateClient.transferActive()) {
                MineZeroClientState.startSpectating();
            }

            if (mineZeroPrimaryButton != null) {
                boolean rollbackReady = MineZeroClientState.allDead()
                        && !MineZeroClientState.spectating()
                        && !MineZeroSpectateClient.transferActive();
                mineZeroPrimaryButton.visible = rollbackReady;
                mineZeroPrimaryButton.active = rollbackReady
                        && !MineZeroClientState.restoring();

                // ScpDeathScreen already renders the authoritative ROLLBACK VOTE
                // count above the controls. Keep the action label stable instead
                // of duplicating that count inside the button.
                if (rollbackReady) {
                    mineZeroPrimaryButton.setMessage(Component.literal("Load Game"));
                }
            }
        } else if (!hardcoreMode) {
            // The normal death screen enters a personnel feed automatically. The
            // old manual Spectate/Return widget must be both inert and unreferenced
            // so Screen's generic input dispatch cannot activate it invisibly.
            if (normalSpectateButton != null) {
                normalSpectateButton.visible = false;
                normalSpectateButton.active = false;
                normalSpectateButton = null;
            }

            // Once CONNECTION LOST has finished, release the feed layout and let
            // the report card return to its centered state.
            if (normalSpectating && !MineZeroSpectateClient.active()) {
                normalSpectating = false;
                normalSpectateChangedAt = Util.getMillis();
            }

            // DeathSpectateClientEvents owns the authoritative roster refresh.
            // Start only after that response proves there is a living target.
            if (!normalSpectating
                    && !MineZeroSpectateClient.transferActive()
                    && MineZeroSpectateClient.availableTargetCount() > 0) {
                MineZeroSpectateClient.start();
                if (MineZeroSpectateClient.active()) {
                    normalSpectating = true;
                    normalSpectateChangedAt = Util.getMillis();
                }
            }
        }

        // Cycling controls are meaningful only when there is an actual choice.
        boolean multipleTargets = MineZeroSpectateClient.hasMultipleTargets();
        boolean showCycling = multipleTargets && MineZeroSpectateClient.active();
        if (previousSpectateButton != null) {
            previousSpectateButton.visible = showCycling;
            previousSpectateButton.active = showCycling;
        }
        if (nextSpectateButton != null) {
            nextSpectateButton.visible = showCycling;
            nextSpectateButton.active = showCycling;
        }
    }
}
