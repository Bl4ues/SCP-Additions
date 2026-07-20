package net.neoforged.neoforge.registries;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
public class RegisterEvent extends Event {
    public <T> void register(ResourceKey<? extends Registry<T>> key, ResourceLocation id, Supplier<T> supplier) {
        @SuppressWarnings("unchecked") Registry<T> registry = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(key.location());
        Registry.register(registry, id, supplier.get());
    }
}
