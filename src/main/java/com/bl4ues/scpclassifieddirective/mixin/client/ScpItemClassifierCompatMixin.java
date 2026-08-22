package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Detects Item#use overrides by signature instead of its mapped method name. */
@Mixin(value = ScpItemClassifier.class, remap = false)
public abstract class ScpItemClassifierCompatMixin {
    @Unique
    private static final Map<Class<?>, Boolean> scpClassifiedDirective$airUseOverrides =
            new ConcurrentHashMap<>();

    @Inject(method = "overridesAirUse", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$detectAirUseBySignature(
            Item item, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(scpClassifiedDirective$airUseOverrides.computeIfAbsent(
                item.getClass(), ScpItemClassifierCompatMixin::scpClassifiedDirective$hasAirUseOverride));
    }

    @Unique
    private static boolean scpClassifiedDirective$hasAirUseOverride(Class<?> itemClass) {
        for (Method method : itemClass.getMethods()) {
            if (method.getDeclaringClass() == Item.class
                    || method.getReturnType() != InteractionResultHolder.class) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 3
                    && parameters[0] == Level.class
                    && parameters[1] == Player.class
                    && parameters[2] == InteractionHand.class) {
                return true;
            }
        }
        return false;
    }
}
