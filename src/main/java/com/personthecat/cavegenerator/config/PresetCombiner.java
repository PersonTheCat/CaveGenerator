package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.PathComponent;
import org.apache.commons.lang3.CharUtils;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.io.SafeFileIO.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/** Used for merging JsonObject paths between json files. */
public class PresetCombiner {

    /**
     * Responsible for loading all necessary data for merging JsonObjects.
     *
     * @param from The complete path to the value being copied.
     * @param to The name of the preset being written into.
     */
    public static void combine(String from, String to) {
        // Load all data.
        final int endOfName = from.indexOf('.');
        final File fileFrom = getRequiredPreset(from.substring(0, endOfName));
        final File fileTo = getRequiredPreset(to);
        final JsonObject objFrom = getRequiredJson(fileFrom);
        final JsonObject objTo = getRequiredJson(fileTo);
        // Process the rest of the path.
        final List<PathComponent> components;
        try {
            components = processPath(from.substring(endOfName + 1));
        } catch (IOException e) {
            throw runEx(e.getMessage());
        }
        // Create a backup and use the data.
        backup(fileTo);
        run(components, objFrom, objTo, fileTo);
    }

    private static File getRequiredPreset(String path) {
        return CaveInit.locatePreset(path)
            .orElseThrow(() -> runExF("Unable to locate a preset named \"{}\"", path));
    }

    private static JsonObject getRequiredJson(File file) {
        return PresetReader.getPresetJson(file)
            .orElseThrow(() -> runExF("{} does not contain a valid json or hjson object.", file.getName()));
    }

    /**
     * Primary method for merging objects.
     *
     * @param components The parsed path tree leading to the desired value.
     * @param from The JSON object source being copied from.
     * @param to The JSON object target being written into.
     * @param fileTo The file being written into.
     */
    private static void run(List<PathComponent> components, JsonObject from, JsonObject to, File fileTo) {
        JsonValue fromValue = getLastContainer(from, components);
        if (fromValue.isObject()) {
            final String key = components.get(components.size() - 1).key
                .orElseThrow(() -> runEx("Expected an object at end of path."));
            fromValue = fromValue.asObject().get(key);
        } else if (fromValue.isArray()) {
            final int index = components.get(components.size() - 1).index
                .orElseThrow(() -> runEx("Expected an array at end of path."));
            fromValue = fromValue.asArray().get(index);
        }
        setValueFromPath(to, components, fromValue);
        writeJson(to, fileTo).throwIfPresent();
    }

    /**
     * Converts this path into a complete path tree for easier use.
     *
     * @throws IOException If the Reader in use errs.
     * @param path The raw path data.
     * @return A parsed path tree leading to the desired value.
     */
    private static List<PathComponent> processPath(String path) throws IOException {
        final PushbackReader reader = new PushbackReader(new StringReader(path));
        final List<PathComponent> components = new ArrayList<>();
        final StringBuilder currentPath = new StringBuilder();

        // The first component is always a key.
        final String first = readString(reader, currentPath);
        components.add(PathComponent.key(first));
        currentPath.append(first);

        while (reader.ready()) {
            final int current = reader.read();
            currentPath.append((char) current);
            if (current == -1 || current == 65535) { // invalid push back -> EOF.
                break;
            } else if (current == '.') {
                final String key = readString(reader, currentPath);
                components.add(PathComponent.key(key));
                currentPath.append(key);
            } else if (current == '[') {
                final int index = readInt(reader, currentPath);
                components.add(PathComponent.index(index));
                currentPath.append(index);
                expect(reader, path, ']');
                currentPath.append(']');
            } else {
                throw runExF("Invalid character ({}) in path: {} <- Here", (char) current, currentPath);
            }
        }
        reader.close();
        return components;
    }

    private static String readString(PushbackReader reader, CharSequence path) throws IOException {
        final StringBuilder sb = new StringBuilder();
        int current;
        while (CharUtils.isAsciiAlphanumeric((char) (current = reader.read()))) {
            sb.append((char) current);
        }
        if (sb.length() == 0) {
            throw runExF("Expected key in path: {} <- Here", path);
        }
        // We read an invalid character. Put it back.
        reader.unread(current);
        return sb.toString();
    }

    private static int readInt(PushbackReader reader, CharSequence path) throws IOException {
        final StringBuilder sb = new StringBuilder();
        int current;
        while (CharUtils.isAsciiNumeric((char) (current = reader.read()))) {
            sb.append((char) current);
        }
        if (sb.length() == 0) {
            throw runExF("Expected index in path: {} <- Here", path);
        }
        // We read an invalid character. Put it back.
        reader.unread(current);
        final int parsed = Integer.parseInt(sb.toString());
        if (parsed <= 0) {
            throw runExF("Invalid index ({}) in path: {} <- Here", parsed, path);
        }
        return parsed;
    }

    private static void expect(PushbackReader reader, CharSequence path, char ch) throws IOException {
        if (reader.read() != ch) {
            throw runExF("Expected {} in path: {} <- Here", ch, path);
        }
    }
}