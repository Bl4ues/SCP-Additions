package net.neoforged.neoforge.capabilities;
import java.util.function.BiFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
public class RegisterCapabilitiesEvent extends Event {
    public <T extends BlockEntity, C> void registerBlockEntity(Object capability,
            BlockEntityType<T> type, BiFunction<T, Direction, C> provider) {}
}
