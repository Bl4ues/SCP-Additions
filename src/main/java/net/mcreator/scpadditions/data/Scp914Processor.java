package net.mcreator.scpadditions.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Comparator;
import java.util.Optional;

public final class Scp914Processor {
	private static final int START_DELAY_TICKS = 30;
	private static final int FINISH_DELAY_TICKS = 160;
	private static final Vec3 INTAKE_OFFSET = new Vec3(-4, 0, -3);
	private static final Vec3 OUTPUT_OFFSET = new Vec3(4, 0, -3);

	private Scp914Processor() {
	}

	public static void process(LevelAccessor world, double x, double y, double z, Entity user, Scp914RecipeManager.Setting setting) {
		playSound(world, x, y, z, "scp_additions:scp914refining");
		setRefining(world, true);

		ScpAdditionsMod.queueServerWork(START_DELAY_TICKS, () -> {
			tryProcessItem(world, x, y, z, setting);
			ScpAdditionsMod.queueServerWork(FINISH_DELAY_TICKS, () -> setRefining(world, false));
		});
	}

	private static void tryProcessItem(LevelAccessor world, double x, double y, double z, Scp914RecipeManager.Setting setting) {
		if (!(world instanceof ServerLevel level)) {
			return;
		}

		Vec3 intakeCenter = new Vec3(x + INTAKE_OFFSET.x, y + INTAKE_OFFSET.y, z + INTAKE_OFFSET.z);
		Optional<ItemEntity> optionalInput = level.getEntitiesOfClass(ItemEntity.class, new AABB(intakeCenter, intakeCenter).inflate(2), item -> !item.getItem().isEmpty())
				.stream()
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(intakeCenter)));

		if (optionalInput.isEmpty()) {
			return;
		}

		ItemEntity inputEntity = optionalInput.get();
		ItemStack inputStack = inputEntity.getItem();
		Optional<Scp914RecipeManager.RecipeDefinition> optionalRecipe = Scp914RecipeManager.findRecipe(setting, inputStack);
		if (optionalRecipe.isEmpty()) {
			return;
		}

		Scp914RecipeManager.RecipeDefinition recipe = optionalRecipe.get();
		ItemStack outputStack = Scp914RecipeManager.createResult(recipe, inputStack);
		if (outputStack.isEmpty()) {
			return;
		}

		inputStack.shrink(recipe.inputCount());
		if (inputStack.isEmpty()) {
			inputEntity.discard();
		} else {
			inputEntity.setItem(inputStack);
		}

		if (level.random.nextFloat() <= recipe.chance()) {
			Vec3 outputPos = new Vec3(x + OUTPUT_OFFSET.x, y + OUTPUT_OFFSET.y, z + OUTPUT_OFFSET.z);
			ItemEntity outputEntity = new ItemEntity(level, outputPos.x, outputPos.y, outputPos.z, outputStack);
			outputEntity.setPickUpDelay(10);
			level.addFreshEntity(outputEntity);
		}
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
}