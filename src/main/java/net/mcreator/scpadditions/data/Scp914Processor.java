package net.mcreator.scpadditions.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp914Processor {
	private Scp914Processor() {
	}

	public static void process(LevelAccessor world, double x, double y, double z, Entity user, Scp914RecipeManager.Setting setting) {
		if (!(world instanceof ServerLevel level)) {
			return;
		}

		BlockPos keyPos = BlockPos.containing(x, y, z);
		Scp914RecipeManager.MachineConfig machineConfig = Scp914RecipeManager.machineConfig();
		Direction front = getFacing(level, keyPos);
		Optional<ProcessingContext> optionalContext = tryDirection(level, keyPos, setting, machineConfig, front);
		if (optionalContext.isEmpty()) {
			return;
		}

		ProcessingContext context = optionalContext.get();
		closeDoorsImmediately(level, keyPos);
		playSound(world, x, y, z, "scp_additions:scp914refining");
		setRefining(world, true);

		ScpAdditionsMod.queueServerWork(machineConfig.startDelayTicks(), () -> {
			applyRecipe(level, context.outputCenter(), context.match());
			ScpAdditionsMod.queueServerWork(machineConfig.finishDelayTicks(), () -> setRefining(world, false));
		});
	}

	private static Optional<ProcessingContext> tryDirection(ServerLevel level, BlockPos keyPos, Scp914RecipeManager.Setting setting, Scp914RecipeManager.MachineConfig machineConfig, Direction front) {
		Vec3 intakeCenter = centerOf(keyPos.offset(toWorldOffset(machineConfig.intakeOffset(), front)));
		Vec3 outputCenter = centerOf(keyPos.offset(toWorldOffset(machineConfig.outputOffset(), front)));

		List<ItemEntity> itemInputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(intakeCenter, intakeCenter).inflate(machineConfig.searchRadius()), item -> !item.isRemoved() && !item.getItem().isEmpty())
				.stream()
				.sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(intakeCenter)))
				.toList();
		List<Entity> entityInputs = level.getEntitiesOfClass(Entity.class, new AABB(intakeCenter, intakeCenter).inflate(machineConfig.searchRadius()), entity -> !(entity instanceof ItemEntity) && !entity.isRemoved())
				.stream()
				.sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(intakeCenter)))
				.toList();

		return Scp914RecipeManager.findRecipe(setting, itemInputs, entityInputs).map(match -> new ProcessingContext(match, outputCenter));
	}

	private static void applyRecipe(ServerLevel level, Vec3 outputCenter, Scp914RecipeManager.RecipeMatch match) {
		if (level.random.nextFloat() > match.recipe().chance()) {
			consumeInputs(match);
			return;
		}

		ItemStack firstInputStack = match.firstInputStack();
		consumeInputs(match);

		for (Scp914RecipeManager.ItemOutput output : Scp914RecipeManager.rollItemOutputs(match.recipe(), level.random)) {
			ItemStack outputStack = Scp914RecipeManager.createItemOutput(output, firstInputStack, match.recipe().copyInputNbt());
			if (!outputStack.isEmpty()) {
				ItemEntity outputEntity = new ItemEntity(level, outputCenter.x, outputCenter.y, outputCenter.z, outputStack);
				outputEntity.setPickUpDelay(10);
				level.addFreshEntity(outputEntity);
			}
		}

		for (Scp914RecipeManager.EntityOutput output : match.recipe().entityOutputs()) {
			Optional<EntityType<?>> type = Scp914RecipeManager.getEntityType(output);
			if (type.isEmpty()) {
				ScpAdditionsMod.LOGGER.warn("SCP-914 recipe {} points to missing entity output {}", match.recipe().id(), output.entity());
				continue;
			}
			for (int i = 0; i < output.count(); i++) {
				Entity spawned = type.get().spawn(level, BlockPos.containing(outputCenter), MobSpawnType.MOB_SUMMONED);
				if (spawned != null) {
					spawned.setDeltaMovement(0, 0, 0);
				}
			}
		}
	}

	private static void consumeInputs(Scp914RecipeManager.RecipeMatch match) {
		for (Scp914RecipeManager.ItemUse itemUse : match.itemUses()) {
			ItemStack stack = itemUse.entity().getItem();
			stack.shrink(itemUse.count());
			if (stack.isEmpty()) {
				itemUse.entity().discard();
			} else {
				itemUse.entity().setItem(stack);
			}
		}
		for (Scp914RecipeManager.EntityUse entityUse : match.entityUses()) {
			if (entityUse.consume() && !(entityUse.entity() instanceof Player)) {
				entityUse.entity().discard();
			}
		}
	}

	private static void closeDoorsImmediately(ServerLevel level, BlockPos keyPos) {
		boolean closedAnyDoor = false;
		for (BlockPos pos : BlockPos.betweenClosed(keyPos.offset(-8, -4, -8), keyPos.offset(8, 4, 8))) {
			BlockPos target = pos.immutable();
			BlockState state = level.getBlockState(target);
			Block block = state.getBlock();
			if (block == ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR.get()) {
				level.setBlock(target, copyProperties(state, ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR_CLOSED.get().defaultBlockState()), 3);
				closedAnyDoor = true;
			} else if (block == ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR.get()) {
				level.setBlock(target, copyProperties(state, ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR_CLOSED.get().defaultBlockState()), 3);
				closedAnyDoor = true;
			}
		}

		if (closedAnyDoor) {
			level.playSound(null, keyPos, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914doorclose")), SoundSource.NEUTRAL, 1, 1);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState copyProperties(BlockState from, BlockState to) {
		BlockState result = to;
		for (Map.Entry<Property<?>, Comparable<?>> entry : from.getValues().entrySet()) {
			Property property = result.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
			if (property != null) {
				try {
					result = result.setValue((Property) property, (Comparable) entry.getValue());
				} catch (Exception ignored) {
				}
			}
		}
		return result;
	}

	private static Direction getFacing(LevelAccessor world, BlockPos keyPos) {
		BlockState state = world.getBlockState(keyPos);
		if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
			return state.getValue(HorizontalDirectionalBlock.FACING);
		}
		return Direction.NORTH;
	}

	private static BlockPos toWorldOffset(Scp914RecipeManager.Offset offset, Direction front) {
		Direction rightFromViewer = front.getCounterClockWise();
		Vec3i right = rightFromViewer.getNormal();
		Vec3i forward = front.getNormal();
		return new BlockPos(
				right.getX() * offset.x() + forward.getX() * offset.z(),
				offset.y(),
				right.getZ() * offset.x() + forward.getZ() * offset.z());
	}

	private static Vec3 centerOf(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}

	private static void setRefining(LevelAccessor world, boolean value) {
		ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = value;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
	}

	private static void playSound(LevelAccessor world, double x, double y, double z, String soundId) {
		if (world instanceof Level level) {
			ResourceLocation sound = new ResourceLocation(soundId);
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, 1, 1);
			} else {
				level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
	}

	private record ProcessingContext(Scp914RecipeManager.RecipeMatch match, Vec3 outputCenter) {
	}
}