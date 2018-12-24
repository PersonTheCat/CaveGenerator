package com.personthecat.cavegenerator.util;

import java.util.Optional;
import java.util.function.Consumer;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

/**
 * A counterpart to java.util.Optional used for neatly handling errors.
 *
 * It would be a great idea to not execute the underlying process until
 * a method such as `except()`, `andThen()` or `ignore()` is called, as
 * this would ensure that result gets handled in some way; however, this
 * class is mainly intended to be more of a good measure that also helps
 * in avoiding runtime crashes. So long as the errors *do* get handled,
 * it has served its purpose.
 */
public class Result<E extends Throwable> {
    /** A static field to avoid unnecessary instantiation. */
    private static final Result<?> OK = new Result<>();

    /** May be null to indicate that no error was returned. */
    private final E err;

    /** A private constructor so that err is correctly defined. */
    private Result() {
        this.err = null;
    }

    /** Returns an instance with no error. See Optional#empty. */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Result<E> ok() {
        return (Result<E>) OK;
    }

    /** Constructs a Result object, ensuring that an error is present. */
    private Result(E err) {
        if (err == null) {
            throw runEx("Error: Attempted to construct an error with no value present.");
        } else {
            this.err = err;
        }
    }

    /** Constructs a Result object when an error is known to exist. */
    public static <E extends Throwable> Result<E> of(E err) {
        return new Result<>(err);
    }

    /** Constructs a Result object when an error is not known to exist.*/
    public static <E extends Throwable> Result<E> ofUnknown(E err) {
        return err == null ? ok() : of(err);
    }

    /** Returns whether an underlying error is present. */
    public boolean isErr() {
        return err != null;
    }

    /** Returns whether no underlying error is present. */
    public boolean isOk() {
        return err == null;
    }

    /** Calls Consumer function if error is present. */
    public Result<E> handleIfPresent(Consumer<E> func) {
        if (err != null) {
            func.accept(err);
        }
        return this;
    }

    /** Runs a function if no error is present. */
    public Result<E> andThen(Runnable func) {
        if (err == null) {
            func.run();
        }
        return this;
    }

    /** Throws the underlying exception, if present. */
    public void throwIfPresent() {
        if (err != null) {
            throw new RuntimeException(err);
        }
    }

    /** Throws the underlying exception with an error message. */
    public void expect(String message) {
        if (err != null) {
            throw new RuntimeException(message, err);
        }
    }

    /** Throws the underlying exception with a formatted error message. */
    public void expectF(String message, Object... args) {
        if (err != null) {
            throw new RuntimeException(String.format(message, args), err);
        }
    }

    /** Returns the exception's error, if applicable. */
    public Optional<String> getMessage() {
        return Optional.of(err.getMessage());
    }
}