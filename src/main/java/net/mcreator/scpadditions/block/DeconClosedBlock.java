
package net.mcreator.scpadditions.block;

import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Rotation;
import net.minecraft.util.Mirror;
import net.minecraft.util.Direction;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.StateContainer;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.BooleanProperty;
import net.minecraft.loot.LootContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.BlockItem;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.FluidState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;

import net.mcreator.scpadditions.procedures.DeconClosedBlockAddedProcedure;
import net.mcreator.scpadditions.ScpAdditionsModElements;

import java.util.stream.Stream;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.AbstractMap;

@ScpAdditionsModElements.ModElement.Tag
public class DeconClosedBlock extends ScpAdditionsModElements.ModElement {
	@ObjectHolder("scp_additions:decon_closed")
	public static final Block block = null;

	public DeconClosedBlock(ScpAdditionsModElements instance) {
		super(instance, 196);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new CustomBlock());
		elements.items.add(() -> new BlockItem(block, new Item.Properties().group(null)).setRegistryName(block.getRegistryName()));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientLoad(FMLClientSetupEvent event) {
		RenderTypeLookup.setRenderLayer(block, RenderType.getTranslucent());
	}

	public static class CustomBlock extends Block implements IWaterLoggable {
		public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;
		public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

		public CustomBlock() {
			super(Block.Properties.create(Material.IRON).sound(SoundType.METAL).hardnessAndResistance(20f, 30f).setLightLevel(s -> 6).harvestLevel(1)
					.harvestTool(ToolType.PICKAXE).setRequiresTool().notSolid().setOpaque((bs, br, bp) -> false));
			this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
			setRegistryName("decon_closed");
		}

		@Override
		public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
			return state.getFluidState().isEmpty();
		}

		@Override
		public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
			return 0;
		}

		@Override
		public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
			Vector3d offset = state.getOffset(world, pos);
			switch ((Direction) state.get(FACING)) {
				case SOUTH :
				default :
					return VoxelShapes
							.or(makeCuboidShape(-16, -16, 32, -13, 32, -16), makeCuboidShape(-16, 32, -16, 16, 19, -13),
									makeCuboidShape(16, 32, -16, 13, -16, 32), makeCuboidShape(15, 19, 32, -16, 32, 29),
									makeCuboidShape(-16, -16, 32, -8, 19, 29), makeCuboidShape(16, 19, 29, 10, -16, 32),
									makeCuboidShape(16, -16, -16, 10, 19, -13), makeCuboidShape(-8, 19, -13, -16, -16, -16),
									makeCuboidShape(-8, -16, -16, 10, 19, -13), makeCuboidShape(-8, 19, 29, 10, -16, 32),
									makeCuboidShape(16, 19, -16, 10, -16, -13), makeCuboidShape(-16, -16, -16, -8, 19, -13),
									makeCuboidShape(-16, 19, 32, -8, -16, 29), makeCuboidShape(-14, 32, 32, 0, 30, -16))

							.withOffset(offset.x, offset.y, offset.z);
				case NORTH :
					return VoxelShapes
							.or(makeCuboidShape(32, -16, -16, 29, 32, 32), makeCuboidShape(32, 32, 32, 0, 19, 29),
									makeCuboidShape(0, 32, 32, 3, -16, -16), makeCuboidShape(1, 19, -16, 32, 32, -13),
									makeCuboidShape(32, -16, -16, 24, 19, -13), makeCuboidShape(0, 19, -13, 6, -16, -16),
									makeCuboidShape(0, -16, 32, 6, 19, 29), makeCuboidShape(24, 19, 29, 32, -16, 32),
									makeCuboidShape(24, -16, 32, 6, 19, 29), makeCuboidShape(24, 19, -13, 6, -16, -16),
									makeCuboidShape(0, 19, 32, 6, -16, 29), makeCuboidShape(32, -16, 32, 24, 19, 29),
									makeCuboidShape(32, 19, -16, 24, -16, -13), makeCuboidShape(30, 32, -16, 16, 30, 32))

							.withOffset(offset.x, offset.y, offset.z);
				case EAST :
					return VoxelShapes
							.or(makeCuboidShape(32, -16, 32, -16, 32, 29), makeCuboidShape(-16, 32, 32, -13, 19, 0),
									makeCuboidShape(-16, 32, 0, 32, -16, 3), makeCuboidShape(32, 19, 1, 29, 32, 32),
									makeCuboidShape(32, -16, 32, 29, 19, 24), makeCuboidShape(29, 19, 0, 32, -16, 6),
									makeCuboidShape(-16, -16, 0, -13, 19, 6), makeCuboidShape(-13, 19, 24, -16, -16, 32),
									makeCuboidShape(-16, -16, 24, -13, 19, 6), makeCuboidShape(29, 19, 24, 32, -16, 6),
									makeCuboidShape(-16, 19, 0, -13, -16, 6), makeCuboidShape(-16, -16, 32, -13, 19, 24),
									makeCuboidShape(32, 19, 32, 29, -16, 24), makeCuboidShape(32, 32, 30, -16, 30, 16))

							.withOffset(offset.x, offset.y, offset.z);
				case WEST :
					return VoxelShapes
							.or(makeCuboidShape(-16, -16, -16, 32, 32, -13), makeCuboidShape(32, 32, -16, 29, 19, 16),
									makeCuboidShape(32, 32, 16, -16, -16, 13), makeCuboidShape(-16, 19, 15, -13, 32, -16),
									makeCuboidShape(-16, -16, -16, -13, 19, -8), makeCuboidShape(-13, 19, 16, -16, -16, 10),
									makeCuboidShape(32, -16, 16, 29, 19, 10), makeCuboidShape(29, 19, -8, 32, -16, -16),
									makeCuboidShape(32, -16, -8, 29, 19, 10), makeCuboidShape(-13, 19, -8, -16, -16, 10),
									makeCuboidShape(32, 19, 16, 29, -16, 10), makeCuboidShape(32, -16, -16, 29, 19, -8),
									makeCuboidShape(-16, 19, -16, -13, -16, -8), makeCuboidShape(-16, 32, -14, 32, 30, 0))

							.withOffset(offset.x, offset.y, offset.z);
			}
		}

		@Override
		protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
			builder.add(FACING, WATERLOGGED);
		}

		@Override
		public BlockState getStateForPlacement(BlockItemUseContext context) {
			boolean flag = context.getWorld().getFluidState(context.getPos()).getFluid() == Fluids.WATER;
			return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite()).with(WATERLOGGED, flag);
		}

		public BlockState rotate(BlockState state, Rotation rot) {
			return state.with(FACING, rot.rotate(state.get(FACING)));
		}

		public BlockState mirror(BlockState state, Mirror mirrorIn) {
			return state.rotate(mirrorIn.toRotation(state.get(FACING)));
		}

		@Override
		public FluidState getFluidState(BlockState state) {
			return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
		}

		@Override
		public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos,
				BlockPos facingPos) {
			if (state.get(WATERLOGGED)) {
				world.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
			}
			return super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos);
		}

		@Override
		public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
			return new ItemStack(DeconOpenBlock.block);
		}

		@Override
		public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
			List<ItemStack> dropsOriginal = super.getDrops(state, builder);
			if (!dropsOriginal.isEmpty())
				return dropsOriginal;
			return Collections.singletonList(new ItemStack(DeconOpenBlock.block));
		}

		@Override
		public void onBlockAdded(BlockState blockstate, World world, BlockPos pos, BlockState oldState, boolean moving) {
			super.onBlockAdded(blockstate, world, pos, oldState, moving);
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();

			DeconClosedBlockAddedProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
	}
}
