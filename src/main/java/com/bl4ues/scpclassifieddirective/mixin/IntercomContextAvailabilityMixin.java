package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomModule;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the two static Intercom context rules behave like one stateful prompt. */
@Mixin(value = ContextInteractionRegistry.Rule.class, remap = false)
public abstract class IntercomContextAvailabilityMixin {
    @Shadow @Final private Block block;
    @Shadow @Final private String interactionKey;

    @Inject(method = "isAvailable(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$intercomStatePrompt(Level level,
            BlockPos pos, BlockState state, Player player,
            CallbackInfoReturnable<Boolean> cir) {
        if (block != IntercomModule.BLOCK.get() || state == null
                || !state.hasProperty(IntercomModule.ACTIVE)) return;
        boolean active = state.getValue(IntercomModule.ACTIVE);
        if ("turn_on_intercom".equals(interactionKey)) {
            cir.setReturnValue(!active);
        } else if ("turn_off_intercom".equals(interactionKey)) {
            cir.setReturnValue(active);
        }
    }
}
