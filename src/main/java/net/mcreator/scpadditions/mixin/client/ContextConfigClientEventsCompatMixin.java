package net.mcreator.scpadditions.mixin.client;

import com.bl4ues.scpinventory.client.ContextConfigClientEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps context-config hovered-slot lookup working in reobfuscated jars. */
@Mixin(value = ContextConfigClientEvents.class, remap = false)
public abstract class ContextConfigClientEventsCompatMixin {
    @Inject(method = "getSlotUnderMouse", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$getHoveredSlot(
            AbstractContainerScreen<?> screen,
            CallbackInfoReturnable<Object> callback) {
        callback.setReturnValue(((AbstractContainerScreenAccessor) (Object) screen)
                .scpAdditions$getHoveredSlot());
    }
}
