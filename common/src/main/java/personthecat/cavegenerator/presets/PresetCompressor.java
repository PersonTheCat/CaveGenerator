package personthecat.cavegenerator.presets;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Optional;

import static java.util.Optional.empty;
import static personthecat.catlib.util.Shorthand.full;

public class PresetCompressor {

    /**
     * Compresses this object slightly. For now, this only implies compressing
     * single element arrays into individual values.
     *
     * @param json The JSON object being compressed.
     */
    public static JsonObject compress(final JsonObject json) {
        final JsonObject clone = new JsonObject();
        for (final JsonObject.Member member : json) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            if (value.isArray()) {
                clone.add(name, compressSingle(value.asArray()).orElse(value));
            } else if (value.isObject()) {
                clone.add(name, compress(value.asObject()));
            } else {
                clone.add(name, value);
            }
        }
        return clone;
    }

    private static JsonArray compressArray(final JsonArray array) {
        final JsonArray clone = new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            final JsonValue value = array.get(i);
            if (value.isArray()) {
                clone.add(compressSingle(value.asArray()).orElse(value));
            } else if (value.isObject()) {
                clone.add(compress(value.asObject()));
            } else {
                clone.add(value);
            }
        }
        return clone;
    }

    private static Optional<JsonValue> compressSingle(final JsonArray array) {
        if (array.size() == 1) {
            final JsonValue single = array.get(0);
            if (single.isObject()) {
                return full(compress(single.asObject()));
            } else if (single.isArray()) {
                return full(compressArray(single.asArray()));
            }
            return full(single);
        } else if (!array.isEmpty()) {
            return full(compressArray(array));
        }
        return empty();
    }
}
