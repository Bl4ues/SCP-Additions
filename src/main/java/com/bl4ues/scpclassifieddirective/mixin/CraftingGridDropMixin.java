package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.inventory.crafting.ScpCraftingService;
import com.bl4ues.scpclassifieddirective.inventory.crafting.ScpCraftingState;
import com.bl4ues.scpclassifieddirective.inventory.network.CraftingActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds server-authoritative world drops for items held in the crafting grid. */
@Mixin(value = ScpCraftingService.class, remap = false)
public abstract class CraftingGridDropMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$dropCraftingGridItem(ServerPlayer player,
            int action, int source, int target,
            net.minecraft.resources.ResourceLocation recipeId,
            CallbackInfo callback) {
        if (action != CraftingActionPacket.MOVE_GRID_TO_WORLD) return;
        callback.cancel();
        if (player == null || player.isSpectator()
                || source < 0 || source >= ScpCraftingState.GRID_SIZE) {
            return;
        }

        ScpCraftingState.Data state = ScpCraftingState.load(player);
        ItemStack stack = state.getGridItem(source);
        if (stack.isEmpty()) return;

        ItemStack dropped = stack.copy();
        state.setGridItem(source, ItemStack.EMPTY);
        ScpCraftingState.save(player, state);
        player.drop(dropped, false);
        ScpCraftingService.syncState(player, state);
    }
}
