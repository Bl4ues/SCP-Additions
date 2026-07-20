package net.neoforged.neoforge.event.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.util.TriState;

public class PlayerInteractEvent extends Event {
    private final Player entity;
    protected PlayerInteractEvent(Player entity) { this.entity = entity; }
    public Player getEntity() { return entity; }
    public static final class RightClickBlock extends PlayerInteractEvent {
        private final InteractionHand hand; private final BlockHitResult hit;
        private InteractionResult cancellationResult = InteractionResult.PASS;
        private TriState useBlock = TriState.DEFAULT, useItem = TriState.DEFAULT;
        public RightClickBlock(Player player, InteractionHand hand, BlockHitResult hit) { super(player); this.hand = hand; this.hit = hit; }
        public InteractionHand getHand() { return hand; }
        public Level getLevel() { return getEntity().level(); }
        public BlockPos getPos() { return hit.getBlockPos(); }
        public Direction getFace() { return hit.getDirection(); }
        public BlockHitResult getHitVec() { return hit; }
        public ItemStack getItemStack() { return getEntity().getItemInHand(hand); }
        public void setCancellationResult(InteractionResult result) { cancellationResult = result; }
        public InteractionResult getCancellationResult() { return cancellationResult; }
        public void setUseBlock(TriState state) { useBlock = state; }
        public void setUseItem(TriState state) { useItem = state; }
        public TriState getUseBlock() { return useBlock; }
        public TriState getUseItem() { return useItem; }
    }
    public static final class RightClickItem extends PlayerInteractEvent {
        private final InteractionHand hand; private InteractionResult cancellationResult = InteractionResult.PASS;
        public RightClickItem(Player player, InteractionHand hand) { super(player); this.hand = hand; }
        public Level getLevel() { return getEntity().level(); }
        public ItemStack getItemStack() { return getEntity().getItemInHand(hand); }
        public InteractionHand getHand() { return hand; }
        public void setCancellationResult(InteractionResult result) { cancellationResult = result; }
        public InteractionResult getCancellationResult() { return cancellationResult; }
    }
    public static final class EntityInteract extends PlayerInteractEvent {
        private final Entity target; private InteractionResult cancellationResult = InteractionResult.PASS;
        public EntityInteract(Player player, Entity target) { super(player); this.target = target; }
        public Entity getTarget() { return target; }
        public void setCancellationResult(InteractionResult result) { cancellationResult = result; }
        public InteractionResult getCancellationResult() { return cancellationResult; }
    }
}
