package com.bl4ues.scpclassifieddirective.block;

import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import com.bl4ues.scpclassifieddirective.procedures.Scp294restockProcedure;
import com.bl4ues.scpclassifieddirective.procedures.Scp294BlockAddedProcedure;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.integration.PlayerCurrencyAccess;

import java.util.List;
import java.util.Collections;

public class Scp294Block extends Block implements EntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public static final String COIN_INTERACTION_KEY = "scp_294_coin";
	public static final String KEYBOARD_INTERACTION_KEY = "scp_294_keyboard";

	// Authored control centers in the NORTH-facing vanilla block model. The
	// keyboard is the slanted 5..14 x 19.7..25.7 element. The payment control
	// center is derived from the measured 1.4..3.9 x 21.6..25.6 x -0.1..0 box.
	public static final double COIN_ANCHOR_X = 2.65D / 16.0D;
	public static final double COIN_ANCHOR_Y = 23.60D / 16.0D;
	public static final double COIN_ANCHOR_Z = -0.05D / 16.0D;
	public static final double KEYBOARD_ANCHOR_X = 9.50D / 16.0D;
	public static final double KEYBOARD_ANCHOR_Y = 22.70D / 16.0D;
	public static final double KEYBOARD_ANCHOR_Z = -0.015D;

	private static final double COIN_HIT_RADIUS_SQR = 0.26D * 0.26D;
	private static final double KEYBOARD_HIT_RADIUS_SQR = 0.34D * 0.34D;

	public Scp294Block() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(40f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, BlockGetter world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("The Coffee Machine"));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			default -> box(0, 0, 0, 16, 32, 16);
			case NORTH -> box(0, 0, 0, 16, 32, 16);
			case EAST -> box(0, 0, 0, 16, 32, 16);
			case WEST -> box(0, 0, 0, 16, 32, 16);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
		if (player.getInventory().getSelected().getItem() instanceof PickaxeItem tieredItem)
			return tieredItem.getTier().getLevel() >= 1;
		return false;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		world.scheduleTick(pos, this, 20);
		Scp294BlockAddedProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		Scp294restockProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		world.scheduleTick(pos, this, 20);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof Scp294BlockEntity machine)) {
			return InteractionResult.PASS;
		}

		Direction facing = state.getValue(FACING);
		Vec3 location = hit.getLocation();
		if (location.distanceToSqr(coinAnchor(pos, facing)) <= COIN_HIT_RADIUS_SQR) {
			if (world.isClientSide) return InteractionResult.SUCCESS;
			return insertCoin(world, pos, player, machine)
					? InteractionResult.CONSUME : InteractionResult.FAIL;
		}

		if (location.distanceToSqr(keyboardAnchor(pos, facing)) <= KEYBOARD_HIT_RADIUS_SQR) {
			if (!machine.getItem(0).is(ScpClassifiedDirectiveModItems.COIN.get())) {
				return InteractionResult.FAIL;
			}
			if (world.isClientSide) return InteractionResult.SUCCESS;
			if (player instanceof ServerPlayer serverPlayer) {
				NetworkHooks.openScreen(serverPlayer, machine, pos);
				return InteractionResult.CONSUME;
			}
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	private static boolean insertCoin(Level world, BlockPos pos, Player player,
			Scp294BlockEntity machine) {
		if (!machine.getItem(0).isEmpty()) return false;
		ItemStack coin = extractCoin(player);
		if (coin.isEmpty()) return false;

		machine.setItem(0, coin);
		machine.setChanged();
		BlockState state = machine.getBlockState();
		world.sendBlockUpdated(pos, state, state, 3);
		world.playSound(null, pos, ScpClassifiedDirectiveModSounds.SCP294COINSLOT.get(),
				SoundSource.NEUTRAL, 1.0F, 1.0F);
		return true;
	}

	/**
	 * SCP-294 accepts the mod coin from either inventory backend. The custom SCP
	 * inventory remains preferred when enabled, but vanilla inventory is also
	 * checked so Creative/testing and players who keep currency there still work.
	 */
	private static ItemStack extractCoin(Player player) {
		ItemStack coin = PlayerCurrencyAccess.extractOne(player,
				ScpClassifiedDirectiveModItems.COIN.get());
		if (!coin.isEmpty() || !PlayerCurrencyAccess.usesCustomInventory()) {
			return coin;
		}

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(ScpClassifiedDirectiveModItems.COIN.get())) {
				ItemStack extracted = stack.split(1);
				player.getInventory().setChanged();
				return extracted;
			}
		}
		return ItemStack.EMPTY;
	}

	public static Vec3 coinAnchor(BlockPos pos, Direction facing) {
		return localAnchor(pos, facing, COIN_ANCHOR_X, COIN_ANCHOR_Y,
				COIN_ANCHOR_Z);
	}

	public static Vec3 keyboardAnchor(BlockPos pos, Direction facing) {
		return localAnchor(pos, facing, KEYBOARD_ANCHOR_X, KEYBOARD_ANCHOR_Y,
				KEYBOARD_ANCHOR_Z);
	}

	private static Vec3 localAnchor(BlockPos pos, Direction facing,
			double x, double y, double z) {
		Vec3 local = new Vec3(x - 0.5D, y - 0.5D, z - 0.5D);
		Vec3 rotated = switch (facing) {
			case SOUTH -> new Vec3(-local.x, local.y, -local.z);
			case EAST -> new Vec3(-local.z, local.y, local.x);
			case WEST -> new Vec3(local.z, local.y, -local.x);
			default -> local;
		};
		return Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D).add(rotated);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Scp294BlockEntity(pos, state);
	}

	@Override
	public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
		super.triggerEvent(state, world, pos, eventID, eventParam);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity != null && blockEntity.triggerEvent(eventID, eventParam);
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof Scp294BlockEntity be) {
				Containers.dropContents(world, pos, be);
				world.updateNeighbourForOutputSignal(pos, this);
			}
			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
		BlockEntity tileentity = world.getBlockEntity(pos);
		if (tileentity instanceof Scp294BlockEntity be)
			return AbstractContainerMenu.getRedstoneSignalFromContainer(be);
		return 0;
	}
}
