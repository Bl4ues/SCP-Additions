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
import org.lwjgl.glfw.GLFW;


/** Restores vanilla middle-click copy semantics for transformed proxy blocks. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformPickBlockClient {
    private TransformPickBlockClient() {
    }

    /**
     * Forge's interaction-key event can be skipped when vanilla reports MISS
     * for a visual Off-Grid/Surface block (there is no vanilla target block).
     * Handle the physical middle click before vanilla's target-dependent path.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMiddleClick(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || !minecraft.options.keyPickItem.matchesMouse(event.getButton())) {
            return;
        }
        if (pickTransformedBlock(minecraft)) event.setCanceled(true);
    }

    /** Also support a remapped pick-block key when Forge dispatches it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;
        if (!pickTransformedBlock(Minecraft.getInstance())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static boolean pickTransformedBlock(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null
                || minecraft.screen != null || !player.isCreative()) return false;

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
                            : surface.surface().attachments().get(surface.slot());
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
        if (state == null || state.isAir()) return false;
        ItemStack picked = FacilityPipeModule.pick(state);
        if (picked.isEmpty()) picked = state.getBlock().asItem().getDefaultInstance();
        if (picked.isEmpty()) return false;

        player.getInventory().setPickedItem(picked);
        minecraft.gameMode.handleCreativeModeItemAdd(
                player.getInventory().getSelected(),
                36 + player.getInventory().selected);
        return true;
    }
}
