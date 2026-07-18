package net.mcreator.scpadditions.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Public SCP-714 jade ring item. Its equipped effects are handled centrally. */
public final class Scp714Item extends Item {
    public Scp714Item() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.scp_additions.scp_714.exhaustion")
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.scp_additions.scp_714.protection")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scp_additions.scp_714.slot")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
