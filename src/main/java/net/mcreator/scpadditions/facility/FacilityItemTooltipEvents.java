package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Contextual tooltips for functional facility items. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityItemTooltipEvents {
    private FacilityItemTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(FacilityModule.SIGN_SUPPORT.get().asItem())) {
            return;
        }
        event.getToolTip().add(Component.literal(
                        "Choose SCP information, facility notices, or custom artwork")
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal(
                        "Place it, then use a Screwdriver to edit the sign")
                .withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.literal(
                        "Custom templates are shared by the current world")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
