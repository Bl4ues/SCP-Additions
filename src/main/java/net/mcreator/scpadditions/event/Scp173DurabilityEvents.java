package net.mcreator.scpadditions.event;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173DurabilityEvents {
    private static final double MAX_HEALTH = 1730.0D;
    private static final double ARMOR = 80.0D;
    private static final double ARMOR_TOUGHNESS = 40.0D;
    private static final double KNOCKBACK_RESISTANCE = 1.0D;
    private static final float DAMAGE_MULTIPLIER = 0.02F;
    private static final float MIN_SURVIVABLE_DAMAGE = 0.25F;

    private Scp173DurabilityEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Scp173Entity scp173) || event.getLevel().isClientSide()) return;
        setBaseAttribute(scp173, Attributes.MAX_HEALTH, MAX_HEALTH);
        setBaseAttribute(scp173, Attributes.ARMOR, ARMOR);
        setBaseAttribute(scp173, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
        setBaseAttribute(scp173, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
        if (scp173.getHealth() < scp173.getMaxHealth()) scp173.setHealth(scp173.getMaxHealth());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Scp173Entity)) return;
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(Math.max(MIN_SURVIVABLE_DAMAGE, amount * DAMAGE_MULTIPLIER));
    }

    private static void setBaseAttribute(Scp173Entity scp173, Attribute attribute, double value) {
        AttributeInstance instance = scp173.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) instance.setBaseValue(value);
    }
}
