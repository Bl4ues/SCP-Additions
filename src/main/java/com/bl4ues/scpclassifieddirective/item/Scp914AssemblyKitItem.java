package com.bl4ues.scpclassifieddirective.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.bl4ues.scpclassifieddirective.facility.StructurePlacementFeedback;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Scp914AssemblyKitItem extends Item {
	private static final Direction BASE_FRONT = Direction.WEST;
	private static final int ANCHOR_X = 0;
	private static final int ANCHOR_Y = 0;
	private static final int ANCHOR_Z = 6;
	private static final int PLAYER_CLEARANCE_OFFSET = 4;
	private static final String STRUCTURE_RESOURCE = "/data/scp_classified_directive/structures/scp_914_full.nbt";

	/*
	 * The GeckoLib model reaches well beyond the legacy NBT blocks. These cells
	 * reserve the complete working envelope, including the sweep of both doors
	 * and the empty space the machine visibly occupies/needs while operating.
	 * Coordinates are relative to the NBT anchor used by toWorld().
	 */
	private static final int RESERVED_DEPTH_MIN = -3;
	private static final int RESERVED_DEPTH_MAX = 2;
	private static final int RESERVED_SIDE_MIN = -8;
	private static final int RESERVED_SIDE_MAX = 7;
	private static final int RESERVED_Y_MIN = 0;
	private static final int RESERVED_Y_MAX = 2;

	private static StructureData cachedStructure;

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
		tooltip.add(Component.literal("Places SCP-914. Blocked space is highlighted."));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		Direction front = context.getHorizontalDirection();
		BlockPos origin = context.getClickedPos().relative(context.getClickedFace()).relative(front, PLAYER_CLEARANCE_OFFSET);
		StructureData structure = loadStructure();
		if (structure == null) {
			if (!level.isClientSide()) {
				message(player, "Cannot assemble SCP-914: structure data failed to load.");
			}
			return InteractionResult.FAIL;
		}

		List<BlockPos> blocked = findBlockedPositions(level, origin, front, structure);
		if (!blocked.isEmpty()) {
			StructurePlacementFeedback.reportBlocked(new BlockPlaceContext(context), blocked);
			return InteractionResult.FAIL;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.FAIL;
		}

		int rotationSteps = rotationSteps(front);
		for (TemplateBlock block : structure.blocks()) {
			BlockPos target = toWorld(origin, front, block.x(), block.y(), block.z());
			serverLevel.setBlock(target, rotateBlockState(block.state(), rotationSteps), 3);
		}

		if (player != null && !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		message(player, "SCP-914 assembled.");
		return InteractionResult.SUCCESS;
	}

	private static List<BlockPos> findBlockedPositions(Level level, BlockPos origin, Direction front, StructureData structure) {
		Set<BlockPos> required = new LinkedHashSet<>();

		for (TemplateBlock block : structure.blocks()) {
			required.add(toWorld(origin, front, block.x(), block.y(), block.z()));
		}

		for (int depth = RESERVED_DEPTH_MIN; depth <= RESERVED_DEPTH_MAX; depth++) {
			for (int side = RESERVED_SIDE_MIN; side <= RESERVED_SIDE_MAX; side++) {
				for (int y = RESERVED_Y_MIN; y <= RESERVED_Y_MAX; y++) {
					required.add(toWorld(origin, front,
							ANCHOR_X + depth,
							ANCHOR_Y + y,
							ANCHOR_Z + side));
				}
			}
		}

		List<BlockPos> blocked = new ArrayList<>();
		for (BlockPos target : required) {
			if (!level.getBlockState(target).canBeReplaced()) {
				blocked.add(target.immutable());
			}
		}
		return blocked;
	}

	private static StructureData loadStructure() {
		if (cachedStructure != null) {
			return cachedStructure;
		}
		try (InputStream stream = Scp914AssemblyKitItem.class.getResourceAsStream(STRUCTURE_RESOURCE)) {
			if (stream == null) {
				return null;
			}
			CompoundTag root = NbtIo.readCompressed(stream);
			ListTag paletteTag = root.getList("palette", Tag.TAG_COMPOUND);
			List<BlockState> palette = new ArrayList<>();
			for (int i = 0; i < paletteTag.size(); i++) {
				palette.add(readState(paletteTag.getCompound(i)));
			}

			ListTag blocksTag = root.getList("blocks", Tag.TAG_COMPOUND);
			List<TemplateBlock> blocks = new ArrayList<>();
			for (int i = 0; i < blocksTag.size(); i++) {
				CompoundTag blockTag = blocksTag.getCompound(i);
				ListTag pos = blockTag.getList("pos", Tag.TAG_INT);
				int stateIndex = blockTag.getInt("state");
				BlockState state = stateIndex >= 0 && stateIndex < palette.size() ? palette.get(stateIndex) : Blocks.AIR.defaultBlockState();
				if (!state.isAir()) {
					blocks.add(new TemplateBlock(pos.getInt(0), pos.getInt(1), pos.getInt(2), state));
				}
			}

			cachedStructure = new StructureData(blocks);
			return cachedStructure;
		} catch (IOException | RuntimeException exception) {
			return null;
		}
	}

	private static BlockState readState(CompoundTag tag) {
		ResourceLocation id = new ResourceLocation(tag.getString("Name"));
		Block block = ForgeRegistries.BLOCKS.getValue(id);
		if (block == null) {
			return Blocks.AIR.defaultBlockState();
		}
		BlockState state = block.defaultBlockState();
		if (tag.contains("Properties", Tag.TAG_COMPOUND)) {
			state = applyProperties(state, tag.getCompound("Properties"));
		}
		return state;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState applyProperties(BlockState state, CompoundTag properties) {
		BlockState result = state;
		for (String key : properties.getAllKeys()) {
			Property property = result.getBlock().getStateDefinition().getProperty(key);
			if (property == null) {
				continue;
			}
			Optional value = property.getValue(properties.getString(key));
			if (value.isPresent()) {
				try {
					result = result.setValue(property, (Comparable) value.get());
				} catch (Exception ignored) {
				}
			}
		}
		return result;
	}

	private static BlockState rotateBlockState(BlockState state, int steps) {
		BlockState result = state;
		if (result.hasProperty(HorizontalDirectionalBlock.FACING)) {
			Direction facing = result.getValue(HorizontalDirectionalBlock.FACING);
			for (int i = 0; i < steps; i++) {
				facing = facing.getClockWise();
			}
			result = result.setValue(HorizontalDirectionalBlock.FACING, facing);
		}
		return result;
	}

	private static int rotationSteps(Direction targetFront) {
		Direction current = BASE_FRONT;
		for (int steps = 0; steps < 4; steps++) {
			if (current == targetFront) {
				return steps;
			}
			current = current.getClockWise();
		}
		return 0;
	}

	private static BlockPos toWorld(BlockPos origin, Direction front, int x, int y, int z) {
		Direction back = front.getOpposite();
		Direction rightFromViewer = front.getCounterClockWise();
		int dx = x - ANCHOR_X;
		int dy = y - ANCHOR_Y;
		int dz = z - ANCHOR_Z;
		return origin.offset(back.getStepX() * dx + rightFromViewer.getStepX() * dz, dy, back.getStepZ() * dx + rightFromViewer.getStepZ() * dz);
	}

	private static void message(Player player, String text) {
		if (player != null) {
			player.displayClientMessage(Component.literal(text), true);
		}
	}

	private record TemplateBlock(int x, int y, int z, BlockState state) {
	}

	private record StructureData(List<TemplateBlock> blocks) {
	}
}
