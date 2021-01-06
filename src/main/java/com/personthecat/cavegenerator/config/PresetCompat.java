package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.util.Result;
import org.hjson.JsonArray;
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
    static Result<IOException> update(JsonObject json, File file) {
        getObject(json, "tunnels").ifPresent(PresetCompat::renameAngles);
        getObject(json, "ravines").ifPresent(PresetCompat::renameAngles);
        rename(json, "stoneClusters", "clusters");
        removeBlankSlate(json);
        return writeJson(json, file);
    }

    private static void renameAngles(JsonObject json) {
        rename(json, "angleXZ", "yaw");
        rename(json, "angleY", "pitch");
        rename(json, "twistXZ", "dYaw");
        rename(json, "twistY", "dPitch");
    }

    private static void rename(JsonObject json, String from, String to) {
        final JsonValue get = json.get(from);
        if (get != null) {
            json.set(to, get);
            json.remove(from);
        }
    }

    private static void removeBlankSlate(JsonObject json) {
        if (json.has("blankSlate")) {
            // User did *not* want a blank slate.
            if (!json.get("blankSlate").asBoolean()) {
                // Todo: Maybe remove these inserts and just enforce
                // that imports always be at the top of the file.
                // Todo: Check for and add to prior imports if they exist.
                json.insert(0, "$VANILLA", JsonValue.valueOf("ALL"), "Default ravines and lava settings.");
                json.insert(0,"imports", new JsonArray()
                    .add("defaults.cave::VANILLA")
                    .setCondensed(false));
            }
            json.remove("blankSlate");
        }
    }
}
