package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.function.Supplier;
import java.util.Map;

public class Scp294CoinSlotProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (ScpAdditionsModVariables.WorldVariables.get(world).coinslot == 0
				&& ScpAdditionsModItems.COIN.get() == (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(0)).getItem() : ItemStack.EMPTY).getItem()) {
			ScpAdditionsModVariables.WorldVariables.get(world).coinslot = ScpAdditionsModVariables.WorldVariables.get(world).coinslot + 1;
			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294coinslot")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294coinslot")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
		}
	}
}
