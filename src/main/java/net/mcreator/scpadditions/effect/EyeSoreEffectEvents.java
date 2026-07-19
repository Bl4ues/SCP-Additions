package net.mcreator.scpadditions.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EyeSoreEffectEvents {
    private EyeSoreEffectEvents() {
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance.getEffect() == ScpAdditionsModMobEffects.EYE_SORE.get()
                && event.getEntity() instanceof Player player
                && EyeProtectionAccess.blocksExternalEyeSore(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        MobEffectInstance instance = entity.getEffect(ScpAdditionsModMobEffects.EYE_SORE.get());
        if (instance != null && entity.hasEffect(ScpAdditionsModMobEffects.LUBRICATED_EYE.get())) {
            entity.removeEffect(ScpAdditionsModMobEffects.EYE_SORE.get());
            return;
        }
        if (instance == null || (!instance.isVisible() && !instance.showIcon())) return;
        int duration = instance.getDuration();
        if (duration <= 0) return;
        int amplifier = instance.getAmplifier();
        boolean ambient = instance.isAmbient();
        entity.removeEffect(ScpAdditionsModMobEffects.EYE_SORE.get());
        // Suppress particles while retaining the icon for inventory screens.
        // EyeSoreEffect itself hides the vanilla HUD icon only.
        entity.addEffect(new MobEffectInstance(ScpAdditionsModMobEffects.EYE_SORE.get(),
                duration, amplifier, ambient, false, true));
    }
}
