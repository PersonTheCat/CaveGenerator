package com.personthecat.cavegenerator.config;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class SyntaxHelper {

    /** A message informing the user that their comma is inside of a string. */
    private static final String COMMA_MESSAGE = "Found comma (,) in string. "
        + "Remove it or use double quotes. e.g. <\"value\",>\n"
        + "Source: {}";

    /** A message informing the user that their comment is inside of a string. */
    private static final String COMMENT_MESSAGE = "Found comment (# or //) in string. "
        + "Put your comment above this value or use double quotes. e.g. <\"value\" # comment>\n"
        + "Source: {}";

    /** A message reminding users not to use semicolons. */
    private static final String SEMICOLON_MESSAGE = "Found semicolon (;) in string. "
        + "You need to remove it.\n"
        + "Source: {}";

    /** Detects comments at the ends of strings.  */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("#|//|/\\*");
    
    /**
     * Recursively checks for a series of common syntax errors in presets.
     * This class is designed to provide more helpful crashes / error logs
     * than would otherwise occur when invalid strings a present in a file.
     *
     * @throws RuntimeException If an error is found.
     * @param file The file source of this JSON.
     * @param json The object being tested.
     */
    public static void check(File file, JsonObject json) {
        for (JsonObject.Member member : json) {
            final JsonValue value = member.getValue();
            if (value.isString()) {
                checkString(file, value.asString());
            } else if (value.isObject()) {
                check(file, value.asObject());
            } else if (value.isArray()) {
                checkArray(file, value.asArray());
            }
        }
    }

    /**
     * Recursively checks for syntax errors in an array.
     *
     * @throws RuntimeException If an error is found.
     * @param file The file source of this JSON.
     * @param array The array being tested.
     */
    private static void checkArray(File file, JsonArray array) {
        for (JsonValue value : array) {
            if (value.isString()) {
                checkString(file, value.asString());
            } else if (value.isObject()) {
                check(file, value.asObject());
            } else if (value.isArray()) {
                checkArray(file, value.asArray());
            }
        }
    }

    /**
     * Checks a single string value for common syntax errors.
     *
     * @throws RuntimeException If an error is found.
     * @param file The file source of this data.
     * @param string The string value being tested.
     */
    private static void checkString(File file, String string) {
        if (string.endsWith(",")) {
            throw runExF(COMMA_MESSAGE, file.getName());
        } else if (string.endsWith(";")) {
            throw runExF(SEMICOLON_MESSAGE, file.getName());
        } else if (COMMENT_PATTERN.matcher(string).find()) {
            throw runExF(COMMENT_MESSAGE, file.getName());
        }
    }
}
