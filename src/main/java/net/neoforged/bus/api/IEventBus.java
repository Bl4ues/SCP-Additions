package net.neoforged.bus.api;
import java.util.function.Consumer;
public interface IEventBus {
    void register(Object target);
    <T> void addListener(Consumer<T> listener);
    boolean post(Object event);
}
