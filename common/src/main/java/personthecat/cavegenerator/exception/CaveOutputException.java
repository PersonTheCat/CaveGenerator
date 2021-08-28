package personthecat.cavegenerator.exception;

import java.io.IOException;

public class CaveOutputException extends RuntimeException {
    public CaveOutputException(final IOException wrapped) {
        super(wrapped);
    }
}
