package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.procedures.SCP572ItemInInventoryTickProcedure;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * SCP-572 is physically a poor weapon. Its anomalous strength exists only in
 * the holder's perception; client presentation of that delusion lives in
 * Scp572ClientEffects so the server never grants the apparent power.
 */
public class SCP572Item extends Item {
    private static final double REAL_ATTACK_DAMAGE_MODIFIER = 1.0D;
    private static final double REAL_ATTACK_SPEED_MODIFIER = -3.0D;

    public SCP572Item() {
        super(new Item.Properties()
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.COMMON));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(
            EquipmentSlot equipmentSlot) {
        if (equipmentSlot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder =
                    ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(equipmentSlot));
            // Players have 1 base attack damage, so +1 gives SCP-572 a real
            // fully-charged hit of 2 damage despite its much grander tooltip.
            builder.put(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                            "Item modifier", REAL_ATTACK_DAMAGE_MODIFIER,
                            AttributeModifier.Operation.ADDITION));
            // Players have 4 base attack speed, leaving the real weapon at
            // only 1 attack per second. The holder sees a client-only lie.
            builder.put(Attributes.ATTACK_SPEED,
                    new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                            "Item modifier", REAL_ATTACK_SPEED_MODIFIER,
                            AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(equipmentSlot);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world,
            List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        list.add(Component.literal("Katana of Apparent Invincibility"));
    }

    @Override
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity,
            int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        SCP572ItemInInventoryTickProcedure.execute(entity);
    }
}
