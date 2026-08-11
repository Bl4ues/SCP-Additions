package net.mcreator.scpadditions.mixin.client;

import com.bl4ues.scpinventory.context.ContextConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/** Detects block right-click behavior without relying on the Mojmap use name. */
@Mixin(value = ContextConfigManager.class, remap = false)
public abstract class ContextConfigManagerCompatMixin {
    @Inject(method = "likelySupportsRightClick", at = @At("HEAD"),
            cancellable = true)
    private static void scpAdditions$detectUseBySignature(BlockState state,
            CallbackInfoReturnable<Boolean> callback) {
        if (state == null || state.isAir()) {
            callback.setReturnValue(false);
            return;
        }
        for (Method method : state.getBlock().getClass().getMethods()) {
            Class<?> declaring = method.getDeclaringClass();
            if (declaring == Block.class || declaring == BlockBehaviour.class
                    || method.getReturnType() != InteractionResult.class) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 6
                    && parameters[0] == BlockState.class
                    && parameters[1] == Level.class
                    && parameters[2] == BlockPos.class
                    && parameters[3] == Player.class
                    && parameters[4] == InteractionHand.class
                    && parameters[5] == BlockHitResult.class) {
                callback.setReturnValue(true);
                return;
            }
        }
        callback.setReturnValue(false);
    }
}
