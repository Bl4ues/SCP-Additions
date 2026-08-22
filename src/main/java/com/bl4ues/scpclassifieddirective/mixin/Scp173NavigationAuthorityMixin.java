package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173MovementController;
import com.bl4ues.scpclassifieddirective.entity.Scp173UnityNavigator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the legacy repair-only pass with one stable pursuit authority. */
@Mixin(value = Scp173MovementController.class, remap = false)
public abstract class Scp173NavigationAuthorityMixin {
    @Inject(method = "onLevelTickStart", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$disableLegacyRouteAuthority(
            TickEvent.LevelTickEvent event, CallbackInfo callback) {
        callback.cancel();
    }

    @Inject(method = "validateAndRepair", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$useUnityStyleNavigator(ServerLevel level,
            Scp173Entity statue, CallbackInfo callback) {
        Scp173UnityNavigator.tick(level, statue);
        callback.cancel();
    }
}
