package net.neoforged.neoforge.registries;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public final class DeferredRegister<T> {
    private final Registry<T> registry;
    private final String namespace;
    private final Map<String, DeferredValue<? extends T>> entries = new LinkedHashMap<>();
    private boolean registered;

    private DeferredRegister(Registry<T> registry, String namespace) {
        this.registry = registry;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
        return new DeferredRegister<>(registry, namespace);
    }

    @SuppressWarnings("unchecked")
    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key,
                                                   String namespace) {
        Registry<T> registry = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY
                .get(key.location());
        if (registry == null) throw new IllegalStateException("Missing registry " + key.location());
        return new DeferredRegister<>(registry, namespace);
    }

    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> factory) {
        DeferredValue<I> value = new DeferredValue<>(factory);
        entries.put(name, value);
        if (registered) value.bind(Registry.register(registry,
                ResourceLocation.fromNamespaceAndPath(namespace, name), factory.get()));
        return value;
    }

    public void register(IEventBus ignored) {
        if (registered) return;
        registered = true;
        entries.forEach((name, value) -> value.bindRaw(Registry.register(registry,
                ResourceLocation.fromNamespaceAndPath(namespace, name), value.create())));
    }

    public Collection<Supplier<T>> getEntries() {
        return entries.values().stream().map(v -> (Supplier<T>) v).toList();
    }

    private static final class DeferredValue<I> implements Supplier<I> {
        private final Supplier<? extends I> factory;
        private I value;
        private DeferredValue(Supplier<? extends I> factory) { this.factory = factory; }
        private I create() { return factory.get(); }
        private void bind(I value) { this.value = value; }
        @SuppressWarnings("unchecked") private void bindRaw(Object value) { this.value = (I) value; }
        @Override public I get() {
            if (value == null) throw new IllegalStateException("Deferred value accessed before registration");
            return value;
        }
    }
}
