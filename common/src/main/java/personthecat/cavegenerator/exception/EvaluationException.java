package personthecat.cavegenerator.exception;

import static personthecat.catlib.util.Shorthand.f;

public class EvaluationException extends RuntimeException {
    public EvaluationException(final String msg) {
        super(msg);
    }

    public EvaluationException(final String msg, final Object... args) {
        this(f(msg, args));
    }
}
