package net.neoforged.neoforge.event.entity.living;
import java.util.Collection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.Event;
public class LivingDropsEvent extends Event {
    private final LivingEntity entity; private final Collection<ItemEntity> drops;
    public LivingDropsEvent(LivingEntity entity, Collection<ItemEntity> drops) { this.entity=entity; this.drops=drops; }
    public LivingEntity getEntity() { return entity; }
    public Collection<ItemEntity> getDrops() { return drops; }
}
