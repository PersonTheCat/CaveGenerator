package com.personthecat.cavegenerator.config;

import lombok.AllArgsConstructor;
import org.hjson.HjsonOptions;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.CommonMethods.*;

public class ReferenceHelper {

    /** An equivalent formatter used for parsing functions without comments. */
    public static final HjsonOptions FORMATTER = new HjsonOptions()
        .setParseLegacyRoot(false)
        .setOutputComments(false)
        .setAllowCondense(true)
        .setAllowMultiVal(true)
        .setCommentSpace(0)
        .setSpace(2)
        .setBracesSameLine(true);

    /**
     * If <code>val</code> contains references, the values will be copied out of <code>from</code>.
     *
     * @param from The source where variables are defined.
     * @param s A string which may or may not contain references.
     * @return The substituted value, if necessary.
     */
    public static Optional<JsonValue> trySubstitute(JsonObject from, String s) {
        if (!Reference.containsReferences(s)) {
            return empty();
        }
        return full(Reference.replaceAll(from, s));
    }

    /**
     * Copies a value by key from a JSON object, asserting that one must exist.
     *
     * @throws RuntimeException If the JSON does not contain the expected key.
     * @param from The source where variables are defined.
     * @param ref A string which is known to be a reference.
     * @return The value contained within <code>from</code>.
     */
    public static JsonValue readValue(JsonObject from, String ref) {
        if (from.has(ref)) {
            return from.get(ref);
        }
        final String asFunction = ref + "()";
        if (from.has(asFunction)) {
            return from.get(asFunction);
        }
        throw runExF("Use of undeclared variable: {}", ref);
    }

    @AllArgsConstructor
    private static class Reference {
        /** A pattern used for testing whether a string contains references. */
        static final Pattern REFERENCE_PATTERN = Pattern.compile("(?<!\\\\)\\$(?!\\d)(\\w+)");

        final int start;
        final int end;
        final String key;
        final List<String> args;

        static boolean containsReferences(String val) {
            return REFERENCE_PATTERN.matcher(val).find();
        }

        /**
         * Substitutes all references in a string using data from the given JSON object.
         *
         * @param json The source where variables are defined.
         * @param val The raw string containing references.
         * @return A generated JSON value with substitutions in place.
         */
        static JsonValue replaceAll(JsonObject json, String val) {
            // Convert to and from JSON to allow in-place substitutions.
            String ret = val;
            do {
                final Reference r = create(ret);
                final String sub = Argument.generateValue(json, r);
                final String updated = ret.substring(0, r.start) + sub + ret.substring(r.end);
                if (ret.equals(updated)) throw runExF("Self reference: {}", sub);
                ret = updated;
            } while (containsReferences(ret));
            return JsonValue.readHjson(ret, FORMATTER);
        }

        static Reference create(String val) {
            final Matcher matcher = REFERENCE_PATTERN.matcher(val);
            if (!matcher.find()) {
                throw new IllegalStateException("No references");
            }
            final List<String> args = new ArrayList<>();
            final String key = matcher.group(1);
            final int start = matcher.start();
            int end = matcher.end();
            final int opening = findOpening(val, end);
            if (opening > -1) {
                final int closing = findClosing(val, opening, '(', ')');
                end = closing + 1;
                readArgs(val.substring(opening + 1, closing), args);
            }
            return new Reference(start, end, key, args);
        }

        static void readArgs(String val, List<String> args) {
            int i = 0;
            while (i < val.length()) {
                final char c = val.charAt(i);
                if (c != ' ' && c != '\t') {
                    final int closing;
                    final int read;
                    switch (c) {
                        case '[' :
                            final int bk = findClosing(val, i, '[', ']');
                            closing = read = findNext(val, bk,',', false);
                            break;
                        case '{' :
                            final int bc = findClosing(val, i, '{', '}');
                            closing = read = findNext(val, bc,',', false);
                            break;
                        case '(' :
                            final int pr = findClosing(val, i, '(', ')');
                            closing = read = findNext(val, pr,',', false);
                            break;
                        case '"' :
                            i++;
                            read = findNext(val, i, '"', true);
                            closing = findNext(val, read, ',', false);
                            break;
                        case '\'' :
                            i++;
                            read = findNext(val, i, '\'', true);
                            closing = findNext(val, read, ',', false);
                            break;
                        default :
                            closing = read = findNext(val, i, ',', false);
                    }
                    args.add(val.substring(i, read));
                    i = closing + 1;
                }
                i++;
            }
        }
    }

    @AllArgsConstructor
    private static class Argument {
        /** A pattern used for testing whether a string contains arguments. */
        static final Pattern ARGUMENT_PATTERN = Pattern.compile("@(\\d+)");

        final int start;
        final int end;
        final int index;
        final boolean optional;
        final String defaultVal;

        static boolean containsArgument(String val) {
            return ARGUMENT_PATTERN.matcher(val).find();
        }

        static int countArguments(String val) {
            final Matcher matcher = ARGUMENT_PATTERN.matcher(val);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        /**
         * Generates the raw string value of a variable reference with support for arguments.
         *
         * @param json The source where variables are defined.
         * @param r The parsed reference containing a key and arguments.
         * @return The raw generated string after processing variable substitution.
         */
        static String generateValue(JsonObject json, Reference r) {
            JsonValue value = readValue(json, r.key);
            if (value.isString()) {
                return replaceString(value.asString(), r);
            } else if (value.isObject()) {
                value = replaceObject(value.asObject(), r);
            } else if (value.isArray()) {
                value = replaceArray(value.asArray(), r);
            }
            return value.toString(FORMATTER);
        }

        static String replaceString(String val, Reference r) {
            final int numArguments = countArguments(val);
            String buffer = val;
            int skipped = 0;

            for (int i = 0; i < numArguments; i++) {
                final Argument a = create(buffer, skipped);
                String sub;
                if (r.args.size() <= a.index) {
                    if (!a.optional) {
                        throw new IllegalStateException("Missing argument: " + (a.index + 1));
                    }
                    sub = a.defaultVal.isEmpty() ? "" : replaceString(a.defaultVal, r);
                } else {
                    sub = r.args.get(a.index);
                }
                final int numParams = countArguments(sub);
                if (numParams == 1) {
                    // Export the default argument for this parameter.
                    final Argument param = create(sub, 0);
                    if (!a.defaultVal.isEmpty() && param.defaultVal.isEmpty()) {
                        sub += "(" + a.defaultVal + ")";
                    }
                }
                buffer = buffer.substring(0, a.start) + sub + buffer.substring(a.end);
                // Each argument in the substitution is a parameter.
                skipped += numParams;
            }
            return buffer;
        }

        static JsonObject replaceObject(JsonObject json, Reference r) {
            final JsonObject clone = new JsonObject();
            for (JsonObject.Member member : json) {
                final String name = replaceString(member.getName(), r);
                if (name.isEmpty()) continue;

                final JsonValue value = member.getValue();
                if (value.isString()) {
                    final String s = replaceString(value.asString(), r);
                    if (s.isEmpty()) continue;
                    clone.add(name, JsonValue.readHjson(quote(s), FORMATTER));
                } else if (value.isObject()) {
                    clone.add(name, replaceObject(value.asObject(), r));
                } else if (value.isArray()) {
                    clone.add(name, replaceArray(value.asArray(), r));
                } else {
                    clone.add(name, value);
                }
            }
            return clone;
        }

        static JsonArray replaceArray(JsonArray json, Reference r) {
            final JsonArray clone = new JsonArray();
            for (JsonValue value : json) {
                if (value.isString()) {
                    final String s = replaceString(value.asString(), r);
                    if (s.isEmpty()) continue;
                    clone.add(s);
                } else if (value.isObject()) {
                    clone.add(replaceObject(value.asObject(), r));
                } else if (value.isArray()) {
                    clone.add(replaceArray(value.asArray(), r));
                } else {
                    clone.add(value);
                }
            }
            return clone;
        }

        static Argument create(String val, int skipped) {
            final Matcher matcher = ARGUMENT_PATTERN.matcher(val);
            boolean found = matcher.find();
            for (int i = 0; i < skipped; i++) {
                found &= matcher.find();
            }
            if (!found) {
                throw new IllegalStateException("No arguments");
            }
            final int index = Integer.parseInt(matcher.group(1)) - 1;
            final int start = matcher.start();
            int end = matcher.end();
            boolean optional = false;
            String defaultVal = "";
            final int question = findQuestion(val, end);
            if (question > -1) {
                optional = true;
                end = question + 1;
                final int opening = findOpening(val, end);
                if (opening > -1) {
                    final int closing = findClosing(val, opening, '(', ')');
                    end = closing + 1;
                    defaultVal = val.substring(opening + 1, closing);
                }
            }
            return new Argument(start, end, index, optional, defaultVal);
        }
    }

    /**
     * Finds the first question mark (optional indicator) after the given index.
     *
     * @param val A raw string which may or may not contain parentheses.
     * @param index The first index to check for a parenthesis.
     * @return The index of the parenthesis, or else -1.
     */
    private static int findQuestion(String val, int index) {
        return findSpecialCharacter(val, index, '?');
    }

    /**
     * Finds the first opening parenthesis after the given index.
     *
     * @param val A raw string which may or may not contain parentheses.
     * @param index The first index to check for a parenthesis.
     * @return The index of the parenthesis, or else -1.
     */
    private static int findOpening(String val, int index) {
        return findSpecialCharacter(val, index, '(');
    }

    /**
     * Finds the first occurrence of the given character, ignoring whitespace. This
     * function will return -1 given another alphanumeric character comes first.
     *
     * @param val A raw string which may or may not contain parentheses.
     * @param index The first index to check for a parenthesis.
     * @param f The character to search for.
     * @return The index of the parenthesis, or else -1.
     */
    private static int findSpecialCharacter(String val, int index, char f) {
        for (int i = index; i < val.length(); i++) {
            final char c = val.charAt(i);
            if (c == f) return i;
            if (Character.isLetterOrDigit(c)) return -1;
        }
        return -1;
    }

    /**
     * Finds a balanced closing parenthesis matching the one at the current position.
     * This function does support escaped, quoted, and nested parentheses.
     *
     * @throws IllegalStateException if a balanced closer is not found.
     * @param val The raw string which is expected to contain a closer.
     * @param opening The index of the opening parenthesis.
     * @param open The character to be balanced with.
     * @param close The character being searched for.
     * @return The index of the closing parenthesis.
     */
    private static int findClosing(String val, int opening, char open, char close) {
        int numP = 0;
        boolean dq = false;
        boolean sq = false;
        boolean esc = false;
        for (int i = opening; i < val.length(); i++) {
            if (esc) {
                esc = false;
                continue;
            }
            final char c = val.charAt(i);
            if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                dq = !dq;
            } else if (c == '\'') {
                sq = !sq;
            } else if (c == open) {
                if (!dq && !sq) numP++;
            } else if (c == close) {
                if (!dq && !sq && --numP == 0) return i;
            }
        }
        throw new IllegalStateException("Missing " + close + " in function");
    }

    /**
     * Finds the next possible instance of this character, ignoring escaped characters.
     *
     * @param val The raw string being searched through.
     * @param index The starting index to search from.
     * @param f The exact character to find.
     * @param required Whether to throw an exception if the character is missing.
     * @return The index of this character, or else end of string.
     */
    private static int findNext(String val, int index, char f, boolean required) {
        boolean esc = false;
        int i = index;
        while(i < val.length()) {
            if (esc) {
                esc = false;
                i++;
                continue;
            }
            final char c = val.charAt(i);
            if (c == '\\') esc = true;
            else if (c == f) return i;

            if (f == '\"' || f == '\'') {
                i++;
                continue;
            }
            if (c == '(') i = findClosing(val, i, '(', ')');
            else if (c == '[') i = findClosing(val, i, '[', ']');
            else if (c == '{') i = findClosing(val, i, '{', '}');
            else i++;
        }
        if (required) {
            throw new IllegalStateException("Missing " + f + " in function");
        }
        return val.length();
    }


    /**
     * Determines whether the input text is string data and should be quoted.
     * If it is, returns the text in quotes.
     *
     * @param s The raw parameter value input to the template
     * @return The quoted text, else the original text.
     */
    private static String quote(String s) {
        final char first = s.charAt(0);
        final char last = s.charAt(s.length() - 1);
        if (Character.isWhitespace(first)) {
            final boolean single = containsRawDoubleQuote(s);
            // Only possible if the input was already quoted.
            return single ? '\'' + s + '\'' : '"' + s + '"';
        } else if ((first == '{' && last == '}') || (first == '[' && last == ']')) {
            return s;
        }
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                final boolean single = containsRawDoubleQuote(s);
                return single ? '\'' + s + '\'' : '"' + s + '"';
            }
        }
        return s;
    }

    /**
     * Determines whether the input text contains raw double quotes (") with
     * no escape characters.
     *
     * @param s The raw parameter value input to the template
     * @return Whether an unescaped double quote is found.
     */
    private static boolean containsRawDoubleQuote(String s) {
        boolean escaped = false;
        for (char c : s.toCharArray()) {
            if (c == '\\') escaped = true;
            else if (c == '"' && !escaped) return true;
            else escaped = false;
        }
        return false;
    }
}
