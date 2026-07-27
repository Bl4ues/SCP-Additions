package net.mcreator.scpadditions.init;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CyclingCreativeTabIconRenderer
        extends BlockEntityWithoutLevelRenderer {
    private final CyclingCreativeTabIconItem iconItem;

    public CyclingCreativeTabIconRenderer(CyclingCreativeTabIconItem iconItem) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
        this.iconItem = iconItem;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        ItemStack display = iconItem.currentDisplayStack();
        if (display.isEmpty() || display.is(iconItem)) return;

        Minecraft.getInstance().getItemRenderer().renderStatic(display,
                context, packedLight, packedOverlay, poseStack, buffer,
                null, 0);
    }
}
