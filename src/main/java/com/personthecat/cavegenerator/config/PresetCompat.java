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
public class PresetCompat {
    static Result<IOException> update(JsonObject json, File file) {
        getObject(json, "tunnels").ifPresent(PresetCompat::renameAngles);
        getObject(json, "ravines").ifPresent(PresetCompat::renameAngles);
        rename(json, "clusters", "clusters");
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
}
