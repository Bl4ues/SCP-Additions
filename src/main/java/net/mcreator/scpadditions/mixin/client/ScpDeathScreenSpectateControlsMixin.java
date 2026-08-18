package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.mcreator.scpadditions.client.MineZeroSpectateClient;
import net.mcreator.scpadditions.client.ScpDeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The camera switch arrows exist only when there is another live feed to select. */
@Mixin(value = ScpDeathScreen.class, remap = false)
public abstract class ScpDeathScreenSpectateControlsMixin {
    @Shadow private Button previousSpectateButton;
    @Shadow private Button nextSpectateButton;

    @Inject(method = "updateSpectateWidgets", at = @At("TAIL"))
    private void scpAdditions$hideSingleTargetArrows(CallbackInfo callback) {
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
