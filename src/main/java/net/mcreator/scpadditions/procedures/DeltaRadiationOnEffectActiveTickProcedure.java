package net.mcreator.scpadditions.procedures;

import net.minecraft.util.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;

import net.mcreator.scpadditions.item.HazmatSuitItem;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class DeltaRadiationOnEffectActiveTickProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure DeltaRadiationOnEffectActiveTick!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		if (entity.isAlive() && (!(HazmatSuitItem.helmet == ((entity instanceof LivingEntity)
				? ((LivingEntity) entity).getItemStackFromSlot(EquipmentSlotType.HEAD)
				: ItemStack.EMPTY).getItem()
				&& HazmatSuitItem.body == ((entity instanceof LivingEntity)
						? ((LivingEntity) entity).getItemStackFromSlot(EquipmentSlotType.CHEST)
						: ItemStack.EMPTY).getItem()
				&& HazmatSuitItem.legs == ((entity instanceof LivingEntity)
						? ((LivingEntity) entity).getItemStackFromSlot(EquipmentSlotType.LEGS)
						: ItemStack.EMPTY).getItem()
				&& HazmatSuitItem.boots == ((entity instanceof LivingEntity)
						? ((LivingEntity) entity).getItemStackFromSlot(EquipmentSlotType.FEET)
						: ItemStack.EMPTY).getItem())
				|| !((entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new ScpAdditionsModVariables.PlayerVariables())).scp059infected0
						|| (entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
								.orElse(new ScpAdditionsModVariables.PlayerVariables())).scp059infected1))) {
			if (entity instanceof LivingEntity) {
				((LivingEntity) entity).attackEntityFrom(new DamageSource("scp059delta").setDamageBypassesArmor(), (float) 1);
			}
		}
	}
}
