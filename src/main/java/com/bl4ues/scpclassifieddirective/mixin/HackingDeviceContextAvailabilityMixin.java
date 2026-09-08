package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceClientState;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.hacking.HackingDeviceAttachmentManager;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import com.bl4ues.scpclassifieddirective.inventory.context.HackingDeviceContextDefaults;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

/** Hides the reader's ordinary controls while a temporary hack session owns it. */
@Mixin(value = ContextInteractionRegistry.Rule.class, remap = false)
public abstract class HackingDeviceContextAvailabilityMixin {
    @Shadow @Final private Block block;
    @Shadow @Final private String interactionKey;

    @Inject(method = "isAvailable(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$hackingDevicePromptState(Level level,
            BlockPos pos, BlockState state, Player player,
            CallbackInfoReturnable<Boolean> cir) {
        if (level == null || pos == null || state == null
                || !HackingDeviceAttachmentManager.isCompatibleTarget(state)) {
            return;
        }

        boolean attached = level.isClientSide
                ? HackingDeviceClientState.isAttached(pos)
                : level instanceof ServerLevel serverLevel
                && HackingDeviceAttachmentManager.isAttached(serverLevel, pos);

        if (HackingDeviceContextDefaults.ATTACH_KEY.equals(interactionKey)) {
            cir.setReturnValue(!attached);
            return;
        }
        if (!attached) return;

        if (KeycardReaderLevels.describe(state) != null) {
            cir.setReturnValue(false);
            return;
        }

        if (state.is(ObjectContainmentUnitModule.UNIT.get())
                && ("open_object_containment_unit".equals(interactionKey)
                || "configure_object_containment_unit".equals(interactionKey))) {
            cir.setReturnValue(false);
        }
    }
}
