package com.personthecat.cavegenerator.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Creates a sort of lazily initialized value. Values wrapped in
 * this class will not exist until the first time they are used.
 * This is used anytime the wrapped value is not yet available
 * upon creation. Allows the field to be final up front.
 */
public class Lazy<T> {

    /** The underlying value being wrapped by this object. */
    private T value = null;

    /** A supplier used for producing the value when it is ready. */
    private final Supplier<T> supplier;

    /** The primary constructor with instructions for producing the value. */
    private Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /** To be used in the event that a value already exists. */
    private Lazy(@NotNull T value) {
        this.value = value;
        this.supplier = () -> null;
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    public static <T> Lazy<T> of(T value) {
        return new Lazy<>(value);
    }

    /** The primary method for retrieving the underlying value. */
    public T get() {
        if (value == null) {
            value = supplier.get();
            if (value == null) {
                throw new NullPointerException("Lazy value produced nothing.");
            }
        }
        return value;
    }

    /** Returns the value only if it has already been computed. */
    public Optional<T> getIfComputed() {
        return Optional.ofNullable(value);
    }

    /** Returns whether the underlying operation has completed. */
    public boolean computed() {
        return value != null;
    }
}