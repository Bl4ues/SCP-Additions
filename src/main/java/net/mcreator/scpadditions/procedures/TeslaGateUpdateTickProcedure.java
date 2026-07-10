package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.Comparator;
import java.util.List;

public class TeslaGateUpdateTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (!world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON)) {
			return;
		}

		final Vec3 center = new Vec3(x, y, z);
		List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center))).toList();
		for (Entity ignored : entities) {
			if (world instanceof Level level) {
				if (!level.isClientSide()) {
					level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:teslaactivate")), SoundSource.HOSTILE, 1, 1);
				} else {
					level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:teslaactivate")), SoundSource.HOSTILE, 1, 1, false);
				}
			}
			ScpAdditionsMod.queueServerWork(5, () -> TeslaGateTransitionHelper.transitionIfCurrent(world, x, y, z, ScpAdditionsModBlocks.TESLA_GATE, ScpAdditionsModBlocks.TESLA_ACTIVE));
			break;
		}
	}
}
