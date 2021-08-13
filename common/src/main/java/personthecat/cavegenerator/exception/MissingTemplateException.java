package personthecat.cavegenerator.exception;

import static personthecat.catlib.util.Shorthand.f;

public class MissingTemplateException extends RuntimeException {
    public MissingTemplateException(final String name) {
        super(f("No template named \"{}\" was found. This must be a filename or resource ID.", name));
    }
}
