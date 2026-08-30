package com.bl4ues.scpclassifieddirective.anomalous;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class Item006BlockItem extends BlockItem {
    public Item006BlockItem(Block block) {
        super(block, new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Item #006, Vol. I");
    }
}
