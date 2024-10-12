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
		if (ScpAdditionsModVariables.MapVariables.get(world).Scp914Rough) {
			Scp914WindKeyRoughProcedure.execute(world, x, y, z, entity);
		} else {
			if (ScpAdditionsModVariables.MapVariables.get(world).Scp914Coarse) {
				Scp914WindKeyCoarseProcedure.execute(world, x, y, z, entity);
			} else {
				if (ScpAdditionsModVariables.MapVariables.get(world).Scp914OneToOne) {
					Scp914WindKey1to1Procedure.execute(world, x, y, z, entity);
				} else {
					if (ScpAdditionsModVariables.MapVariables.get(world).Scp914Fine) {
						Scp914WindKeyFineProcedure.execute(world, x, y, z, entity);
					} else {
						Scp914WindKeyVeryFineProcedure.execute(world, x, y, z, entity);
					}
				}
			}
		}
	}
}
