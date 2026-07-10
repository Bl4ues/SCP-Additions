package net.mcreator.scpadditions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scp914AssemblyKitItem extends Item {
	public Scp914AssemblyKitItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.literal("SCP-914 Assembly Kit");
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, world, tooltip, flag);
		tooltip.add(Component.literal("Places a complete 13 x 4 x 4 SCP-914 structure."));
		tooltip.add(Component.literal("Click the floor at the machine's front-center."));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		Player player = context.getPlayer();
		Direction front = context.getHorizontalDirection().getOpposite();
		BlockPos keyPos = context.getClickedPos().relative(context.getClickedFace());
		Map<BlockPos, BlockState> structure = buildStructure(keyPos, front);

		for (BlockPos pos : structure.keySet()) {
			if (!level.getBlockState(pos).isAir()) {
				if (player != null) {
					player.displayClientMessage(Component.literal("Cannot assemble SCP-914: not enough clear space."), true);
				}
				return InteractionResult.FAIL;
			}
		}

		for (Map.Entry<BlockPos, BlockState> entry : structure.entrySet()) {
			level.setBlock(entry.getKey(), entry.getValue(), 3);
		}

		if (player != null && !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		if (player != null) {
			player.displayClientMessage(Component.literal("SCP-914 assembled."), true);
		}
		return InteractionResult.SUCCESS;
	}

	private static Map<BlockPos, BlockState> buildStructure(BlockPos keyPos, Direction front) {
		Map<BlockPos, BlockState> placements = new LinkedHashMap<>();

		// 13 wide, 4 tall, 4 deep outer housing. Local Z runs into the machine.
		for (int x = -6; x <= 6; x++) {
			for (int y = 0; y <= 3; y++) {
				for (int z = -3; z <= 0; z++) {
					boolean sideWall = x == -6 || x == 6;
					boolean backWall = z == -3;
					boolean roof = y == 3;
					if (sideWall || backWall || roof) {
						put(placements, keyPos, front, x, y, z, ScpAdditionsModBlocks.SCP_914BLOCK.get(), front);
					}
				}
			}
		}

		// Intake/output chambers and door frames.
		put(placements, keyPos, front, -5, 1, 0, ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR.get(), front);
		put(placements, keyPos, front, 5, 1, 0, ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR.get(), front);
		put(placements, keyPos, front, -3, 1, 0, ScpAdditionsModBlocks.SCP_914_INTAKE.get(), front);
		put(placements, keyPos, front, 3, 1, 0, ScpAdditionsModBlocks.SCP_914_OUTPUT.get(), front);

		// Main clockworks face.
		put(placements, keyPos, front, -1, 1, 0, ScpAdditionsModBlocks.SCP_914CLOCKWORKS.get(), front);
		put(placements, keyPos, front, 0, 1, 0, ScpAdditionsModBlocks.SCP_914BODY.get(), front);
		put(placements, keyPos, front, 0, 2, 0, ScpAdditionsModBlocks.SCP_914DIAL_1TO_1.get(), front);

		// The key block is the functional origin used by the processor offsets.
		put(placements, keyPos, front, 0, 1, -1, ScpAdditionsModBlocks.SCP_914_KEY_WIND.get(), front);

		return placements;
	}

	private static void put(Map<BlockPos, BlockState> placements, BlockPos origin, Direction front, int x, int y, int z, Block block, Direction facing) {
		placements.put(toWorld(origin, front, x, y, z), withFacing(block.defaultBlockState(), facing));
	}

	private static BlockState withFacing(BlockState state, Direction facing) {
		if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
			return state.setValue(HorizontalDirectionalBlock.FACING, facing);
		}
		return state;
	}

	private static BlockPos toWorld(BlockPos origin, Direction front, int x, int y, int z) {
		Direction rightFromViewer = front.getCounterClockWise();
		Vec3i right = rightFromViewer.getNormal();
		Vec3i forward = front.getNormal();
		return origin.offset(
				right.getX() * x + forward.getX() * z,
				y,
				right.getZ() * x + forward.getZ() * z);
	}
}
