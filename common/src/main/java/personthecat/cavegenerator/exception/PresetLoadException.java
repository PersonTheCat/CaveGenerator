package personthecat.cavegenerator.exception;

import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;

public abstract class PresetLoadException extends FormattedException {

    public PresetLoadException(final String msg) {
        super(msg);
    }

    public PresetLoadException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.presets";
    }
}
