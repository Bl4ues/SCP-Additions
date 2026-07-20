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
