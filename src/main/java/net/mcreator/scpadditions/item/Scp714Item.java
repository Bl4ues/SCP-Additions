package net.mcreator.scpadditions.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Public SCP-714 jade ring item. Its equipped effects are handled centrally. */
public final class Scp714Item extends Item {
    private static final Component SUBTITLE = Component.literal("The Jaded Ring");

    public Scp714Item() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(SUBTITLE);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
