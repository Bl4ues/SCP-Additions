package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.client.DecontaminationItemRenderer;
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

/** GeckoLib-backed inventory renderer for the rebuilt checkpoint. */
public final class DecontaminationBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public DecontaminationBlockItem(Block block) {
        super(block, new Item.Properties());
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        // The public placement cell is the base of the entrance door. The
        // actual controller is kept one block above it so the BLACK_DOOR can
        // own the clicked cell. Structure coordinates deliberately use the
        // controller's block below as their logical origin.
        BlockPos placementPos = context.getClickedPos();
        BlockPos controllerPos = placementPos.above();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (DecontaminationStructure.hasPlacementSupport(
                context.getLevel(), controllerPos, facing)) {
            List<BlockPos> blockers = DecontaminationStructure.collectObstructions(
                    context.getLevel(), controllerPos, facing, placementPos);
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
            private DecontaminationItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new DecontaminationItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static inventory preview; the placed block owns the live animations.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
