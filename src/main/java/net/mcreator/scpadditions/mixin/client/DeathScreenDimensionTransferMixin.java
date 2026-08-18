package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.mcreator.scpadditions.client.MineZeroSpectateClient;
import net.mcreator.scpadditions.client.ScpDeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the SCP death UI alive while a remote live feed changes dimensions. */
@Mixin(Minecraft.class)
public abstract class DeathScreenDimensionTransferMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$preserveDeathFeedScreen(Screen next,
            CallbackInfo callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (!(minecraft.screen instanceof ScpDeathScreen)
                || !MineZeroSpectateClient.transferActive()) {
            return;
        }

        // Cross-dimension teleports normally clear the current screen or briefly
        // install ReceivingLevelScreen. The live feed already owns presentation,
        // so allowing either would tear down the camera/session mid-transfer.
        if (next == null || next instanceof ReceivingLevelScreen) {
            callback.cancel();
        }
    }
}
