package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public final class FacilitySignBlockItem extends BlockItem {
    private final FacilitySignBlock.SignType type;

    public FacilitySignBlockItem(Block block, Properties properties,
            FacilitySignBlock.SignType type) {
        super(block, properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String prefix = type == FacilitySignBlock.SignType.CORE_ROOM
                ? "core_room_sign" : "door_sign";
        tooltip.add(Component.translatable(
                "tooltip.scp_additions." + prefix + "_primary")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.scp_additions." + prefix + "_secondary")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
