package com.personthecat.cavegenerator.util;

import lombok.AllArgsConstructor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A bare-bones Cave expression linter for displaying some JSON data in the chat.
 *
 * This class is <em>not intended</em> to be a foolproof utility. It is only
 * designed for a few scenarios and can highlight keys and documentation.
 */
public class CaveLinter {

    /** Identifies multiline documentation comments. Just because. */
    private static final Pattern MULTILINE_DOC = Pattern.compile("/\\*\\*[\\s\\S]*?\\*/", Pattern.DOTALL);

    /** Identifies multiline / inline comments to be highlighted. */
    private static final Pattern MULTILINE_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/", Pattern.DOTALL);

    /** Identifies todos in single line comments. Just because. */
    private static final Pattern LINE_TODO = Pattern.compile("(?:#|//).*(?:todo|to-do).*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    /** Identifies single line documentation comments. Just because. */
    private static final Pattern LINE_DOC = Pattern.compile("(?:#!|///).*$", Pattern.MULTILINE);

    /** Identifies single line comments to be highlighted. */
    private static final Pattern LINE_COMMENT = Pattern.compile("(?:#|//).*$", Pattern.MULTILINE);

    /** Identifies constant value keys to be highlighted. */
    private static final Pattern CONSTANT = Pattern.compile("[-_A-Z]+\\s*:|[-_\\w./]+\\s*::\\s*[-_A-Z]+|.*\\s[aA][sS]\\s+[-_A-Z]+", Pattern.MULTILINE);

    /** Identifies function keys to be highlighted. */
    private static final Pattern FUNCTION = Pattern.compile("\\b\\w+\\(\\)\\s*:|[-_\\w./]+\\s*::\\s*\\w+\\(\\)|.*\\s[aA][sS]\\s+\\w+\\(\\)", Pattern.MULTILINE);

    /** Identifies all other keys to be highlighted. */
    private static final Pattern KEY = Pattern.compile("(\"[\\w\\s]*\"|\\w+)\\s*:|[-_\\w./]+\\s*::\\s*\\w+|.*\\s[aA][sS]\\s+\\w+", Pattern.MULTILINE);

    /** Identifies function calls to be highlighted. */
    private static final Pattern CALL = Pattern.compile("\\B\\$\\w+\\((?:.*\\)\\B)?", Pattern.MULTILINE);

    /** Identifies all other references to be highlighted. */
    private static final Pattern REFERENCE = Pattern.compile("\\B\\$\\w+", Pattern.MULTILINE);

    /** Identifies function arguments to be highlighted. */
    private static final Pattern ARGUMENT = Pattern.compile("\\B@\\d+(?:\\s*\\?(?:\\s*\\(.*\\))?)?", Pattern.MULTILINE);

    public static ITextComponent lint(String text) {
        final ITextComponent formatted = new TextComponentString("");
        final Context ctx = new Context(text);

        Scope s;
        int i = 0;
        while ((s = ctx.next(i)) != null) {
            final int start = s.matcher.start();
            final int end = s.matcher.end();
            ctx.skipTo(end);

            if (start - i > 0) {
                // Append unformatted text;
                formatted.appendSibling(tcs(text.substring(i, start)));
            }
            formatted.appendSibling(tcs(text.substring(start, end)).setStyle(s.style));

            i = end;
        }

        return formatted.appendSibling(tcs(text.substring(i)));
    }

    private static ITextComponent tcs(String s) {
        return new TextComponentString(s);
    }

    private static class Context {
        static final Target[] TARGETS = {
            new Target(MULTILINE_DOC, new Style().setColor(TextFormatting.DARK_GREEN).setItalic(true)),
            new Target(LINE_TODO, new Style().setColor(TextFormatting.YELLOW)),
            new Target(LINE_DOC, new Style().setColor(TextFormatting.DARK_GREEN).setItalic(true)),
            new Target(MULTILINE_COMMENT, new Style().setColor(TextFormatting.GRAY)),
            new Target(LINE_COMMENT, new Style().setColor(TextFormatting.GRAY)),
            new Target(CONSTANT, new Style().setColor(TextFormatting.DARK_PURPLE)),
            new Target(FUNCTION, new Style().setColor(TextFormatting.GOLD)),
            new Target(KEY, new Style().setColor(TextFormatting.AQUA)),
            new Target(CALL, new Style().setColor(TextFormatting.GOLD).setUnderlined(true)),
            new Target(REFERENCE, new Style().setColor(TextFormatting.DARK_PURPLE).setUnderlined(true)),
            new Target(ARGUMENT, new Style().setColor(TextFormatting.DARK_GREEN).setUnderlined(true))
        };

        final List<Scope> scopes = new ArrayList<>();
        final String text;

        Context(String text) {
            this.text = text;
            for (Target t : TARGETS) {
                final Matcher matcher = t.pattern.matcher(text);
                this.scopes.add(new Scope(matcher, t.style, matcher.find()));
            }
        }

        @Nullable
        Scope next(int i) {
            // Figure out whether any other matches have been found;
            int start = Integer.MAX_VALUE;
            Scope first = null;
            for (Scope s : this.scopes) {
                if (!s.found) continue;
                final int mStart = s.matcher.start();

                if (mStart >= i && mStart < start) {
                    start = mStart;
                    first = s;
                }
            }
            return first;
        }

        void skipTo(int i) {
            for (Scope s : this.scopes) {
                if (!s.found) continue;
                if (s.matcher.end() <= i) {
                    s.next();
                }
            }
        }
    }

    @AllArgsConstructor
    private static class Target {
        final Pattern pattern;
        final Style style;
    }

    @AllArgsConstructor
    private static class Scope {
        final Matcher matcher;
        final Style style;
        boolean found;

        void next() {
            this.found = this.matcher.find();
        }
    }
}
