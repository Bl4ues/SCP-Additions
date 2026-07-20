package net.neoforged.neoforge.event.tick;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
public abstract class EntityTickEvent extends Event {
    private final Entity entity;
    protected EntityTickEvent(Entity entity) { this.entity = entity; }
    public Entity getEntity() { return entity; }
    public static final class Post extends EntityTickEvent { public Post(Entity entity) { super(entity); } }
}
