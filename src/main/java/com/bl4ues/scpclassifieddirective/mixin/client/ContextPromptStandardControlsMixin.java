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
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Standard facility buttons/readers keep their hand prompt but no generic text,
 * while right-click remains owned by the blocks' native interaction path.
 */
@Mixin(ContextPromptClient.class)
public abstract class ContextPromptStandardControlsMixin {
    @ModifyArgs(method = "findBlockTarget",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/inventory/client/ContextPromptClient$ContextTarget;<init>(Lnet/minecraft/core/BlockPos;IZLnet/minecraft/world/phys/Vec3;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZZLnet/minecraft/resources/ResourceLocation;FZD)V"),
            remap = false)
    private static void scpclassifieddirective$makeStandardControlPromptIconOnly(
            Args args) {
        BlockPos pos = args.get(0);
        String interactionKey = args.get(4);
        if (!isStandardControl(pos)
                || NativeContextVariants.SCREWDRIVER_INTERACTION.equals(
                        interactionKey)) {
            return;
        }

        // showAction, showName, allowRightClick. The target stays alive so the
        // hand icon is still rendered, but ContextPromptClient neither labels
        // nor consumes the native block use action.
        args.set(7, false);
        args.set(8, false);
        args.set(9, false);
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
