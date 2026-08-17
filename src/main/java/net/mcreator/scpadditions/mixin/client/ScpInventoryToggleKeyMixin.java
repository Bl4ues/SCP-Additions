package net.mcreator.scpadditions.mixin.client;

import com.bl4ues.scpinventory.client.Keybinds;
import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mirrors vanilla inventory behavior by letting the open key close the screen. */
@Mixin(value = ScpInventoryScreen.class, remap = false)
public abstract class ScpInventoryToggleKeyMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$closeWithInventoryKey(int keyCode, int scanCode,
            int modifiers, CallbackInfoReturnable<Boolean> callback) {
        if (!Keybinds.OPEN_SCP_INVENTORY.matches(keyCode, scanCode)) return;
        ((Screen) (Object) this).onClose();
        callback.setReturnValue(true);
    }
}
