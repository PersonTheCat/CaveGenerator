package com.personthecat.cavegenerator.util;

import java.util.Optional;
import java.util.function.Function;

import static com.personthecat.cavegenerator.util.CommonMethods.nullable;

/**
 * Counterpart to {@link Lazy} which requires an input. Acts as a sort
 * of function which only runs once.
 */
public class LazyFunction<T, R> {

    /** The underlying value being wrapped by this object. */
    private R value = null;

    /** A supplier used for producing the value when it is ready. */
    private final Function<T, R> func;

    /** The primary constructor with instructions for producing the value. */
    public LazyFunction(Function<T, R> func) {
        this.func = func;
    }

    /** To be used in the event that a value already exists. */
    public LazyFunction(R value) {
        this.value = value;
        this.func = r -> null;
    }

    /** The primary method for retrieving the underlying value. */
    public R apply(T t) {
        if (value == null) {
            value = func.apply(t);
            if (value == null) {
                throw new NullPointerException("Lazy value produced nothing.");
            }
        }
        return value;
    }

    /** Returns the value only if it has already been computed. */
    public Optional<R> getIfComputed() {
        return nullable(value);
    }

    /** Returns whether the underlying operation has completed. */
    public boolean computed() {
        return value != null;
    }
}