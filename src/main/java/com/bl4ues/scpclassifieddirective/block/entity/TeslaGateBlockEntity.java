package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.client.TeslaGateElectricity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Static GeckoLib host for the full Tesla Gate model. */
public final class TeslaGateBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public TeslaGateBlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.TESLA_GATE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
            TeslaGateBlockEntity blockEntity) {
        TeslaGateElectricity.clientTick(level, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The replacement gate is static; electrical motion is particle-driven.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Covers the wide cabinet, frame, cameras and floor plates.
        return new AABB(worldPosition).inflate(4.5D, 4.5D, 3.0D);
    }
}
