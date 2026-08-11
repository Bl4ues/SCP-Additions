package net.mcreator.scpadditions.mixin.client;

import com.bl4ues.scpinventory.container.StorageContainerSupport;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/** Identifies semantic Slot overrides by JVM signature, not mapped names. */
@Mixin(value = StorageContainerSupport.class, remap = false)
public abstract class StorageContainerSupportCompatMixin {
    @Inject(method = "declaresSemanticBehavior", at = @At("HEAD"),
            cancellable = true)
    private static void scpAdditions$detectSemanticSlotSignatures(
            Class<?> type, Class<?> safeBase,
            CallbackInfoReturnable<Boolean> callback) {
        Class<?> current = type;
        while (current != null && current != safeBase) {
            for (Method method : current.getDeclaredMethods()) {
                if (scpAdditions$isSemanticSlotSignature(method)) {
                    callback.setReturnValue(true);
                    return;
                }
            }
            current = current.getSuperclass();
        }
        callback.setReturnValue(false);
    }

    @Unique
    private static boolean scpAdditions$isSemanticSlotSignature(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> result = method.getReturnType();

        if (result == boolean.class && parameters.length == 0) return true;
        if (result == boolean.class && parameters.length == 1
                && (parameters[0] == ItemStack.class
                || parameters[0] == Player.class)) return true;
        if (result == void.class && parameters.length == 2
                && parameters[0] == Player.class
                && parameters[1] == ItemStack.class) return true;
        return result == int.class
                && (parameters.length == 0
                || (parameters.length == 1
                && parameters[0] == ItemStack.class));
    }
}
