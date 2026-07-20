package net.mcreator.scpadditions.effect;

import net.minecraft.core.registries.BuiltInRegistries;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class EyeSoreEffectEvents {
    private EyeSoreEffectEvents() {
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance.getEffect().value() == ScpAdditionsModMobEffects.EYE_SORE.get()
                && event.getEntity() instanceof Player player
                && EyeProtectionAccess.blocksExternalEyeSore(player)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide) return;
        MobEffectInstance instance = entity.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.EYE_SORE.get()));
        if (instance != null && entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.LUBRICATED_EYE.get()))) {
            entity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.EYE_SORE.get()));
            return;
        }
        if (instance == null || (!instance.isVisible() && !instance.showIcon())) return;
        int duration = instance.getDuration();
        if (duration <= 0) return;
        int amplifier = instance.getAmplifier();
        boolean ambient = instance.isAmbient();
        entity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.EYE_SORE.get()));
        // Suppress particles while retaining the icon for inventory screens.
        // EyeSoreEffect itself hides the vanilla HUD icon only.
        entity.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.EYE_SORE.get()),
                duration, amplifier, ambient, false, true));
    }
}
