package com.bl4ues.scpclassifieddirective.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Static GeckoLib host for the authored SCiPNET terminal geometry. */
public final class SystemTerminalBlockEntity extends BlockEntity
        implements GeoBlockEntity {
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public SystemTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.SCP_079_SYSTEM_CONTROL.get(),
                pos, state);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // Static model. GeckoLib is used for geometry, glowmask, and no-cull.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
