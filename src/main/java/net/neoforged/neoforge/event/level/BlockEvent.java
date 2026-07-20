package net.neoforged.neoforge.event.level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
public class BlockEvent extends Event {
    private final LevelAccessor level; private final BlockPos pos; private final BlockState state;
    protected BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) { this.level=level; this.pos=pos; this.state=state; }
    public LevelAccessor getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public BlockState getState() { return state; }
    public static class BreakEvent extends BlockEvent {
        private final Player player;
        public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) { super(level,pos,state); this.player=player; }
        public Player getPlayer() { return player; }
    }
    public static class EntityPlaceEvent extends BlockEvent {
        private final Entity entity;
        public EntityPlaceEvent(LevelAccessor level, BlockPos pos, BlockState state, Entity entity) { super(level,pos,state); this.entity=entity; }
        public Entity getEntity() { return entity; }
        public BlockState getPlacedBlock() { return getState(); }
    }
    public static class NeighborNotifyEvent extends BlockEvent {
        public NeighborNotifyEvent(LevelAccessor level, BlockPos pos, BlockState state) { super(level,pos,state); }
    }
}
