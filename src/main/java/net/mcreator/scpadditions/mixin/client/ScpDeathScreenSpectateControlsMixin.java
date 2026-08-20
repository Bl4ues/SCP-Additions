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

    @Inject(method = "init", at = @At("TAIL"))
    private void scpAdditions$startPersonnelFeedAutomatically(
            CallbackInfo callback) {
        if (mineZeroMode) {
            if (MineZeroClientState.livingPlayers() > 0
                    && !MineZeroClientState.spectating()
                    && !MineZeroSpectateClient.transferActive()) {
                MineZeroClientState.startSpectating();
            }
            return;
        }

        if (hardcoreMode) return;

        // The normal death screen no longer exposes a Spectate/Return control.
        // Keep the old widget inert as well as unreferenced so Screen's generic
        // input dispatch cannot activate an invisible leftover button.
        if (normalSpectateButton != null) {
            normalSpectateButton.visible = false;
            normalSpectateButton.active = false;
            normalSpectateButton = null;
        }

        // Reset stale roster knowledge before the first automatic decision. The
        // authoritative query response supplies availableTargetCount(), avoiding
        // PlayerInfo guesses that can mistake another dead player for a survivor.
        if (!MineZeroSpectateClient.transferActive()) {
            MineZeroSpectateClient.refreshServerState();
        }
    }

    @Redirect(method = "drawReportCard", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/MineZeroClientState;allDead()Z"),
            remap = false)
    private boolean scpAdditions$hideSeparateRollbackVote() {
        return false;
    }

    @Inject(method = "updateSpectateWidgets", at = @At("TAIL"))
    private void scpAdditions$polishSpectateControls(CallbackInfo callback) {
        if (mineZeroMode) {
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
        } else if (!hardcoreMode && !normalSpectating
                && !MineZeroSpectateClient.transferActive()
                && MineZeroSpectateClient.availableTargetCount() > 0) {
            // The first server roster response found a survivor. Enter the feed
            // immediately and leave Load Game available on the death card.
            MineZeroSpectateClient.start();
            if (MineZeroSpectateClient.active()) {
                normalSpectating = true;
                normalSpectateChangedAt = Util.getMillis();
            }
        }

        if (MineZeroSpectateClient.hasMultipleTargets()) return;
        if (previousSpectateButton != null) {
            previousSpectateButton.visible = false;
            previousSpectateButton.active = false;
        }
        if (nextSpectateButton != null) {
            nextSpectateButton.visible = false;
            nextSpectateButton.active = false;
        }
    }
}
