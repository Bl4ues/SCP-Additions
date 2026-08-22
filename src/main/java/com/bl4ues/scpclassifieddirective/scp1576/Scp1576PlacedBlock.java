package com.bl4ues.scpclassifieddirective.scp1576;

import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.sound.InventoryInteractionSoundFeedback;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Physical placed form of SCP-1576. The exact item stack remains inside it. */
public final class Scp1576PlacedBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Match the communicator's main wooden body rather than the horn, crank and
    // other thin authored details. The model body is about 6.2 x 3.2 x 4.2 px.
    private static final VoxelShape NORTH_SOUTH = box(4.9D, 0.0D, 5.9D,
            11.1D, 3.25D, 10.1D);
    private static final VoxelShape EAST_WEST = box(5.9D, 0.0D, 4.9D,
            10.1D, 3.25D, 11.1D);

    public Scp1576PlacedBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.WOOD)
                .strength(0.5F)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scp1576PlacedBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return bodyShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return bodyShape(state);
    }

    private static VoxelShape bodyShape(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? EAST_WEST : NORTH_SOUTH;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Scp1576PlacedBlockEntity placed)
                || placed.getStoredItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stored = placed.getStoredItem().copy();
        if (!giveToPlayer(serverPlayer, stored)) {
            return InteractionResult.FAIL;
        }

        placed.clearContent();
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    private static boolean giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Placed communicators from worlds created before the placement cleanup
        // can still contain the temporary hotbar-mirror flag. Strip only SCP
        // Inventory transport metadata; the communicator's active/cooldown NBT
        // is deliberately preserved so taking it does not reset its state.
        ItemStack pickup = stack.copy();
        ScpPickupRouter.stripUsableSession(pickup);
        ScpPickupRouter.stripNoMergeMarker(pickup);

        if (ScpClassifiedDirectiveModulesConfig.get().inventory.enabled
                && !player.isCreative() && !player.isSpectator()) {
            IScpInventory inventory = player.getCapability(
                    ScpInventoryCapability.INSTANCE).resolve().orElse(null);
            if (inventory == null) return false;
            int accepted = ScpPickupRouter.accept(inventory, player, pickup.copy());
            ModNetwork.syncTo(player, inventory);
            if (accepted < pickup.getCount()) {
                ModNetwork.showInventoryFull(player);
                return false;
            }
            InventoryInteractionSoundFeedback.pickup(player);
            return true;
        }

        return player.addItem(pickup.copy());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Scp1576PlacedBlockEntity placed) {
                Containers.dropContents(level, pos, placed);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
