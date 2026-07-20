package net.mcreator.scpadditions.event;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173DurabilityEvents {
    private static final double MAX_HEALTH = 1730.0D;
    private static final double ARMOR = 0.0D;
    private static final double ARMOR_TOUGHNESS = 0.0D;
    private static final double KNOCKBACK_RESISTANCE = 1.0D;
    private static final float DAMAGE_THRESHOLD = 6.0F;
    private static final float MIN_ACCEPTED_DAMAGE = 1.0F;

    private Scp173DurabilityEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Scp173Entity scp173) || event.getLevel().isClientSide()) return;
        float previousMaxHealth = scp173.getMaxHealth();
        float previousHealth = scp173.getHealth();
        setBaseAttribute(scp173, Attributes.MAX_HEALTH, MAX_HEALTH);
        setBaseAttribute(scp173, Attributes.ARMOR, ARMOR);
        setBaseAttribute(scp173, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS);
        setBaseAttribute(scp173, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);

        // Initialize old/fresh 80-health statues proportionally, but never heal a
        // damaged 173 every time its chunk or world is loaded.
        if (previousHealth > 0.0F && previousMaxHealth > 0.0F
                && previousMaxHealth < MAX_HEALTH) {
            double ratio = Math.min(1.0D, previousHealth / previousMaxHealth);
            scp173.setHealth((float) Math.max(1.0D, MAX_HEALTH * ratio));
        } else if (previousHealth > scp173.getMaxHealth()) {
            scp173.setHealth(scp173.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Scp173Entity)) return;
        float amount = event.getAmount();
        if (amount <= DAMAGE_THRESHOLD) {
            event.setCanceled(true);
            return;
        }

        // Six damage or less cannot scratch the statue. Stronger attacks deal
        // one point at seven damage, then scale upward with their excess power.
        event.setAmount(Math.max(MIN_ACCEPTED_DAMAGE, amount - DAMAGE_THRESHOLD));
    }

    private static void setBaseAttribute(Scp173Entity scp173, Attribute attribute, double value) {
        AttributeInstance instance = scp173.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) instance.setBaseValue(value);
    }
}
