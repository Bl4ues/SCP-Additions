package net.mcreator.scpadditions.mixin.compat;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.mcreator.scpadditions.compat.MineZeroCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets SCP Additions own the death flow while the optional MineZero patch is on. */
@Pseudo
@Mixin(targets = "boomcow.minezero.event.DeathEventHandler", remap = false)
public abstract class MineZeroDeathHandlerMixin {
    @Inject(method = "onPlayerDeath", at = @At("HEAD"), cancellable = true,
            require = 0)
    private void scpAdditions$deferAutomaticRestore(
            LivingDeathEvent event, CallbackInfo callback) {
        if (MineZeroCompatibility.enabled()) callback.cancel();
    }
}
