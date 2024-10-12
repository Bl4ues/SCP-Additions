package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

public class Scp914WindKeyVeryFineProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914refining")), SoundSource.NEUTRAL, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914refining")), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
		ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = true;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsMod.queueServerWork(30, () -> {
			if (!world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
				VeryFineItemsProcedure.execute(world, x, y, z);
			} else {
				VeryFineAliveProcedure.execute(world, x, y, z, entity);
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		});
	}
}
