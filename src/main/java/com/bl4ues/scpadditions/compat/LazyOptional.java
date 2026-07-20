package com.bl4ues.scpadditions.compat;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Small source-compatibility wrapper for the Forge LazyOptional API removed in
 * NeoForge. Values are resolved lazily and are not invalidated because data
 * attachments are owned directly by their holder.
 */
public final class LazyOptional<T> {
    private static final LazyOptional<?> EMPTY = new LazyOptional<>(null, false);

    private final Supplier<? extends T> supplier;
    private final boolean present;

    private LazyOptional(Supplier<? extends T> supplier, boolean present) {
        this.supplier = supplier;
        this.present = present;
    }

    public static <T> LazyOptional<T> of(Supplier<? extends T> supplier) {
        return new LazyOptional<>(Objects.requireNonNull(supplier), true);
    }

    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> empty() {
        return (LazyOptional<T>) EMPTY;
    }

    public boolean isPresent() {
        return present && supplier.get() != null;
    }

    public void ifPresent(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        if (present) {
            T value = supplier.get();
            if (value != null) consumer.accept(value);
        }
    }

    public T orElse(T fallback) {
        if (!present) return fallback;
        T value = supplier.get();
        return value == null ? fallback : value;
    }

    public T orElseThrow() {
        T value = orElse(null);
        if (value == null) throw new NoSuchElementException("No value present");
        return value;
    }

    public <R> LazyOptional<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        if (!present) return empty();
        return of(() -> {
            T value = supplier.get();
            return value == null ? null : mapper.apply(value);
        });
    }

    public Optional<T> resolve() {
        return Optional.ofNullable(present ? supplier.get() : null);
    }

    @SuppressWarnings("unchecked")
    public <R> LazyOptional<R> cast() {
        return (LazyOptional<R>) this;
    }

    public void invalidate() {
        // Attachments follow the holder lifetime; no manual invalidation needed.
    }
}
