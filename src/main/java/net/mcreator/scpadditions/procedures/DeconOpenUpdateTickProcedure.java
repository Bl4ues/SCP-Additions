package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.List;
import java.util.Comparator;

public class DeconOpenUpdateTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		{
			final Vec3 _center = new Vec3((x - 0.5), y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2.5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				ScpAdditionsMod.queueServerWork(3, () -> {
					{
						BlockPos _bp = BlockPos.containing(x, y, z);
						BlockState _bs = ScpAdditionsModBlocks.DECON_CLOSED.get().defaultBlockState();
						BlockState _bso = world.getBlockState(_bp);
						for (Map.Entry<Property<?>, Comparable<?>> entry : _bso.getValues().entrySet()) {
							Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
							if (_property != null && _bs.getValue(_property) != null)
								try {
									_bs = _bs.setValue(_property, (Comparable) entry.getValue());
								} catch (Exception e) {
								}
						}
						world.setBlock(_bp, _bs, 3);
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x - 0.5, y, z - 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:doorclosing")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound((x - 0.5), y, (z - 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:doorclosing")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x - 0.5, y, z + 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:doorclosing")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound((x - 0.5), y, (z + 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:doorclosing")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
				});
			}
		}
	}
}
