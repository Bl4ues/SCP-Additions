package net.mcreator.scpadditions.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.FacilityModule;

/** Adds the shared decorative-only description to legacy prop block items. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityDecorativeTooltipClient {
    private FacilityDecorativeTooltipClient() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        if (item != FacilityModule.WALLLIGHT.get().asItem()
                && item != FacilityModule.WALLLIGHT_2.get().asItem()
                && item != FacilityModule.TV.get().asItem()) {
            return;
        }

        Component tooltip = Component.translatable(
                "tooltip.scp_additions.decorative_prop")
                .withStyle(ChatFormatting.GRAY);
        boolean alreadyPresent = event.getToolTip().stream()
                .anyMatch(line -> line.getString().equals(tooltip.getString()));
        if (!alreadyPresent) event.getToolTip().add(tooltip);
    }
}
