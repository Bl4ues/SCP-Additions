package net.mcreator.scpadditions.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.mcreator.scpadditions.client.SystemTerminalItemRenderer;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** GeckoLib block item for the authored SCiPNET terminal model. */
public final class SystemTerminalItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public SystemTerminalItem(Block block) {
        super(block, new Item.Properties());
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SystemTerminalItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer
                    getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SystemTerminalItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // Static model.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
