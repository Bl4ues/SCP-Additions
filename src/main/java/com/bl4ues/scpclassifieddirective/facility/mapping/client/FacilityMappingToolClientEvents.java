package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Converts the mapping tool's non-destructive left click into a selection. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FacilityMappingToolClientEvents {
    private static boolean attackLatch;

    private FacilityMappingToolClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isCreative()
                || !player.getMainHandItem().is(FacilityMappingItems.getTool())
                && !player.getOffhandItem().is(FacilityMappingItems.getTool())) {
            return;
        }
        event.setCanceled(true);
        if (attackLatch) return;
        attackLatch = true;
        if (FacilityMappingGeometryEditorClient.isEditing()) {
            FacilityMappingGeometryEditorClient.selectVertexUnderCrosshair();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            handleBlockClick(hit.getBlockPos());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyAttack.isDown()) attackLatch = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(FacilityMappingItems.getTool())
                || !event.getEntity().isCreative()) return;

        if (!attackLatch) {
            attackLatch = true;
            if (FacilityMappingGeometryEditorClient.isEditing()) {
                FacilityMappingGeometryEditorClient.selectVertexUnderCrosshair();
            } else {
                handleBlockClick(event.getPos());
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
    private static void handleBlockClick(net.minecraft.core.BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) return;
        var dimension = minecraft.level.dimension().location();
        if (FacilityMappingClientState.cameraLinkSelection() != null) {
            FacilityMappingNetwork.requestCameraLinkAssign(pos);
        } else if (FacilityMappingClientState.cameraAt(dimension, pos) != null) {
            FacilityMappingNetwork.requestCameraLinkStart(pos);
        } else {
            FacilityMappingClientState.setSelectionStart(pos);
            FacilityMappingNetwork.requestSelectionStart(pos);
        }
    }

}
