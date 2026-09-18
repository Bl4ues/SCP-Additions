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

        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            if (!player.isCreative() || selection == null
                    || selection.type() != SelectionType.SURFACE) return;
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(selection.id());
            if (surface == null) return;
            TransformSurfaceRaycast.Target target =
                    TransformSurfaceRaycast.target(player, surface);
            if (target == null) return;
            TransformConstructionNetwork.placeSurfaceBlock(surface.id(),
                    target.slot(), target.hit());
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
        TransformSurfaceRaycast.Target target = TransformSurfaceRaycast.target(
                player, TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));
        if (target == null) return;
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
