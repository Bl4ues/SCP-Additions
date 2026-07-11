package net.mcreator.scpadditions.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EyeSoreEffectEvents {
    private EyeSoreEffectEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        MobEffectInstance instance = entity.getEffect(ScpAdditionsModMobEffects.EYE_SORE.get());
        if (instance == null || (!instance.isVisible() && !instance.showIcon())) return;
        int duration = instance.getDuration();
        if (duration <= 0) return;
        int amplifier = instance.getAmplifier();
        boolean ambient = instance.isAmbient();
        entity.removeEffect(ScpAdditionsModMobEffects.EYE_SORE.get());
        entity.addEffect(new MobEffectInstance(ScpAdditionsModMobEffects.EYE_SORE.get(),
                duration, amplifier, ambient, false, false));
    }
}
