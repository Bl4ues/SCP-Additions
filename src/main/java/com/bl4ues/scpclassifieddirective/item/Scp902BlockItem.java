package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.client.Scp902ItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** GeckoLib item renderer for either legacy SCP-902-A block-state item id. */
public final class Scp902BlockItem extends BlockItem implements GeoItem {
    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("closed");
    private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("open");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private final boolean open;

    public Scp902BlockItem(Block block, boolean open) {
        super(block, new Item.Properties());
        this.open = open;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public boolean isOpenModel() {
        return open;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("SCP-902-A");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private Scp902ItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new Scp902ItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "scp902_item", 0,
                state -> state.setAndContinue(open ? OPEN : CLOSED)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
