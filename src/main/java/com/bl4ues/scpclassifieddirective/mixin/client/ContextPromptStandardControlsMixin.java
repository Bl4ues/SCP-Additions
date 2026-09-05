package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps ordinary facility buttons/readers as hand-only contextual prompts. */
@Mixin(ContextPromptClient.class)
public abstract class ContextPromptStandardControlsMixin {
    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/inventory/client/ContextPromptClient;drawScaledString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;Ljava/lang/String;IIFI)V"),
            remap = false)
    private static void scpclassifieddirective$hideStandardControlText(
            GuiGraphics graphics, Minecraft minecraft, String text,
            int x, int y, float scale, int color) {
        if (isStandardControlUnderCrosshair(minecraft)) return;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, ScpFonts.roboto(text),
                0, 0, color, true);
        pose.popPose();
    }

    private static boolean isStandardControlUnderCrosshair(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        Block block = minecraft.level.getBlockState(hit.getBlockPos()).getBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !"scp_classified_directive".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.startsWith("button_") || path.contains("reader");
    }
}
