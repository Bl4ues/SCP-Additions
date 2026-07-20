package net.neoforged.neoforge.event.tick;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
public abstract class LevelTickEvent extends Event {
    private final Level level;
    protected LevelTickEvent(Level level) { this.level = level; }
    public Level getLevel() { return level; }
    public static final class Pre extends LevelTickEvent { public Pre(Level level) { super(level); } }
    public static final class Post extends LevelTickEvent { public Post(Level level) { super(level); } }
}
