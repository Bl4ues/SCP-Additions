package net.mcreator.scpadditions.procedures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Comparator;
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

		final Vec3 center = new Vec3(x, y, z);
		List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center))).toList();
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
				}, 20);
			}
			if (entity instanceof ServerPlayer player) {
				Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("scp_additions:tesla"));
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

		ScpAdditionsMod.queueServerWork(3, () -> TeslaGateTransitionHelper.transitionIfCurrent(world, x, y, z, expectedBlock, nextBlock));
	}
}
