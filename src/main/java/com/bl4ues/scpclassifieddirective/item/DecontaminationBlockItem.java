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
        // BlockPlaceContext#getClickedPos() is already the actual placement
        // cell (it resolves the clicked face when the clicked block is not
        // replaceable). The controller belongs in this exact cell. The model's
        // authored floor is one block below it and therefore legitimately
        // collides with an existing floor instead of silently lifting the whole
        // checkpoint upward.
        BlockPos controllerPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (DecontaminationStructure.hasPlacementSupport(
                context.getLevel(), controllerPos, facing)) {
            List<BlockPos> blockers = DecontaminationStructure.collectObstructions(
                    context.getLevel(), controllerPos, facing, controllerPos);
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
