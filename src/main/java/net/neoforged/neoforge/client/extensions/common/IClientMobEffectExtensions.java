package net.neoforged.neoforge.client.extensions.common;
import net.minecraft.world.effect.MobEffectInstance;
public interface IClientMobEffectExtensions {
    default boolean isVisibleInInventory(MobEffectInstance instance) { return true; }
    default boolean isVisibleInGui(MobEffectInstance instance) { return true; }
}
