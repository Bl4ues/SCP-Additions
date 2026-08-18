package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.client.CustomDeathScreenEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures constructor data without depending on private DeathScreen fields. */
@Mixin(DeathScreen.class)
public abstract class DeathScreenCaptureMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void scpAdditions$captureDeathData(Component causeOfDeath,
            boolean hardcore, CallbackInfo callback) {
        CustomDeathScreenEvents.remember((DeathScreen) (Object) this,
                causeOfDeath, hardcore);
    }
}
