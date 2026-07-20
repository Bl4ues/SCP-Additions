from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()
JAVA = ROOT / 'src/main/java'
if not JAVA.exists():
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / 'src/main/java'


def write(rel: str, content: str) -> None:
    path = JAVA / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + '\n', encoding='utf-8')

# Fabric keeps the 1.21.1 Mojang mappings used by the NeoForge migration
# frontier. A compact compatibility surface preserves the existing common code
# while loader-specific callbacks are bridged from the Fabric entrypoints.
write('net/neoforged/api/distmarker/Dist.java', '''
package net.neoforged.api.distmarker;
public enum Dist { CLIENT, DEDICATED_SERVER }
''')
write('net/neoforged/api/distmarker/OnlyIn.java', '''
package net.neoforged.api.distmarker;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface OnlyIn { Dist value(); }
''')
write('net/neoforged/bus/api/SubscribeEvent.java', '''
package net.neoforged.bus.api;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
''')
write('net/neoforged/bus/api/EventPriority.java', '''
package net.neoforged.bus.api;
public enum EventPriority { HIGHEST, HIGH, NORMAL, LOW, LOWEST }
''')
write('net/neoforged/bus/api/Event.java', '''
package net.neoforged.bus.api;
public class Event {
    public enum Result { DENY, DEFAULT, ALLOW }
    private boolean canceled;
    private Result result = Result.DEFAULT;
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result == null ? Result.DEFAULT : result; }
}
''')
write('net/neoforged/bus/api/IEventBus.java', '''
package net.neoforged.bus.api;
import java.util.function.Consumer;
public interface IEventBus {
    void register(Object target);
    <T> void addListener(Consumer<T> listener);
    boolean post(Object event);
}
''')
write('net/neoforged/bus/api/SimpleEventBus.java', '''
package net.neoforged.bus.api;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SimpleEventBus implements IEventBus {
    private record Handler(Class<?> type, EventPriority priority, boolean receiveCanceled,
                           Object target, Method method, Consumer<Object> consumer) {}
    private final List<Handler> handlers = new CopyOnWriteArrayList<>();

    @Override
    public void register(Object target) {
        Class<?> type = target instanceof Class<?> c ? c : target.getClass();
        Object instance = target instanceof Class<?> ? null : target;
        for (Method method : type.getDeclaredMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null || method.getParameterCount() != 1) continue;
            if (instance == null && !Modifier.isStatic(method.getModifiers())) continue;
            method.setAccessible(true);
            handlers.add(new Handler(method.getParameterTypes()[0], annotation.priority(),
                    annotation.receiveCanceled(), instance, method, null));
        }
        handlers.sort(Comparator.comparingInt(h -> h.priority().ordinal()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addListener(Consumer<T> listener) {
        handlers.add(new Handler(Object.class, EventPriority.NORMAL, true, null, null,
                (Consumer<Object>) listener));
    }

    @Override
    public boolean post(Object event) {
        for (Handler handler : handlers) {
            if (!handler.type().isInstance(event) && handler.type() != Object.class) continue;
            if (event instanceof Event forgeEvent && forgeEvent.isCanceled()
                    && !handler.receiveCanceled()) continue;
            try {
                if (handler.consumer() != null) handler.consumer().accept(event);
                else handler.method().invoke(handler.target(), event);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                throw new RuntimeException(cause);
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }
        return event instanceof Event forgeEvent && forgeEvent.isCanceled();
    }
}
''')
write('net/neoforged/fml/common/Mod.java', '''
package net.neoforged.fml.common;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod { String value(); }
''')
write('net/neoforged/fml/common/EventBusSubscriber.java', '''
package net.neoforged.fml.common;
import java.lang.annotation.*;
import net.neoforged.api.distmarker.Dist;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    String modid() default "";
    Dist[] value() default {};
    Bus bus() default Bus.GAME;
    enum Bus { GAME, MOD }
}
''')
write('net/neoforged/fml/event/lifecycle/FMLCommonSetupEvent.java', '''
package net.neoforged.fml.event.lifecycle;
import net.neoforged.bus.api.Event;
public class FMLCommonSetupEvent extends Event {
    public void enqueueWork(Runnable work) { work.run(); }
}
''')
write('net/neoforged/fml/event/lifecycle/FMLClientSetupEvent.java', '''
package net.neoforged.fml.event.lifecycle;
import net.neoforged.bus.api.Event;
public class FMLClientSetupEvent extends Event {
    public void enqueueWork(Runnable work) { work.run(); }
}
''')
write('net/neoforged/fml/loading/FMLEnvironment.java', '''
package net.neoforged.fml.loading;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.api.distmarker.Dist;
public final class FMLEnvironment {
    public static Dist dist = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
            ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    private FMLEnvironment() {}
}
''')
write('net/neoforged/fml/loading/FMLPaths.java', '''
package net.neoforged.fml.loading;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
public enum FMLPaths {
    CONFIGDIR;
    public Path get() { return FabricLoader.getInstance().getConfigDir(); }
}
''')
write('net/neoforged/fml/ModList.java', '''
package net.neoforged.fml;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
public final class ModList {
    private static final ModList INSTANCE = new ModList();
    public static ModList get() { return INSTANCE; }
    public boolean isLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    public Optional<ModContainer> getModContainerById(String id) {
        return isLoaded(id) ? Optional.of(new ModContainer()) : Optional.empty();
    }
    public static final class ModContainer {
        public <T> void registerExtensionPoint(Class<T> type, T extension) { }
    }
}
''')
write('net/neoforged/neoforge/common/NeoForge.java', '''
package net.neoforged.neoforge.common;
import net.neoforged.bus.api.SimpleEventBus;
public final class NeoForge {
    public static final SimpleEventBus EVENT_BUS = new SimpleEventBus();
    private NeoForge() {}
}
''')
write('net/neoforged/neoforge/client/gui/IConfigScreenFactory.java', '''
package net.neoforged.neoforge.client.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
@FunctionalInterface
public interface IConfigScreenFactory { Screen create(Minecraft minecraft, Screen parent); }
''')

# Deferred registration is implemented directly on top of the vanilla registry.
write('net/neoforged/neoforge/registries/DeferredRegister.java', '''
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
''')

# Register all annotation-based subscribers without a classpath-scanning runtime
# dependency. The list is generated from the transformed source each build.
subscribers = []
for path in JAVA.rglob('*.java'):
    source = path.read_text(encoding='utf-8', errors='ignore')
    if '@EventBusSubscriber' not in source:
        continue
    package = re.search(r'^package\s+([\w.]+);', source, re.MULTILINE)
    clazz = re.search(r'\b(?:public\s+)?(?:final\s+)?class\s+(\w+)', source)
    annotation = re.search(r'@EventBusSubscriber(?:\((.*?)\))?', source, re.DOTALL)
    if not package or not clazz or not annotation:
        continue
    args = annotation.group(1) or ''
    bus = 'MOD' if 'Bus.MOD' in args else 'GAME'
    client = 'Dist.CLIENT' in args
    subscribers.append((package.group(1) + '.' + clazz.group(1), bus, client))

subscriber_lines = []
for name, bus, client in sorted(subscribers):
    subscriber_lines.append(
        f'        register("{name}", EventBusSubscriber.Bus.{bus}, {str(client).lower()});')
write('net/mcreator/scpadditions/fabric/FabricSubscriberBootstrap.java', f'''
package net.mcreator.scpadditions.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SimpleEventBus;

final class FabricSubscriberBootstrap {{
    private static SimpleEventBus modBus;
    private FabricSubscriberBootstrap() {{}}
    static void registerAll(SimpleEventBus bus) {{
        modBus = bus;
{chr(10).join(subscriber_lines)}
    }}
    private static void register(String name, EventBusSubscriber.Bus bus, boolean clientOnly) {{
        if (clientOnly && FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
        try {{
            Class<?> type = Class.forName(name, true, FabricSubscriberBootstrap.class.getClassLoader());
            (bus == EventBusSubscriber.Bus.MOD ? modBus : NeoForge.EVENT_BUS).register(type);
        }} catch (ClassNotFoundException exception) {{
            throw new IllegalStateException("Missing Fabric subscriber " + name, exception);
        }}
    }}
}}
''')
write('net/mcreator/scpadditions/fabric/ScpAdditionsFabric.java', '''
package net.mcreator.scpadditions.fabric;

import net.fabricmc.api.ModInitializer;
import net.neoforged.bus.api.SimpleEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpAdditionsFabric implements ModInitializer {
    public static final SimpleEventBus MOD_BUS = new SimpleEventBus();
    @Override
    public void onInitialize() {
        new ScpAdditionsMod(MOD_BUS);
        FabricSubscriberBootstrap.registerAll(MOD_BUS);
        MOD_BUS.post(new FMLCommonSetupEvent());
        FabricGameEventBridge.register();
    }
}
''')
write('net/mcreator/scpadditions/fabric/ScpAdditionsFabricClient.java', '''
package net.mcreator.scpadditions.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ScpAdditionsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScpAdditionsFabric.MOD_BUS.post(new FMLClientSetupEvent());
        FabricClientEventBridge.register();
    }
}
''')
write('net/mcreator/scpadditions/fabric/FabricGameEventBridge.java', '''
package net.mcreator.scpadditions.fabric;
final class FabricGameEventBridge {
    private FabricGameEventBridge() {}
    static void register() { }
}
''')
write('net/mcreator/scpadditions/fabric/FabricClientEventBridge.java', '''
package net.mcreator.scpadditions.fabric;
final class FabricClientEventBridge {
    private FabricClientEventBridge() {}
    static void register() { }
}
''')

print(f'Generated Fabric bridge core and {len(subscribers)} subscriber registrations')
