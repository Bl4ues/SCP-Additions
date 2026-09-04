package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A client-only visual object. The PDA deliberately is not an item or entity. */
public final class InventoryPdaAnimatable implements GeoAnimatable {
    public static final InventoryPdaAnimatable INSTANCE = new InventoryPdaAnimatable();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private InventoryPdaAnimatable() {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Screen-space movement is procedural; the authored model stays rigid.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return System.nanoTime() / 50_000_000.0D;
    }
}
