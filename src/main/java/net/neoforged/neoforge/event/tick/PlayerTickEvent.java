package net.neoforged.neoforge.event.tick;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
public abstract class PlayerTickEvent extends Event {
    private final Player entity;
    protected PlayerTickEvent(Player entity) { this.entity = entity; }
    public Player getEntity() { return entity; }
    public static final class Pre extends PlayerTickEvent { public Pre(Player player) { super(player); } }
    public static final class Post extends PlayerTickEvent { public Post(Player player) { super(player); } }
}
