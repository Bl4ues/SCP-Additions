package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

/** Contextual tooltips for functional facility items. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityItemTooltipEvents {
    private FacilityItemTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(FacilityModule.SIGN_SUPPORT.get().asItem())) {
            event.getToolTip().add(Component.literal(
                            "Displays SCP information, facility notices, or custom artwork")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal(
                            "Use a Screwdriver to configure after placement")
                    .withStyle(ChatFormatting.AQUA));
        }

        if (event.getItemStack().is(
                ScpAdditionsModBlocks.TESLA_TERMINAL_BLOCK.get().asItem())) {
            event.getToolTip().add(Component.literal(
                            "Use a Screwdriver to cycle the terminal on or off")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
