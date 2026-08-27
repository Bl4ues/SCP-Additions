package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.client.TeslaGateItemRenderer;
import com.bl4ues.scpclassifieddirective.facility.StructurePlacementFeedback;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/** GeckoLib item renderer host for the replacement Tesla Gate. */
public final class TeslaGateBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public TeslaGateBlockItem(Block block) {
        super(block, new Item.Properties());
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos controllerPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos support = controllerPos.below();
        if (context.getLevel().getBlockState(support).isFaceSturdy(
                context.getLevel(), support, Direction.UP)) {
            List<BlockPos> blockers = TeslaGateStructure.collectObstructions(
                    context.getLevel(), controllerPos, facing);
            if (!blockers.isEmpty()) {
                StructurePlacementFeedback.reportBlocked(context, blockers);
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private TeslaGateItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new TeslaGateItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static item model.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
