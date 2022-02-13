package personthecat.cavegenerator.presets.lang;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static personthecat.catlib.util.Shorthand.f;

public class SyntaxHelper {

    /** A message informing the user that their comma is inside a string. */
    private static final String COMMA_MESSAGE = "Found comma (,) in string. "
        + "Remove it or use double quotes. e.g. <\"value\",>\n"
        + "Value: \"{}\"";

    /** A message informing the user that their comment is inside a string. */
    private static final String COMMENT_MESSAGE = "Found comment (# or //) in string. "
        + "Put your comment above this value or use double quotes. e.g. <\"value\" # comment>\n"
        + "Value: \"{}\"";

    /** A message reminding users not to use semicolons. */
    private static final String SEMICOLON_MESSAGE = "Found semicolon (;) in string. "
        + "You need to remove it.\n"
        + "Value: \"{}\"";

    /** Detects comments at the ends of strings.  */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("#|//|/\\*");

    /**
     * Recursively checks for a series of common syntax errors in presets.
     * This class is designed to provide more helpful crashes / error logs
     * than would otherwise occur when invalid strings a present in a file.
     *
     * @param json The object being tested.
     */
    public static List<String> getExtraneousTokens(final JsonObject json) {
        final List<String> tokens = new ArrayList<>();
        check(tokens, json);
        return tokens;
    }
    
    /**
     * Recursively checks for syntax errors in an object.
     *
     * @param tokens An ongoing list of extraneous tokens.
     * @param json   The object being tested.
     */
    public static void check(final List<String> tokens, final JsonObject json) {
        for (final JsonObject.Member member : json) {
            final JsonValue value = member.getValue();
            if (value.isString()) {
                checkString(tokens, value.asString());
            } else if (value.isObject()) {
                check(tokens, value.asObject());
            } else if (value.isArray()) {
                checkArray(tokens, value.asArray());
            }
        }
    }

    /**
     * Recursively checks for syntax errors in an array.
     *
     * @param tokens An ongoing list of extraneous tokens.
     * @param array  The array being tested.
     */
    private static void checkArray(final List<String> tokens,final JsonArray array) {
        for (JsonValue value : array) {
            if (value.isString()) {
                checkString(tokens, value.asString());
            } else if (value.isObject()) {
                check(tokens, value.asObject());
            } else if (value.isArray()) {
                checkArray(tokens, value.asArray());
            }
        }
    }

    /**
     * Checks a single string value for common syntax errors.
     *
     * @param tokens An ongoing list of extraneous tokens.
     * @param string The string value being tested.
     */
    private static void checkString(final List<String> tokens, final String string) {
        if (string.endsWith(",")) {
            tokens.add(f(COMMA_MESSAGE, string));
        } else if (string.endsWith(";")) {
            tokens.add(f(SEMICOLON_MESSAGE, string));
        } else if (COMMENT_PATTERN.matcher(string).find()) {
            tokens.add(f(COMMENT_MESSAGE, string));
        }
    }
}
