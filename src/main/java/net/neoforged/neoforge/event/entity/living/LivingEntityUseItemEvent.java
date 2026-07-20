package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
public class LivingEntityUseItemEvent extends Event {
    private final LivingEntity entity; private final ItemStack item;
    protected LivingEntityUseItemEvent(LivingEntity entity, ItemStack item) { this.entity=entity; this.item=item; }
    public LivingEntity getEntity() { return entity; }
    public ItemStack getItem() { return item; }
    public static class Start extends LivingEntityUseItemEvent { public Start(LivingEntity e, ItemStack s) { super(e,s); } }
    public static class Finish extends LivingEntityUseItemEvent { public Finish(LivingEntity e, ItemStack s) { super(e,s); } }
}
