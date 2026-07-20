package net.mcreator.scpadditions.item;

import com.bl4ues.scpadditions.compat.LegacyItemTags;

import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int savedLevel = LegacyItemTags.hasTag(stack)
                ? LegacyItemTags.getTag(stack).getInt(KeycardReaderInteractionEvents.SAVED_LEVEL_TAG) : 0;
        if (savedLevel >= 1 && savedLevel <= 6) {
            tooltip.add(Component.translatable("tooltip.scp_additions.screwdriver.saved_level", savedLevel)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.scp_additions.screwdriver.no_saved_level")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.scp_additions.screwdriver.configure")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.scp_additions.screwdriver.copy")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.scp_additions.screwdriver.apply")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
