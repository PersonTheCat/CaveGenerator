package personthecat.cavegenerator.exception;

import static personthecat.catlib.util.Shorthand.f;

public class CaveSyntaxException extends RuntimeException {
    public CaveSyntaxException(final String msg) {
        super(msg);
    }

    public CaveSyntaxException(final String msg, final Object... args) {
        this(f(msg, args));
    }

    public CaveSyntaxException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    public static CaveSyntaxException caveSyntax(final String msg, final Object... args) {
        return new CaveSyntaxException(msg, args);
    }

    public static CaveSyntaxException caveSyntax(final String msg, final Throwable cause) {
        return new CaveSyntaxException(msg, cause);
    }
}
