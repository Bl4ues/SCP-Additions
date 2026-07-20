package net.neoforged.neoforge.client.event;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.Event;
public final class RegisterKeyMappingsEvent extends Event {
    public void register(KeyMapping mapping) { KeyBindingHelper.registerKeyBinding(mapping); }
}
