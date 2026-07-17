package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Places the adaptive SL1 Corner Wall Detail in the exact side or corner
 * selected by the player. Vertical stacks still inherit the facing of their
 * neighboring segment so bottom/middle/top columns remain aligned.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CornerWallDetailPlacementEvents {
    private static final Map<UUID, PendingPlacement> PENDING = new ConcurrentHashMap<>();

    private CornerWallDetailPlacementEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Client and off-hand copies of the interaction run in the same JVM in
        // singleplayer. Only the main-hand server event owns placement state.
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getItemStack().is(UBlocksModule.SL_1_WALL_DETAIL_1_BOT.get().asItem())) {
            return;
        }

        Direction face = event.getFace();
        if (face == null) {
            PENDING.remove(event.getEntity().getUUID());
            return;
        }

        Vec3 hit = event.getHitVec().getLocation();
        BlockPos clickedPos = event.getPos();
        double localX = clampLocal(hit.x - clickedPos.getX());
        double localZ = clampLocal(hit.z - clickedPos.getZ());
        Direction facing = resolveFacing(face, localX, localZ);

        PENDING.put(event.getEntity().getUUID(),
                new PendingPlacement(clickedPos, face, facing));
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

        Direction facing = pending.facing();
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

    /**
     * A horizontal face has two valid corners. Clicking the right half keeps the
     * model's natural orientation for that face; clicking the left half rotates
     * it 90 degrees counter-clockwise. Top and bottom faces select one of all
     * four corners directly from the clicked quadrant.
     */
    private static Direction resolveFacing(Direction face, double localX, double localZ) {
        if (face.getAxis().isHorizontal()) {
            boolean rightHalf = switch (face) {
                case NORTH -> localX < 0.5D;
                case SOUTH -> localX >= 0.5D;
                case EAST -> localZ < 0.5D;
                case WEST -> localZ >= 0.5D;
                default -> true;
            };
            return rightHalf ? face : face.getCounterClockWise();
        }

        boolean eastHalf = localX >= 0.5D;
        boolean southHalf = localZ >= 0.5D;
        if (!eastHalf && southHalf) return Direction.NORTH; // southwest corner
        if (!eastHalf) return Direction.EAST;               // northwest corner
        if (!southHalf) return Direction.SOUTH;             // northeast corner
        return Direction.WEST;                              // southeast corner
    }

    private static double clampLocal(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static boolean isCornerDetail(BlockState state) {
        return state.getBlock() == UBlocksModule.SL_1_WALL_DETAIL_1_BOT.get()
                && state.hasProperty(HorizontalDirectionalBlock.FACING);
    }

    private record PendingPlacement(BlockPos clickedPos, Direction face, Direction facing) {
    }
}
