package net.neoforged.neoforge.event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.Event;
public class RegisterCommandsEvent extends Event {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher) { this.dispatcher=dispatcher; }
    public CommandDispatcher<CommandSourceStack> getDispatcher() { return dispatcher; }
}
