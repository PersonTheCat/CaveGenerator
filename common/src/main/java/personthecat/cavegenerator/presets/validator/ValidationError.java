package personthecat.cavegenerator.presets.validator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

public class ValidationError {

    public static final String INDICATOR = "!!!";

    private final String key;
    private final Object[] args;

    public ValidationError(final String key, final Object... args) {
        this.key = key;
        this.args = args;
    }

    @Environment(EnvType.CLIENT)
    public List<String> format() {
        final List<String> comments = new ArrayList<>();
        final String msg = I18n.get(this.key, this.args);
        for (final String line : msg.split("\r?\n")) {
            comments.add(INDICATOR + line);
        }
        return comments;
    }
}
