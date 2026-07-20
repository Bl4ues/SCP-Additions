package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * The original Unity TV and wall-sign support models are authored one block
 * below their logical anchor. Shift their placement target upward so the visible
 * object appears on the block face the player actually selected.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaisedFacilityPlacementEvents {
    private RaisedFacilityPlacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        if (!stack.is(FacilityModule.itemByPath("sign_support").get())
                && !stack.is(FacilityModule.itemByPath("tv").get())) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockHitResult hit = event.getHitVec();
        BlockPlaceContext original = new BlockPlaceContext(
                new UseOnContext(player, event.getHand(), hit));
        BlockPos raisedTarget = original.getClickedPos().above();

        Vec3 raisedLocation = hit.getLocation().add(0.0D, 1.0D, 0.0D);
        BlockHitResult raisedHit = new BlockHitResult(
                raisedLocation,
                hit.getDirection(),
                raisedTarget,
                hit.isInside());
        BlockPlaceContext raisedContext = new BlockPlaceContext(
                player.level(),
                player,
                event.getHand(),
                stack,
                raisedHit);

        if (!raisedContext.getClickedPos().equals(raisedTarget)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        InteractionResult result = blockItem.place(raisedContext);
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
