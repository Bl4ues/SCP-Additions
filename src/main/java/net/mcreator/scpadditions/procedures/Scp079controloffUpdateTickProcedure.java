package net.mcreator.scpadditions.procedures;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.state.Property;
import net.minecraft.block.BlockState;

import net.mcreator.scpadditions.world.TeslaGateOnGameRule;
import net.mcreator.scpadditions.world.Scp079controlOnGameRule;
import net.mcreator.scpadditions.block.Scp079controloffBlock;
import net.mcreator.scpadditions.block.Scp079controlBlock;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class Scp079controloffUpdateTickProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp079controloffUpdateTick!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure Scp079controloffUpdateTick!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure Scp079controloffUpdateTick!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure Scp079controloffUpdateTick!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		if (world.getWorldInfo().getGameRulesInstance().getBoolean(Scp079controlOnGameRule.gamerule) == true) {
			{
				BlockPos _bp = new BlockPos(x, y, z);
				BlockState _bs = Scp079controlBlock.block.getDefaultState();
				BlockState _bso = world.getBlockState(_bp);
				for (Map.Entry<Property<?>, Comparable<?>> entry : _bso.getValues().entrySet()) {
					Property _property = _bs.getBlock().getStateContainer().getProperty(entry.getKey().getName());
					if (_property != null && _bs.get(_property) != null)
						try {
							_bs = _bs.with(_property, (Comparable) entry.getValue());
						} catch (Exception e) {
						}
				}
				world.setBlockState(_bp, _bs, 3);
			}
			if (world instanceof World) {
				((World) world).getGameRules().get(TeslaGateOnGameRule.gamerule).set((true), ((World) world).getServer());
			}
		}
		if (world.getWorldInfo().getGameRulesInstance().getBoolean(Scp079controlOnGameRule.gamerule) == false) {
			{
				BlockPos _bp = new BlockPos(x, y, z);
				BlockState _bs = Scp079controloffBlock.block.getDefaultState();
				BlockState _bso = world.getBlockState(_bp);
				for (Map.Entry<Property<?>, Comparable<?>> entry : _bso.getValues().entrySet()) {
					Property _property = _bs.getBlock().getStateContainer().getProperty(entry.getKey().getName());
					if (_property != null && _bs.get(_property) != null)
						try {
							_bs = _bs.with(_property, (Comparable) entry.getValue());
						} catch (Exception e) {
						}
				}
				world.setBlockState(_bp, _bs, 3);
			}
			if (world instanceof World) {
				((World) world).getGameRules().get(TeslaGateOnGameRule.gamerule).set((false), ((World) world).getServer());
			}
		}
	}
}
