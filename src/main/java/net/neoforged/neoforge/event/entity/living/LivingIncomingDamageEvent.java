package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
public class LivingIncomingDamageEvent extends Event {
    private final LivingEntity entity; private final DamageSource source; private float amount;
    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) { this.entity=entity; this.source=source; this.amount=amount; }
    public LivingEntity getEntity() { return entity; }
    public DamageSource getSource() { return source; }
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount=amount; }
}
