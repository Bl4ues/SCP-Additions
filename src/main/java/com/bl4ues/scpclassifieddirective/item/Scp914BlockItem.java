package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.client.Scp914ItemRenderer;
import com.bl4ues.scpclassifieddirective.facility.StructurePlacementFeedback;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The only public placement item for SCP-914. */
public final class Scp914BlockItem extends BlockItem implements GeoItem {
    private static final int FORWARD_MIN = -3;
    private static final int FORWARD_MAX = 2;
    private static final int SIDE_MIN = -8;
    private static final int SIDE_MAX = 7;
    private static final int Y_MIN = 0;
    private static final int Y_MAX = 2;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public Scp914BlockItem(Block block) {
        super(block, new Item.Properties().stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("SCP-914");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("The Clockworks"));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos origin = context.getClickedPos();
        Direction front = context.getHorizontalDirection().getOpposite();
        List<BlockPos> blocked = collectObstructions(context.getLevel(), origin, front);
        if (!blocked.isEmpty()) {
            StructurePlacementFeedback.reportBlocked(context, blocked);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    private static List<BlockPos> collectObstructions(Level level, BlockPos origin, Direction front) {
        Direction right = front.getClockWise();
        List<BlockPos> blocked = new ArrayList<>();
        for (int forward = FORWARD_MIN; forward <= FORWARD_MAX; forward++) {
            for (int side = SIDE_MIN; side <= SIDE_MAX; side++) {
                for (int y = Y_MIN; y <= Y_MAX; y++) {
                    BlockPos target = origin.relative(front, forward).relative(right, side).above(y);
                    if (target.equals(origin)) continue;
                    if (!level.getBlockState(target).canBeReplaced()) blocked.add(target.immutable());
                }
            }
        }
        return blocked;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private Scp914ItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new Scp914ItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
