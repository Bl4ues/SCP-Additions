package net.mcreator.scpadditions.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Invisible synchronization marker for the beneficial and harmful SCP-1176
 * honey outcomes. The client uses its duration to render the honey vignette.
 */
public class Scp1176HoneyedEffect extends MobEffect {
    public Scp1176HoneyedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xD99A2B);
    }
}
