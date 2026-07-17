package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes the adaptive SL1 Corner Wall Detail follow the horizontal face used
 * for placement, matching the placement language used by readers and buttons.
 * Vertical stacks still inherit the facing of their neighboring segment.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CornerWallDetailPlacementEvents {
    private static final Map<UUID, PendingPlacement> PENDING = new ConcurrentHashMap<>();

    private CornerWallDetailPlacementEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Client and off-hand copies of the interaction run in the same JVM in
        // singleplayer. Letting either mutate this map could erase the main-hand
        // server input before EntityPlaceEvent consumed it.
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getItemStack().is(UBlocksModule.SL_1_WALL_DETAIL_1_BOT.get().asItem())) {
            return;
        }

        Direction face = event.getFace();
        if (face == null || !face.getAxis().isHorizontal()) {
            PENDING.remove(event.getEntity().getUUID());
            return;
        }

        PENDING.put(event.getEntity().getUUID(), new PendingPlacement(event.getPos(), face));
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getPlacedBlock().getBlock() != UBlocksModule.SL_1_WALL_DETAIL_1_BOT.get()) {
            return;
        }

        PendingPlacement pending = PENDING.remove(player.getUUID());
        if (pending == null) return;

        BlockPos pos = event.getPos();
        BlockPos expectedPos = pending.clickedPos().relative(pending.face());
        if (!pos.equals(expectedPos) && !pos.equals(pending.clickedPos())) return;

        LevelAccessor level = event.getLevel();
        BlockState placed = level.getBlockState(pos);
        if (placed.getBlock() != UBlocksModule.SL_1_WALL_DETAIL_1_BOT.get()
                || !placed.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        Direction facing = pending.face();
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        if (isCornerDetail(below)) {
            facing = below.getValue(HorizontalDirectionalBlock.FACING);
        } else if (isCornerDetail(above)) {
            facing = above.getValue(HorizontalDirectionalBlock.FACING);
        }

        if (placed.getValue(HorizontalDirectionalBlock.FACING) != facing) {
            level.setBlock(pos,
                    placed.setValue(HorizontalDirectionalBlock.FACING, facing),
                    Block.UPDATE_ALL);
        }
    }

    private static boolean isCornerDetail(BlockState state) {
        return state.getBlock() == UBlocksModule.SL_1_WALL_DETAIL_1_BOT.get()
                && state.hasProperty(HorizontalDirectionalBlock.FACING);
    }

    private record PendingPlacement(BlockPos clickedPos, Direction face) {
    }
}
