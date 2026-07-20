package net.neoforged.neoforge.event.entity.player;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

public class PlayerEvent extends Event {
    private final Player entity;
    protected PlayerEvent(Player entity) { this.entity = entity; }
    public Player getEntity() { return entity; }
    public static class PlayerLoggedInEvent extends PlayerEvent { public PlayerLoggedInEvent(Player p) { super(p); } }
    public static class PlayerLoggedOutEvent extends PlayerEvent { public PlayerLoggedOutEvent(Player p) { super(p); } }
    public static class PlayerRespawnEvent extends PlayerEvent { public PlayerRespawnEvent(Player p) { super(p); } }
    public static class PlayerChangedDimensionEvent extends PlayerEvent { public PlayerChangedDimensionEvent(Player p) { super(p); } }
    public static class Clone extends PlayerEvent {
        private final Player original; private final boolean wasDeath;
        public Clone(Player clone, Player original, boolean wasDeath) { super(clone); this.original = original; this.wasDeath = wasDeath; }
        public Player getOriginal() { return original; }
        public boolean isWasDeath() { return wasDeath; }
    }
    public static class StartTracking extends PlayerEvent {
        private final Entity target;
        public StartTracking(Player player, Entity target) { super(player); this.target = target; }
        public Entity getTarget() { return target; }
    }
    public static class ItemCraftedEvent extends PlayerEvent {
        private final Container inventory;
        public ItemCraftedEvent(Player player, Container inventory) { super(player); this.inventory = inventory; }
        public Container getInventory() { return inventory; }
    }
    public static class BreakSpeed extends PlayerEvent {
        private final BlockState state; private final Optional<BlockPos> position;
        private final float originalSpeed; private float newSpeed;
        public BreakSpeed(Player player, BlockState state, Optional<BlockPos> position, float originalSpeed) {
            super(player); this.state = state; this.position = position; this.originalSpeed = originalSpeed; this.newSpeed = originalSpeed;
        }
        public BlockState getState() { return state; }
        public Optional<BlockPos> getPosition() { return position; }
        public float getOriginalSpeed() { return originalSpeed; }
        public float getNewSpeed() { return newSpeed; }
        public void setNewSpeed(float speed) { this.newSpeed = speed; }
    }
}
