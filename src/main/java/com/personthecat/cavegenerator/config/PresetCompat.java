package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.util.Result;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.IOException;

import static com.personthecat.cavegenerator.util.HjsonTools.*;

/**
 * This is a temporary class designed to extend compatibility of deprecated
 * fields and notations until they can safely be phased out. It will handle
 * updating these fields to their new format until the next major update.
 */
class PresetCompat {

    /**
     * This function takes care of any operation related to renaming and
     * updating old variables, as well as enforcing that imports be moved
     * to the top of each file.
     *
     * Todo: track whether changes occur before overwriting.
     *
     * @param json The parsed JSON object to be updated.
     * @param file The file source of this object.
     * @return Whether an exception took place when writing the file.
     */
    static Result<IOException> update(JsonObject json, File file) {
        getValue(json, "tunnels").ifPresent(PresetCompat::renameAngles);
        getValue(json, "ravines").ifPresent(PresetCompat::renameAngles);
        rename(json, "stoneClusters", "clusters");
        removeBlankSlate(json);
        enforceValueOrder(json);
        return writeJson(json, file);
    }

    /**
     * This is a reusable function for renaming angles in tunnels and ravines.
     * The value may be an object or an array.
     *
     * @param value The JSON value to be updated.
     */
    private static void renameAngles(JsonValue value) {
        if (value.isObject()) {
            renameAngles(value.asObject());
        } else if (value.isArray()) {
            for (JsonValue e : value.asArray()) {
                if (e.isObject()) {
                    renameAngles(e.asObject());
                }
            }
        }
    }

    /**
     * For renaming tunnels and ravines when the value is a known object.
     *
     * @param json The JSON object to be updated.
     */
    private static void renameAngles(JsonObject json) {
        rename(json, "angleXZ", "yaw");
        rename(json, "angleY", "pitch");
        rename(json, "twistXZ", "dYaw");
        rename(json, "twistY", "dPitch");
    }

    /**
     * Renames a value in a JSON object, if present.
     *
     * @param json The JSON object to be updated.
     * @param from The original key name.
     * @param to The new key name.
     */
    private static void rename(JsonObject json, String from, String to) {
        final JsonValue get = json.get(from);
        if (get != null) {
            json.set(to, get);
            json.remove(from);
        }
    }

    /**
     * Replaces any instance of <code>blankSlate: false</code> with the following
     * values: <code>
     *   $VANILLA: ALL
     * </code>
     * <p>
     *  If <code>blankSlate</code> is set to <code>true</code>, it is simply removed,
     *  as this is the default behavior.
     * </p>
     *
     * @param json The JSON object to be updated.
     */
    private static void removeBlankSlate(JsonObject json) {
        if (json.has("blankSlate")) {
            // User did *not* want a blank slate.
            if (!json.get("blankSlate").asBoolean()) {
                final JsonValue all = JsonValue.valueOf("ALL")
                    .setEOLComment("Default ravines and lava settings.");
                setOrAdd(json, "$VANILLA", all);
            }
            json.remove("blankSlate");
        }
    }

    /**
     * Ensures that any imports array be placed at the top of a file, and any field
     * being merged at the root level go just below it.
     *
     * @param json The JSON object to be updated.
     */
    private static void enforceValueOrder(JsonObject json) {
        final JsonObject top = new JsonObject();
        final JsonObject bottom = new JsonObject();

        moveValue("imports", json, top);
        moveValue("variables", json, top);
        for (JsonObject.Member member : json) {
            if (member.getName().startsWith("$")) {
                top.add(member.getName(), member.getValue());
            } else {
                bottom.add(member.getName(), member.getValue());
            }
        }
        json.clear();
        json.addAll(top);
        json.addAll(bottom);
    }

    /**
     * Moves a JSON member from one object to another.
     *
     * @param field The field key being moved.
     * @param from The JSON source being copied from.
     * @param to The JSON target being copied into.
     */
    private static void moveValue(String field, JsonObject from, JsonObject to) {
        if (from.has(field)) {
            to.add(field, from.get(field));
            from.remove(field);
        }
    }
}
