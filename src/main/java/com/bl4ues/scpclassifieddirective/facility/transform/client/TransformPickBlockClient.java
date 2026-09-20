package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityPipeModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** Restores vanilla middle-click copy semantics for transformed proxy blocks. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformPickBlockClient {
    private TransformPickBlockClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null
                || minecraft.screen != null || !player.isCreative()) return;

        TransformGroupPlacementClient.PayloadTarget group =
                TransformGroupPlacementClient.findBreakTarget(player);
        TransformSurfaceRaycast.Target surface = TransformSurfaceRaycast.occupiedTarget(
                player, TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));

        BlockState groupState = group == null ? null : group.state();
        BlockState surfaceState = null;
        if (surface != null) {
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.layer().overlay()
                            ? surface.surface().overlay(surface.slot(),
                                    surface.normalSign())
                            : surface.surface().attachments().get(
                                    surface.slot());
            if (attachment != null && !attachment.state().isAir()) {
                surfaceState = attachment.state();
            } else {
                surface = null;
            }
        }

        double groupDistance = group == null
                ? Double.POSITIVE_INFINITY : group.distance();
        double surfaceDistance = surface == null
                ? Double.POSITIVE_INFINITY : surface.distance();
        BlockState state = groupDistance <= surfaceDistance
                ? groupState : surfaceState;
        if (state == null || state.isAir()) return;
        ItemStack picked = FacilityPipeModule.pick(state);
        if (picked.isEmpty()) {
            picked = state.getBlock().asItem().getDefaultInstance();
        }
        if (picked.isEmpty()) return;

        player.getInventory().setPickedItem(picked);
        minecraft.gameMode.handleCreativeModeItemAdd(
                player.getInventory().getSelected(),
                36 + player.getInventory().selected);
        event.setCanceled(true);
        event.setSwingHand(false);
    }


}
