package net.mcreator.scpadditions.procedures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.List;
import java.util.function.Supplier;

public final class TeslaGatePulseHelper {
	private TeslaGatePulseHelper() {
	}

	public static void pulseAndTransition(LevelAccessor world, double x, double y, double z, Supplier<? extends Block> expectedBlock, Supplier<? extends Block> nextBlock) {
		BlockPos pos = BlockPos.containing(x, y, z);
		if (world.getBlockState(pos).getBlock() != expectedBlock.get()) {
			return;
		}

		boolean manualOverride = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
		double pulseRadius = manualOverride ? 2.25D : 1.5D;
		float damage = manualOverride ? 40.0F : 20.0F;

		if (manualOverride) {
			emitOverrideParticles(world, x, y, z);
		}

		final Vec3 center = new Vec3(x, y, z);
		List<Entity> entities = world.getEntitiesOfClass(Entity.class,
				new AABB(center, center).inflate(pulseRadius), e -> true);
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity living) {
				living.hurt(new DamageSource(living.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
					@Override
					public Component getLocalizedDeathMessage(LivingEntity messageEntity) {
						String translateKey = "death.attack.teslagate";
						if (this.getEntity() == null && this.getDirectEntity() == null) {
							return messageEntity.getKillCredit() != null
									? Component.translatable(translateKey + ".player", messageEntity.getDisplayName(), messageEntity.getKillCredit().getDisplayName())
									: Component.translatable(translateKey, messageEntity.getDisplayName());
						}
						Component component = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
						ItemStack itemStack = ItemStack.EMPTY;
						if (this.getEntity() instanceof LivingEntity sourceLiving) {
							itemStack = sourceLiving.getMainHandItem();
						}
						return !itemStack.isEmpty() && itemStack.hasCustomHoverName()
								? Component.translatable(translateKey + ".item", messageEntity.getDisplayName(), component, itemStack.getDisplayName())
								: Component.translatable(translateKey, messageEntity.getDisplayName(), component);
					}
				}, damage);
			}
			if (entity instanceof ServerPlayer player) {
				Advancement advancement = player.server.getAdvancements().getAdvancement(ResourceLocation.parse("scp_additions:tesla"));
				if (advancement != null) {
					AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
					if (!progress.isDone()) {
						for (String criteria : progress.getRemainingCriteria()) {
							player.getAdvancements().award(advancement, criteria);
						}
					}
				}
			}
		}

		ScpAdditionsMod.queueServerWork(manualOverride ? 1 : 3, () -> TeslaGateTransitionHelper.transitionIfCurrent(world, x, y, z, expectedBlock, nextBlock));
	}

	private static void emitOverrideParticles(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof ServerLevel serverLevel)) {
			return;
		}
		serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + 0.5D, y + 1.05D, z + 0.5D, 8, 0.45D, 0.55D, 0.45D, 0.03D);
		serverLevel.sendParticles(ParticleTypes.SMOKE, x + 0.5D, y + 0.95D, z + 0.5D, 2, 0.35D, 0.30D, 0.35D, 0.01D);
	}
}
