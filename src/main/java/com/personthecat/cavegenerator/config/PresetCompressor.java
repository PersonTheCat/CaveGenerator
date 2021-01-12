package com.personthecat.cavegenerator.config;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Optional;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class PresetCompressor {

    /**
     * Compresses this object slightly. For now, this only implies compressing
     * single element arrays into individual values.
     *
     * @param json The JSON object being compressed.
     */
    public static void compress(JsonObject json) {
        final JsonObject clone = new JsonObject();
        for (JsonObject.Member member : json) {
            final String name = member.getName();
            final JsonValue value = member.getValue();
            clone.add(name, value);
            if (value.isArray()) {
                compressSingle(value.asArray()).ifPresent(single -> clone.set(name, single));
            } else if (value.isObject()) {
                compress(value.asObject());
            }
        }
        json.clear();
        json.addAll(clone);
    }

    private static void compressArray(JsonArray array) {
        final JsonArray clone = new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            final JsonValue value = array.get(i);
            clone.add(value);
            if (value.isArray()) {
                final Optional<JsonValue> single = compressSingle(value.asArray());
                if (single.isPresent()) {
                    clone.set(i, single.get());
                }
            } else if (value.isObject()) {
                compress(value.asObject());
            }
        }
        array.clear();
        array.addAll(clone);
    }

    private static Optional<JsonValue> compressSingle(JsonArray array) {
        if (array.size() == 1) {
            final JsonValue single = array.get(0);
            if (single.isObject()) {
                compress(single.asObject());
            } else if (single.isArray()) {
                compressArray(single.asArray());
            }
            return full(single);
        } else if (!array.isEmpty()) {
            compressArray(array);
        }
        return empty();
    }
}
