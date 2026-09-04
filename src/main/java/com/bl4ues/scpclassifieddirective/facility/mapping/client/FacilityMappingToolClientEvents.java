package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Converts the mapping tool's non-destructive left click into a selection. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityMappingToolClientEvents {
    private FacilityMappingToolClientEvents() {
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(FacilityMappingItems.getTool())
                || !event.getEntity().isCreative()) return;
        FacilityMappingClientState.setSelectionStart(event.getPos());
        FacilityMappingNetwork.requestSelectionStart(event.getPos());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
