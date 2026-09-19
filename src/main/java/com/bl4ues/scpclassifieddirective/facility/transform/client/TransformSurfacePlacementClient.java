package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
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

/** Physical use and placement resolve against the nearest authored grid. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformSurfacePlacementClient {
    private static final double HIT_EPSILON = 0.025D;

    private TransformSurfacePlacementClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.isCanceled()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || holdingConstructionTool(player)) return;

        TransformSurfaceRaycast.Target surface = TransformSurfaceRaycast.target(
                player, TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));
        boolean placing = player.isCreative()
                && player.getMainHandItem().getItem() instanceof BlockItem;

        // Only the nearest target for the current action receives this click.
        // Neither the previous editor selection nor a vanilla proxy AABB owns
        // placement or interaction in an authored local grid.
        if (placing) {
            TransformGroupPlacementClient.Target group =
                    TransformGroupPlacementClient.findTarget(player);
            if (group != null && nearer(
                    player.getEyePosition().distanceTo(group.worldHit()),
                    surface)) {
                TransformConstructionNetwork.placeGroupBlock(
                        group.group().id(), group.source(),
                        group.adjacentCell(), group.face(), group.worldHit());
                event.setCanceled(true);
                return;
            }
            if (surface == null) return;
            boolean occupied = surface.surface().attachments()
                    .containsKey(surface.slot());
            int side = surface.layer().overlay()
                    ? surface.normalSign()
                    : clickedSide(player, surface.surface(), surface.slot());
            // An inside click authors an independent inner overlay, even when
            // the outside main wall has not been constructed yet.
            if (surface.layer().overlay() || occupied
                    || side != TransformSurfaceGeometry.MAIN_SIDE) {
                TransformConstructionNetwork.placeSurfaceOverlay(
                        surface.surface().id(), surface.slot(), side,
                        surface.hit());
            } else {
                TransformConstructionNetwork.placeSurfaceBlock(
                        surface.surface().id(), surface.slot(), surface.hit());
            }
            event.setCanceled(true);
            return;
        }

        if (player.isShiftKeyDown()) return;
        // Empty authoring guides do not occlude runtime equipment. Real solid
        // payloads do: a button behind an unrelated authored wall is not a
        // reachable control merely because the raycast skips non-controls.
        if (surface != null && !occupiedSurfaceTarget(surface)) {
            surface = null;
        }
        TransformGroupPlacementClient.PayloadTarget group =
                TransformGroupPlacementClient.findBreakTarget(player);
        if (group != null && nearer(group.distance(), surface)) {
            if (!interactive(group.state())) return;
            TransformConstructionNetwork.useGroupCell(
                    group.group().id(), group.cell());
            event.setCanceled(true);
            return;
        }
        if (surface == null || !interactiveSurfaceTarget(surface)) return;
        if (surface.layer().overlay()) {
            TransformConstructionNetwork.useSurfaceOverlay(
                    surface.surface().id(), surface.slot(),
                    surface.normalSign());
        } else {
            TransformConstructionNetwork.useSurfaceSlot(
                    surface.surface().id(), surface.slot());
        }
        event.setCanceled(true);
    }

    private static ConstructionSurface.SurfaceAttachment surfaceAttachment(
            TransformSurfaceRaycast.Target target) {
        return target.layer().overlay()
                ? target.surface().overlay(target.slot(), target.normalSign())
                : target.surface().attachments().get(target.slot());
    }

    private static boolean occupiedSurfaceTarget(
            TransformSurfaceRaycast.Target target) {
        ConstructionSurface.SurfaceAttachment attachment =
                surfaceAttachment(target);
        return attachment != null && !attachment.state().isAir();
    }

    private static boolean interactiveSurfaceTarget(
            TransformSurfaceRaycast.Target target) {
        ConstructionSurface.SurfaceAttachment attachment =
                surfaceAttachment(target);
        return attachment != null && interactive(attachment.state());
    }

    private static boolean nearer(double groupDistance,
            TransformSurfaceRaycast.Target surfaceTarget) {
        return surfaceTarget == null
                || groupDistance + HIT_EPSILON < surfaceTarget.distance();
    }

    private static boolean holdingConstructionTool(LocalPlayer player) {
        return player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                || player.getMainHandItem().is(
                        TransformConstructionModule.getOffGridTool());
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
                || KeycardReaderLevels.describe(state) != null
                || FacilityModule.isFacilityDoor(state));
    }
}
