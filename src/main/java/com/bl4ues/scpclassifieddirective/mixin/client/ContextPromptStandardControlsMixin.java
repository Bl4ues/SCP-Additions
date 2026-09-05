package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Standard keycard readers and legacy door buttons remain normal world controls;
 * they do not advertise the generic contextual-interaction hand/"Use" prompt.
 */
@Mixin(ContextPromptClient.class)
public abstract class ContextPromptStandardControlsMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "findBlockTarget",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/inventory/context/ContextInteractionRegistry;getBlockRules(Lnet/minecraft/world/level/block/Block;)Ljava/util/List;"),
            remap = false)
    private static List scpclassifieddirective$hideStandardControlPrompt(
            Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null && "scp_classified_directive".equals(id.getNamespace())) {
            String path = id.getPath();
            boolean keycardReader = path.equals("right_reader")
                    || path.equals("left_reader")
                    || path.endsWith("_right_reader")
                    || path.endsWith("_left_reader")
                    || path.endsWith("_reader_wrong")
                    || path.endsWith("_reader_accept");
            boolean standardButton = path.equals("button_roff")
                    || path.equals("button_ron")
                    || path.equals("button_loff")
                    || path.equals("button_lon");
            if (keycardReader || standardButton) return List.of();
        }
        return ContextInteractionRegistry.getBlockRules(block);
    }
}
