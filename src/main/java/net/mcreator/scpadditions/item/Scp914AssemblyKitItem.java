package net.mcreator.scpadditions.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class Scp914AssemblyKitItem extends Item {
	private static final Direction BASE_FRONT = Direction.WEST;
	private static final int ANCHOR_X = 0;
	private static final int ANCHOR_Y = 0;
	private static final int ANCHOR_Z = 6;
	private static StructureData cachedStructure;
	private static final String SCP_914_FULL_NBT = "H4sIAAAAAAAA/6WazWrbQBSFRzPyf/IALaWljxDopll3Xbrq1qi2EoQdy1gKJn2DvnU9qY8JE809p8QwmJDPE517lE8weO7czJVd87sOzrm4RqdVntb1zE3rXd/0Td25+Jq58a9tu9p0c+eKcubCvu3wocsruFHXV319/sQQUwiMF5ggMKXAjARmLDATgZkKzExg5gKzEJgrgykug+aMNWcw1pzBWHMGY80ZjDVnMFZ2L2QHY2UHY2UHY2UHY2UHY2UHY2W/3B8CY2UH8zJ7kWHGAjMRGCs7GCu7c+f7Q2CYxyLDPBYZ5rGCzBkM81jhuMcKxz1WkDmDYR6LDPNYZJjHIsM8xjoFwzzG5gyGeYzNGQzzGMvuhexgmMdYdjDMYyw7GOYxlv3ieoFhHkuz+wwzERjmKJbLnfMzRz3PSGCYoyLDHOXJDMEwR0WGOco77ihP5gyGOSoyzFGRYY6KDHMU6xQMcxSbMxjmKDZnMMxRLLsXsoNhjmLZwTBHsexgmKNYdnDMUSw7GOaoNHuOYR5j2cEwjwWSHQzz2PPfExjmsUDmDIZ5LDjuseC4xwKZMxjmsfjOPBbfmcfiO/MY6xSM1SkYq1MwVqdgmDNZp2CYM1mnYJgzWadgrE7BWJ2CYX5mnYKxOgVjdQrG6hQMexawTsGwZwHrFAx7FrBOwVidgrE6BcOeO6zTyz0iMFanlyUw7BnHOsWyOsWyOsVizzjWKZbVKZZyXveyr5BhvMAEgZkJzFxgFgaDMysrFxgrFxgrFxgrFxgrFxgrVxBygbFygbFygbFygbFygbFylQ4XxBkrOxgrOxgrO5hSYEYC89ItZYaZCPtMBcbqC4zVFxilryvyf5qeeeYY5pb0zDPHMLek54c5hrmF5fJCLi/k8kIuL+TyQq4g5AoDuUYZhrklPfcbZxjmnzT7JMOw+zk998sxzD+sdzDMP+kZ4zTDMP+kc55lGOaf9BxynmGYf9i9Ckbpi/knPc/MMcw/6XlmjmH+Sc8GcwzzD8vlhVxeyOWFXF7I5YVcQcgVhFxByBWEXEHIFYRcpXt9XpdjmFtYdjDMLenZYI5hbknPBnMMc0t6NphjmFtYX2CUviy3lOd31mkQOg1Cp0HoNAidBqHTIHQahE6D0GkQOg1Cp0HoNAx0OtlX27rv63n8/NSV36uH2r3vVvtltV43fdPuutv409ebL8/fYzo99OY/Du2+PsSvOE3d+K5aNbt7V9ZV1zts8Glwg1Xc4NgeNl12l2P9P7sAvH5odvXqUN31t1Vz0C7x4+Dmy039tDw2u7W2yefhTdrHfv/YL9dt++piFsfT7A/b9v6+XrvRXbXt6tzm74ZbaNdPb7q4ZtdXm3rw4ob3+WCFfMv9sG6q7fKmb5c3b7qQf4FOd/PiW9VXP+tDd/q1c9d/3F9hjr/RqCcAAA==";

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
		tooltip.add(Component.literal("Places the saved SCP-914 structure."));
		tooltip.add(Component.literal("Click the floor at the front-center of the machine."));
		tooltip.add(Component.literal("Blocked space is marked with red particles."));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.FAIL;
		}

		Player player = context.getPlayer();
		Direction front = context.getHorizontalDirection().getOpposite();
		BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
		StructureData structure = loadStructure();
		if (structure == null) {
			message(player, "Cannot assemble SCP-914: structure data failed to load.");
			return InteractionResult.FAIL;
		}

		int rotationSteps = rotationSteps(front);
		List<BlockPos> blocked = findBlockedPositions(serverLevel, origin, front, structure);
		if (!blocked.isEmpty()) {
			highlightBlocked(serverLevel, blocked);
			message(player, "Cannot assemble SCP-914: clear the highlighted area first.");
			return InteractionResult.FAIL;
		}

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

	private static List<BlockPos> findBlockedPositions(ServerLevel level, BlockPos origin, Direction front, StructureData structure) {
		List<BlockPos> blocked = new ArrayList<>();
		for (TemplateBlock block : structure.blocks()) {
			BlockPos target = toWorld(origin, front, block.x(), block.y(), block.z());
			BlockState existing = level.getBlockState(target);
			if (!existing.canBeReplaced()) {
				blocked.add(target);
			}
		}
		return blocked;
	}

	private static void highlightBlocked(ServerLevel level, List<BlockPos> blocked) {
		int count = Math.min(96, blocked.size());
		for (int i = 0; i < count; i++) {
			BlockPos pos = blocked.get(i);
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D, 4, 0.28D, 0.28D, 0.28D, 0.0D);
		}
	}

	private static StructureData loadStructure() {
		if (cachedStructure != null) {
			return cachedStructure;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(SCP_914_FULL_NBT);
			CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(bytes));
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
				blocks.add(new TemplateBlock(pos.getInt(0), pos.getInt(1), pos.getInt(2), state));
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
