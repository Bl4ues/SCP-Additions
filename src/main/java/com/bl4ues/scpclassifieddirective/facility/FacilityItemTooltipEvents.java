package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Contextual tooltips for functional facility items. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
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
            return;
        }
        if (event.getItemStack().is(HazardSignModule.ITEM.get())) {
            event.getToolTip().add(Component.literal(
                            "Displays one selectable Anomaly Trait pictogram as a facility hazard warning")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal(
                            "Choose the pictogram when placed; use a Screwdriver to edit it later")
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
