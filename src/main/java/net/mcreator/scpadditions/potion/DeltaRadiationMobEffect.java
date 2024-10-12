
package net.mcreator.scpadditions.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.mcreator.scpadditions.procedures.DeltaRadiationOnEffectActiveTickProcedure;

public class DeltaRadiationMobEffect extends MobEffect {
	public DeltaRadiationMobEffect() {
		super(MobEffectCategory.HARMFUL, -16750849);
	}

	@Override
	public String getDescriptionId() {
		return "effect.scp_additions.delta_radiation";
	}

	@Override
	public boolean isInstantenous() {
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		DeltaRadiationOnEffectActiveTickProcedure.execute(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
