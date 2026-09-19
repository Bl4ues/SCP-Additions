package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Physical Surface input uses the parametric grid and distinguishes a wall's
 * structural payload from extra blocks mounted onto its visible faces.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformSurfacePlacementClient {
    private TransformSurfacePlacementClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Selection selection = TransformConstructionClientState.selection();
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        TransformSurfaceRaycast.Target aimed;
        if (selection != null && selection.type() == SelectionType.SURFACE) {
            ConstructionSurface selectedSurface =
                    TransformConstructionClientState.surface(selection.id());
            aimed = selectedSurface == null ? null
                    : TransformSurfaceRaycast.target(player, selectedSurface);
        } else {
            aimed = TransformSurfaceRaycast.target(player,
                    TransformConstructionClientState.surfaces(
                            minecraft.level.dimension().location()));
        }

        // An old Surface selection must never steal a right-click on a nearer
        // Off-Grid cell after switching to a block in the hotbar. Resolve both
        // logical grids along the same eye ray; do not compare vanilla proxy
        // BlockPos or the misleading axis-aligned technical selection box.
        if (selection != null && selection.type() == SelectionType.SURFACE
                && player.isCreative()
                && player.getMainHandItem().getItem() instanceof BlockItem) {
            TransformGroupPlacementClient.Target groupTarget =
                    TransformGroupPlacementClient.findTarget(player);
            if (groupTarget != null && (aimed == null
                    || player.getEyePosition().distanceTo(groupTarget.worldHit())
                            + 0.025D < aimed.distance())) {
                TransformConstructionNetwork.placeGroupBlock(
                        groupTarget.group().id(), groupTarget.source(),
                        groupTarget.adjacentCell(), groupTarget.face(),
                        groupTarget.worldHit());
                event.setCanceled(true);
                return;
            }
        }

        if (!player.isShiftKeyDown() && aimed != null) {
            if (aimed.layer().overlay()) {
                ConstructionSurface.SurfaceAttachment overlay =
                        aimed.surface().overlay(aimed.slot(),
                                aimed.normalSign());
                if (overlay != null && interactive(overlay.state())) {
                    TransformConstructionNetwork.useSurfaceOverlay(
                            aimed.surface().id(), aimed.slot(),
                            aimed.normalSign());
                    event.setCanceled(true);
                    return;
                }
            } else {
                ConstructionSurface.SurfaceAttachment attachment =
                        aimed.surface().attachments().get(aimed.slot());
                if (attachment != null && interactive(attachment.state())) {
                    TransformConstructionNetwork.useSurfaceSlot(
                            aimed.surface().id(), aimed.slot());
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            if (!player.isCreative() || aimed == null) return;
            boolean mainOccupied =
                    aimed.surface().attachments().containsKey(aimed.slot());
            if (aimed.layer().overlay() || mainOccupied) {
                // Structural blocks stay on the authored surface. Only an
                // EXTRA block chooses the side physically clicked by the user.
                int side = aimed.layer().overlay()
                        ? aimed.normalSign()
                        : clickedSide(player, aimed.surface(), aimed.slot());
                TransformConstructionNetwork.placeSurfaceOverlay(
                        aimed.surface().id(), aimed.slot(), side, aimed.hit());
            } else {
                TransformConstructionNetwork.placeSurfaceBlock(
                        aimed.surface().id(), aimed.slot(), aimed.hit());
            }
            event.setCanceled(true);
            return;
        }

        if (player.getMainHandItem().is(TransformConstructionModule.getSurfaceTool())
                || player.getMainHandItem().is(
                        TransformConstructionModule.getOffGridTool())) {
            return;
        }
        if (aimed == null) return;
        if (aimed.layer().overlay()) {
            ConstructionSurface.SurfaceAttachment overlay =
                    aimed.surface().overlay(aimed.slot(),
                            aimed.normalSign());
            if (overlay == null || !interactive(overlay.state())) return;
            TransformConstructionNetwork.useSurfaceOverlay(
                    aimed.surface().id(), aimed.slot(),
                    aimed.normalSign());
        } else {
            ConstructionSurface.SurfaceAttachment attachment =
                    aimed.surface().attachments().get(aimed.slot());
            if (attachment == null || !interactive(attachment.state())) return;
            TransformConstructionNetwork.useSurfaceSlot(
                    aimed.surface().id(), aimed.slot());
        }
        event.setCanceled(true);
    }

    private static int clickedSide(LocalPlayer player,
            ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 plane = surface.gridPoint(u, v);
        Vec3 normal = surface.gridNormal(u, v);
        return player.getEyePosition().subtract(plane).dot(normal) < 0.0D
                ? -1 : 1;
    }

    private static boolean interactive(BlockState state) {
        return state != null && (state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || TransformWallFixturePlacement.isDoorButton(state)
                || FacilityModule.isFacilityDoor(state));
    }
}
