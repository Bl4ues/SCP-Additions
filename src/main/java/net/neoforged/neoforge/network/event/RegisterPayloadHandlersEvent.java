package net.neoforged.neoforge.network.event;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
public class RegisterPayloadHandlersEvent extends Event {
    public PayloadRegistrar registrar(String version) { return new PayloadRegistrar(); }
}
