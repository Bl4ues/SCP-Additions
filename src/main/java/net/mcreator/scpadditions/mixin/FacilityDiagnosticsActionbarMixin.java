package net.mcreator.scpadditions.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps diagnostics cache-purge feedback inside the terminal UI. */
@Mixin(ServerPlayer.class)
public abstract class FacilityDiagnosticsActionbarMixin {
    private static final String CACHE_PURGE_UNAVAILABLE =
            "CACHE PURGE UNAVAILABLE: AUXILIARY POWER OFFLINE";
    private static final String CACHE_PURGE_COMPLETE =
            "REMOTE SESSION CACHE: PURGE COMPLETE";
    private static final String CACHE_LOCKOUT_PREFIX =
            "REMOTE SESSION CACHE LOCKOUT: ";

    @Inject(method = "displayClientMessage", at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$keepCachePurgeFeedbackInTerminal(
            Component message, boolean actionBar, CallbackInfo callback) {
        if (!actionBar || message == null) return;
        String text = message.getString();
        if (CACHE_PURGE_UNAVAILABLE.equals(text)
                || CACHE_PURGE_COMPLETE.equals(text)
                || text.startsWith(CACHE_LOCKOUT_PREFIX)) {
            callback.cancel();
        }
    }
}
