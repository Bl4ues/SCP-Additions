package net.neoforged.neoforge.event.entity.item;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
public class ItemTossEvent extends Event {
    private final ItemEntity entity; private final Player player;
    public ItemTossEvent(ItemEntity entity, Player player) { this.entity = entity; this.player = player; }
    public ItemEntity getEntity() { return entity; }
    public Player getPlayer() { return player; }
}
