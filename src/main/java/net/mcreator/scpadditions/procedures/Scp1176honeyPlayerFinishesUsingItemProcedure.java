package net.mcreator.scpadditions.procedures;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.component.DataComponents;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

public class Scp1176honeyPlayerFinishesUsingItemProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((ScpAdditionsModVariables.getPlayerVariables(entity).orElse(new ScpAdditionsModVariables.PlayerVariables())).ABpos) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
				_entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 12000, 10, false, false));
				_entity.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.SCP_1176_HONEYED.get()), 12000, 0, false, false, false));
			}
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(MobEffects.POISON);
		} else {
			if (entity instanceof ServerPlayer player) {
				ScpEntityNetwork.playScp1176Music(player);
			}
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide()) {
				_entity.addEffect(new MobEffectInstance(MobEffects.POISON, 1360, 10, false, false));
				_entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1360, 1, false, false));
				_entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1360, 1, false, false));
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1360, 3, false, false));
				_entity.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.SCP_1176_HONEYED.get()), 1360, 0, false, false, false));
			}
			ScpAdditionsMod.queueServerWork(1360, () -> {
				if (entity instanceof LivingEntity _entity)
					_entity.hurt(new DamageSource(_entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
						@Override
						public Component getLocalizedDeathMessage(LivingEntity _msgEntity) {
							String _translatekey = "death.attack." + "scp1176";
							if (this.getEntity() == null && this.getDirectEntity() == null) {
								return _msgEntity.getKillCredit() != null
										? Component.translatable(_translatekey + ".player", _msgEntity.getDisplayName(), _msgEntity.getKillCredit().getDisplayName())
										: Component.translatable(_translatekey, _msgEntity.getDisplayName());
							} else {
								Component _component = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
								ItemStack _itemstack = ItemStack.EMPTY;
								if (this.getEntity() instanceof LivingEntity _livingentity)
									_itemstack = _livingentity.getMainHandItem();
								return !_itemstack.isEmpty() && _itemstack.has(DataComponents.CUSTOM_NAME)
										? Component.translatable(_translatekey + ".item", _msgEntity.getDisplayName(), _component, _itemstack.getDisplayName())
										: Component.translatable(_translatekey, _msgEntity.getDisplayName(), _component);
							}
						}
					}, 50);
			});
		}
	}
}
