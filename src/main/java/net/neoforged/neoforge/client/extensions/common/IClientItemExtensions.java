package net.neoforged.neoforge.client.extensions.common;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
public interface IClientItemExtensions {
    default HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
            EquipmentSlot slot, HumanoidModel<?> original) { return original; }
}
