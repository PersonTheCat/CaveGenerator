package personthecat.cavegenerator.presets.validator;

import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.exception.DetailedPresetException;
import personthecat.cavegenerator.exception.UnusedFieldsException;

public class UnusedFieldLocator {

    private final JsonObject json;

    public UnusedFieldLocator(final JsonObject json) {
        this.json = (JsonObject) json.deepCopy(true);
        this.trim();
    }

    public boolean hasErrors() {
        return !this.json.isEmpty();
    }

    public DetailedPresetException createScreen(final String name) {
        return new UnusedFieldsException(name, this.json);
    }

    private void trim() {
        final JsonObject errors = HjsonUtils.skip(this.json, true);
        this.json.clear().addAll(errors);
    }
}
