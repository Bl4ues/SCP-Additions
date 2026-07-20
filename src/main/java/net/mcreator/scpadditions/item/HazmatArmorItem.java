package net.mcreator.scpadditions.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.mcreator.scpadditions.client.HazmatArmorRenderer;
import net.mcreator.scpadditions.init.ScpAdditionsModArmorMaterials;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** Hidden armor proxy used only while the public Hazmat Suit is worn. */
public final class HazmatArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HazmatArmorItem(Type type) {
        super(ScpAdditionsModArmorMaterials.HAZMAT, type,
                new Item.Properties().stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity, ItemStack itemStack,
                    EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new HazmatArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack,
                        equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // The armor follows the player's humanoid pose. No independent
        // animation controller is required for the first implementation.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
