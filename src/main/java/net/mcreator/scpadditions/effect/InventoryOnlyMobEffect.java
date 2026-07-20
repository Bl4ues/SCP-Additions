package net.mcreator.scpadditions.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

/**
 * Shared visibility policy for conditions that should be listed in inventory
 * interfaces but should not occupy the vanilla in-world HUD effect strip.
 */
public abstract class InventoryOnlyMobEffect extends MobEffect {
    protected InventoryOnlyMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) {
                return true;
            }

            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                return false;
            }
        });
    }
}
