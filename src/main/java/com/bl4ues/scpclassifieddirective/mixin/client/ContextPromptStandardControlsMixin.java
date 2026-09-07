package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient;
import com.bl4ues.scpclassifieddirective.inventory.context.NativeContextVariants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Standard facility buttons/readers keep their compact hand-only prompt while
 * the contextual interaction path owns right-click. This lets the configured
 * physical anchor remain usable from the same side/angle where its prompt is
 * visible instead of falling back to Minecraft's direct block-hit requirement.
 */
@Mixin(ContextPromptClient.class)
public abstract class ContextPromptStandardControlsMixin {
    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/inventory/client/ContextPromptClient$ContextTarget;showAction()Z"),
            remap = false)
    private static boolean scpclassifieddirective$hideStandardControlAction(
            @Coerce Object target) {
        ContextPromptTargetAccessor accessor = accessor(target);
        return accessor != null
                && (!isNativeStandardControl(accessor)
                && accessor.scpclassifieddirective$showAction());
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/inventory/client/ContextPromptClient$ContextTarget;showName()Z"),
            remap = false)
    private static boolean scpclassifieddirective$hideStandardControlName(
            @Coerce Object target) {
        ContextPromptTargetAccessor accessor = accessor(target);
        return accessor != null
                && (!isNativeStandardControl(accessor)
                && accessor.scpclassifieddirective$showName());
    }

    private static ContextPromptTargetAccessor accessor(Object target) {
        return target instanceof ContextPromptTargetAccessor accessor
                ? accessor : null;
    }

    private static boolean isNativeStandardControl(
            ContextPromptTargetAccessor target) {
        if (target == null
                || NativeContextVariants.SCREWDRIVER_INTERACTION.equals(
                        target.scpclassifieddirective$interactionKey())) {
            return false;
        }
        return isStandardControl(target.scpclassifieddirective$pos());
    }

    private static boolean isStandardControl(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (pos == null || minecraft.level == null) return false;
        Block block = minecraft.level.getBlockState(pos).getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !"scp_classified_directive".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.startsWith("button_") || path.contains("reader");
    }
}
