package net.neoforged.neoforge.event.entity.player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.util.TriState;
public final class ItemEntityPickupEvent {
    private ItemEntityPickupEvent() {}
    public static class Pre extends Event {
        private final Player player; private final ItemEntity item; private TriState canPickup = TriState.DEFAULT;
        public Pre(Player player, ItemEntity item) { this.player = player; this.item = item; }
        public Player getPlayer() { return player; }
        public ItemEntity getItemEntity() { return item; }
        public void setCanPickup(TriState state) { canPickup = state; }
        public TriState getCanPickup() { return canPickup; }
    }
}
