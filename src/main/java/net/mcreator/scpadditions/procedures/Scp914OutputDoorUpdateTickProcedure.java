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
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class Scp914OutputDoorUpdateTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (ScpAdditionsModVariables.MapVariables.get(world).Scp914refining) {
			ScpAdditionsMod.queueServerWork(20, () -> {
				{
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockState _bs = ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR_CLOSED.get().defaultBlockState();
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
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914doorclose")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914doorclose")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
			});
		}
	}
}
