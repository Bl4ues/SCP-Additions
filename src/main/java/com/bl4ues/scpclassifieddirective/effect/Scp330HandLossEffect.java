package com.bl4ues.scpclassifieddirective.effect;

import net.minecraft.world.effect.MobEffectCategory;

/** Hidden harmful state used to synchronize SCP-330 hand loss to clients. */
public final class Scp330HandLossEffect extends InventoryOnlyMobEffect {
    public Scp330HandLossEffect() {
        super(MobEffectCategory.HARMFUL, 0x6F1010);
    }
}
