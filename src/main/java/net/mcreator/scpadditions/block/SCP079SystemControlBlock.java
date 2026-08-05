package net.mcreator.scpadditions.block;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Global Foundation diagnostic terminal. It never grants access by redstone. */
public class SCP079SystemControlBlock extends Block
        implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;
    private static final Map<Long, Long> TERMINAL_LOOP_NEXT_TICK =
            new HashMap<>();

    public SCP079SystemControlBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(30.0F, 100.0F).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
            RandomSource random) {
        super.animateTick(state, level, pos, random);
        long key = pos.asLong();
        long gameTime = level.getGameTime();
        long nextLoop = TERMINAL_LOOP_NEXT_TICK.getOrDefault(key, 0L);
        if (gameTime < nextLoop) return;

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation("scp_additions", "terminalloop"));
        if (sound != null) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, sound, SoundSource.BLOCKS,
                    0.4F, 1.0F, false);
        }
        TERMINAL_LOOP_NEXT_TICK.put(key, gameTime + 160L);
        if (TERMINAL_LOOP_NEXT_TICK.size() > 512) {
            TERMINAL_LOOP_NEXT_TICK.clear();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel
                && player instanceof ServerPlayer serverPlayer) {
            ScpEntityNetwork.openFacilityDiagnostics(serverPlayer,
                    Scp079FacilityAccessManager.performDiagnosticScan(
                            serverPlayer), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Displays a global Foundation facility diagnostic summary."));
        tooltip.add(Component.literal(
                "Auxiliary power is required for live telemetry."));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state,
            BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level,
            BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,
            BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos())
                        .getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER,
                    Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos,
                neighborPos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }
}
