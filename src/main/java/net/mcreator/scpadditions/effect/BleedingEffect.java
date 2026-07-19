package net.mcreator.scpadditions.effect;

import net.minecraft.world.effect.MobEffectCategory;

/** Persistent blood loss caused by sufficiently severe SCP-012 exposure. */
public final class BleedingEffect extends InventoryOnlyMobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xA30F19);
    }
}
