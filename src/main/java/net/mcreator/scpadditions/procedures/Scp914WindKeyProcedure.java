package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.data.Scp914Processor;
import net.mcreator.scpadditions.data.Scp914RecipeManager;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

public class Scp914WindKeyProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ServerPlayer _player) {
			Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("scp_additions:scp_914_achievement"));
			AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
			if (!_ap.isDone()) {
				for (String criteria : _ap.getRemainingCriteria())
					_player.getAdvancements().award(_adv, criteria);
			}
		}
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914key")), SoundSource.NEUTRAL, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914key")), SoundSource.NEUTRAL, 1, 1, false);
			}
		}

		Scp914Processor.process(world, x, y, z, entity, getSelectedSetting(world));
	}

	private static Scp914RecipeManager.Setting getSelectedSetting(LevelAccessor world) {
		ScpAdditionsModVariables.MapVariables variables = ScpAdditionsModVariables.MapVariables.get(world);
		if (variables.Scp914Rough) {
			return Scp914RecipeManager.Setting.ROUGH;
		}
		if (variables.Scp914Coarse) {
			return Scp914RecipeManager.Setting.COARSE;
		}
		if (variables.Scp914OneToOne) {
			return Scp914RecipeManager.Setting.ONE_TO_ONE;
		}
		if (variables.Scp914Fine) {
			return Scp914RecipeManager.Setting.FINE;
		}
		return Scp914RecipeManager.Setting.VERY_FINE;
	}
}