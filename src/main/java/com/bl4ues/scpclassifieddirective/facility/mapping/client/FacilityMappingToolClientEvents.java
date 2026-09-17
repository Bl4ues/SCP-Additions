package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Converts the mapping tool's non-destructive left click into a selection. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityMappingToolClientEvents {
    private FacilityMappingToolClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || !FacilityMappingGeometryEditorClient.isEditing()) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isCreative()
                || !player.getMainHandItem().is(FacilityMappingItems.getTool())
                && !player.getOffhandItem().is(FacilityMappingItems.getTool())) {
            return;
        }
        FacilityMappingGeometryEditorClient.selectVertexUnderCrosshair();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(FacilityMappingItems.getTool())
                || !event.getEntity().isCreative()) return;

        if (FacilityMappingGeometryEditorClient.isEditing()) {
            FacilityMappingGeometryEditorClient.selectVertexUnderCrosshair();
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        var dimension = event.getLevel().dimension().location();
        if (FacilityMappingClientState.cameraLinkSelection() != null) {
            FacilityMappingNetwork.requestCameraLinkAssign(event.getPos());
        } else if (FacilityMappingClientState.cameraAt(
                dimension, event.getPos()) != null) {
            FacilityMappingNetwork.requestCameraLinkStart(event.getPos());
        } else {
            FacilityMappingClientState.setSelectionStart(event.getPos());
            FacilityMappingNetwork.requestSelectionStart(event.getPos());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
