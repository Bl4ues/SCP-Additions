package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
public final class MobEffectEvent {
    private MobEffectEvent() {}
    public static class Applicable extends Event {
        private final LivingEntity entity; private final MobEffectInstance instance;
        public Applicable(LivingEntity entity, MobEffectInstance instance) { this.entity=entity; this.instance=instance; }
        public LivingEntity getEntity() { return entity; }
        public MobEffectInstance getEffectInstance() { return instance; }
    }
}
