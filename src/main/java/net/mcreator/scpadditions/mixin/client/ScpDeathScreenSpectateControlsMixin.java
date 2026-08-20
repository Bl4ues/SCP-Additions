package net.mcreator.scpadditions.mixin.client;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.client.MineZeroClientState;
import net.mcreator.scpadditions.client.MineZeroSpectateClient;
import net.mcreator.scpadditions.client.ScpDeathScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the death-screen live personnel feed automatic and its cooperative
 * restore controls compact. A valid survivor is watched as part of the death
 * presentation itself instead of behind a separate Spectate/Return button.
 */
@Mixin(value = ScpDeathScreen.class, remap = false)
public abstract class ScpDeathScreenSpectateControlsMixin {
    @Shadow @Final private boolean hardcoreMode;
    @Shadow @Final private boolean mineZeroMode;
    @Shadow private Button mineZeroPrimaryButton;
    @Shadow private Button normalSpectateButton;
    @Shadow private Button previousSpectateButton;
    @Shadow private Button nextSpectateButton;
    @Shadow private boolean normalSpectating;
    @Shadow private long normalSpectateChangedAt;

    @Redirect(method = "drawReportCard", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/MineZeroClientState;allDead()Z"),
            remap = false)
    private boolean scpAdditions$hideSeparateRollbackVote() {
        return false;
    }

    @Inject(method = "updateSpectateWidgets", at = @At("TAIL"))
    private void scpAdditions$polishSpectateControls(CallbackInfo callback) {
        // Keep every injection in this mixin on methods owned by ScpDeathScreen.
        // Vanilla overrides such as Screen#init are re-obfuscated in a production
        // JAR and are not safe targets for this remap=false compatibility mixin.
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

                if (rollbackReady) {
                    int required = MineZeroClientState.requiredVotes();
                    String label = required > 0
                            ? "Load Game  " + MineZeroClientState.votes()
                                    + " / " + required
                            : "Load Game";
                    mineZeroPrimaryButton.setMessage(Component.literal(label));
                }
            }
        } else if (!hardcoreMode) {
            // The normal death screen no longer exposes a Spectate/Return
            // control. Disable the already-created widget and clear our field so
            // the next layout pass gives Load Game the full button width.
            if (normalSpectateButton != null) {
                normalSpectateButton.visible = false;
                normalSpectateButton.active = false;
                normalSpectateButton = null;
            }

            // After CONNECTION LOST has completed, release the normal feed layout
            // and return the report card to its centered state.
            if (normalSpectating && !MineZeroSpectateClient.active()) {
                normalSpectating = false;
                normalSpectateChangedAt = Util.getMillis();
            }

            // DeathSpectateClientEvents performs the authoritative roster query
            // when this screen first appears. Once its reply reports a survivor,
            // start the feed automatically while Load Game remains available.
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

        // Keep player cycling available only when there is an actual choice.
        boolean multipleTargets = MineZeroSpectateClient.hasMultipleTargets();
        if (previousSpectateButton != null) {
            previousSpectateButton.visible = multipleTargets && MineZeroSpectateClient.active();
            previousSpectateButton.active = previousSpectateButton.visible;
        }
        if (nextSpectateButton != null) {
            nextSpectateButton.visible = multipleTargets && MineZeroSpectateClient.active();
            nextSpectateButton.active = nextSpectateButton.visible;
        }
    }
}
