package com.bl4ues.scpinventory.effect;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class EyeSoreEffectEvents {
    private EyeSoreEffectEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        MobEffectInstance instance = entity.getEffect(ModMobEffects.EYE_SORE.get());
        if (instance == null || (!instance.isVisible() && !instance.showIcon())) {
            return;
        }

        int duration = instance.getDuration();
        if (duration <= 0) {
            return;
        }

        int amplifier = instance.getAmplifier();
        boolean ambient = instance.isAmbient();
        entity.removeEffect(ModMobEffects.EYE_SORE.get());
        entity.addEffect(new MobEffectInstance(ModMobEffects.EYE_SORE.get(), duration, amplifier, ambient, false, false));
    }
}
