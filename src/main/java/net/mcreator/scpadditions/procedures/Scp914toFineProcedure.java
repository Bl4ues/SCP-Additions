package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

import java.util.Map;

public class Scp914toFineProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		{
			BlockPos _bp = BlockPos.containing(x, y, z);
			BlockState _bs = ScpAdditionsModBlocks.SCP_914DIAL_FINE.get().defaultBlockState();
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
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Rough = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Coarse = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914OneToOne = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Fine = true;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914VeryFine = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914dial")), SoundSource.NEUTRAL, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914dial")), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
	}
}
