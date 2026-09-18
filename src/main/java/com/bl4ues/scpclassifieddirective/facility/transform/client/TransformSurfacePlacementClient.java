package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lets empty authored surfaces remain virtual while still accepting block
 * placement. The ray is tested against each local surface cell instead of
 * relying on a vanilla proxy BlockPos.
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

        TransformSurfaceRaycast.Target aimed = null;
        if (selection != null && selection.type() == SelectionType.SURFACE) {
            ConstructionSurface selectedSurface =
                    TransformConstructionClientState.surface(selection.id());
            if (selectedSurface != null) {
                aimed = TransformSurfaceRaycast.target(player, selectedSurface);
            }
        } else {
            aimed = TransformSurfaceRaycast.target(player,
                    TransformConstructionClientState.surfaces(
                            minecraft.level.dimension().location()));
        }

        if (!player.isShiftKeyDown() && aimed != null) {
            ConstructionSurface.SurfaceAttachment overlay =
                    aimed.surface().overlay(aimed.slot(), aimed.normalSign());
            if (overlay != null && interactive(overlay.state())) {
                TransformConstructionNetwork.useSurfaceOverlay(
                        aimed.surface().id(), aimed.slot(), aimed.normalSign());
                event.setCanceled(true);
                return;
            }
            ConstructionSurface.SurfaceAttachment attachment =
                    aimed.surface().attachments().get(aimed.slot());
            if (attachment != null && interactive(attachment.state())) {
                TransformConstructionNetwork.useSurfaceSlot(
                        aimed.surface().id(), aimed.slot());
                event.setCanceled(true);
                return;
            }
        }

        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            if (!player.isCreative()) return;
            TransformSurfaceRaycast.Target target = aimed;
            if (target == null) return;
            if (target.surface().attachments().containsKey(target.slot())) {
                TransformConstructionNetwork.placeSurfaceOverlay(
                        target.surface().id(), target.slot(),
                        target.normalSign(), target.hit());
            } else {
                TransformConstructionNetwork.placeSurfaceBlock(
                        target.surface().id(), target.slot(), target.hit());
            }
            event.setCanceled(true);
            return;
        }

        // Runtime use resolves the authored parametric slot directly. The
        // proxy/VoxelShape behind a curved wall is only a collision bridge and
        // must not decide which button, lever or door the player meant to use.
        if (player.getMainHandItem().is(TransformConstructionModule.getSurfaceTool())
                || player.getMainHandItem().is(
                        TransformConstructionModule.getOffGridTool())) {
            return;
        }
        TransformSurfaceRaycast.Target target = aimed;
        if (target == null) return;
        ConstructionSurface.SurfaceAttachment overlay =
                target.surface().overlay(target.slot(), target.normalSign());
        if (overlay != null && interactive(overlay.state())) {
            TransformConstructionNetwork.useSurfaceOverlay(
                    target.surface().id(), target.slot(), target.normalSign());
            event.setCanceled(true);
            return;
        }
        ConstructionSurface.SurfaceAttachment attachment =
                target.surface().attachments().get(target.slot());
        if (attachment == null || !interactive(attachment.state())) return;
        TransformConstructionNetwork.useSurfaceSlot(target.surface().id(),
                target.slot());
        event.setCanceled(true);
    }
    private static boolean interactive(BlockState state) {
        return state != null && (state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || FacilityModule.isFacilityDoor(state));
    }

}
