package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.client.MineZeroClientState;
import net.mcreator.scpadditions.client.MineZeroSpectateClient;
import net.mcreator.scpadditions.client.ScpDeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the death-screen spectator and cooperative restore controls compact. */
@Mixin(value = ScpDeathScreen.class, remap = false)
public abstract class ScpDeathScreenSpectateControlsMixin {
    @Shadow private Button mineZeroPrimaryButton;
    @Shadow private Button previousSpectateButton;
    @Shadow private Button nextSpectateButton;

    @Redirect(method = "drawReportCard", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/MineZeroClientState;allDead()Z"),
            remap = false)
    private boolean scpAdditions$hideSeparateRollbackVote() {
        return false;
    }

    @Inject(method = "updateSpectateWidgets", at = @At("TAIL"))
    private void scpAdditions$polishSpectateControls(CallbackInfo callback) {
        if (mineZeroPrimaryButton != null && MineZeroClientState.allDead()) {
            int required = MineZeroClientState.requiredVotes();
            String label = required > 0
                    ? "Load Game  " + MineZeroClientState.votes() + " / " + required
                    : "Load Game";
            mineZeroPrimaryButton.setMessage(Component.literal(label));
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
