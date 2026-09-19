package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Creative breaking against transformed logical grids.
 *
 * The vanilla BlockPos under the crosshair may belong to an unrelated wall or
 * may only be a proxy bridge. Resolve the authored local cell/slot and remove
 * that address directly, leaving the vanilla block behind untouched.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformLogicalBreakClient {
    private static boolean attackLatch;

    private TransformLogicalBreakClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || attackLatch) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isCreative() || holdingEditorTool(player)) {
            return;
        }

        TransformGroupPlacementClient.PayloadTarget group =
                TransformGroupPlacementClient.findBreakTarget(player);
        TransformSurfaceRaycast.Target surface =
                TransformSurfaceRaycast.target(player,
                        TransformConstructionClientState.surfaces(
                                minecraft.level.dimension().location()));
        if (surface != null) {
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.layer().overlay()
                            ? surface.surface().overlay(surface.slot(),
                                    surface.normalSign())
                            : surface.surface().attachments().get(
                                    surface.slot());
            if (attachment == null || attachment.state().isAir()) {
                surface = null;
            }
        }

        double groupDistance = group == null
                ? Double.POSITIVE_INFINITY : group.distance();
        double surfaceDistance = surface == null
                ? Double.POSITIVE_INFINITY : surface.distance();
        if (!Double.isFinite(groupDistance)
                && !Double.isFinite(surfaceDistance)) return;

        if (groupDistance <= surfaceDistance) {
            TransformConstructionNetwork.breakGroupCell(
                    group.group().id(), group.cell());
        } else if (surface.layer().overlay()) {
            TransformConstructionNetwork.breakSurfaceOverlay(
                    surface.surface().id(), surface.slot(),
                    surface.normalSign());
        } else {
            TransformConstructionNetwork.breakSurfaceSlot(
                    surface.surface().id(), surface.slot());
        }
        attackLatch = true;
        event.setCanceled(true);
        event.setSwingHand(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Minecraft.getInstance().options.keyAttack.isDown()) {
            attackLatch = false;
        }
    }

    private static boolean holdingEditorTool(LocalPlayer player) {
        return player.getMainHandItem().is(
                    TransformConstructionModule.getOffGridTool())
                || player.getOffhandItem().is(
                    TransformConstructionModule.getOffGridTool())
                || player.getMainHandItem().is(
                    TransformConstructionModule.getSurfaceTool())
                || player.getOffhandItem().is(
                    TransformConstructionModule.getSurfaceTool());
    }
}
