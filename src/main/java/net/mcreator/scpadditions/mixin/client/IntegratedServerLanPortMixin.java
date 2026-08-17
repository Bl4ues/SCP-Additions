package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.server.IntegratedServer;
import net.mcreator.scpadditions.client.PauseMenuLanCompatibilityClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Preserves the custom LAN port while publication runs through vanilla hooks. */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerLanPortMixin {
    @ModifyVariable(method = "publishServer", at = @At("HEAD"),
            argsOnly = true, ordinal = 0)
    private int scpAdditions$useCustomLanPort(int originalPort) {
        Integer override = PauseMenuLanCompatibilityClient.portOverride();
        return override == null ? originalPort : override;
    }
}
