package personthecat.cavegenerator.util;

import net.minecraft.ChatFormatting;
import personthecat.catlib.util.SyntaxLinter;

import java.util.regex.Pattern;

/**
 * A bare-bones Cave expression linter for displaying some JSON data in the chat.
 *
 * This class is <em>not intended</em> to be a foolproof utility. It is only
 * designed for a few scenarios and can highlight keys and documentation.
 */
public class CaveLinter extends SyntaxLinter {

    private static final Pattern CONSTANT = Pattern.compile("(\\\"[A-Z\\s._-]*\\\"|[A-Z._-]+)\\s*(?=:)|[-_\\w./]+\\s*(?:::|[aA][sS])\\s*[A-Z._-]+(.*\\s[aA][sS]\\s+[A-Z._-]+)?", Pattern.MULTILINE);
    private static final Pattern FUNCTION = Pattern.compile("\\b\\w+\\(\\)\\s*:|[-_\\w./]+\\s*::\\s*\\w+\\(\\)|.*\\s[aA][sS]\\s+\\w+\\(\\)", Pattern.MULTILINE);
    private static final Pattern CALL = Pattern.compile("\\B\\$\\w+\\((?:.*\\)\\B)?", Pattern.MULTILINE);
    private static final Pattern REFERENCE = Pattern.compile("\\B\\$\\w+", Pattern.MULTILINE);
    private static final Pattern ARGUMENT = Pattern.compile("\\B@\\d+(?:\\s*\\?(?:\\s*\\(.*\\))?)?", Pattern.MULTILINE);

    private static final Highlighter[] HIGHLIGHTERS = {
        new RegexHighlighter(MULTILINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(LINE_TODO, color(ChatFormatting.YELLOW)),
        new RegexHighlighter(LINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(MULTILINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(LINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(CONSTANT, color(ChatFormatting.DARK_PURPLE)),
        new RegexHighlighter(FUNCTION, color(ChatFormatting.GOLD)),
        new RegexHighlighter(KEY, color(ChatFormatting.AQUA)),
        new RegexHighlighter(CALL, color(ChatFormatting.GOLD).withUnderlined(true)),
        new RegexHighlighter(REFERENCE, color(ChatFormatting.DARK_PURPLE).withUnderlined(true)),
        new RegexHighlighter(ARGUMENT, color(ChatFormatting.DARK_GREEN).withUnderlined(true)),
        new RegexHighlighter(BOOLEAN_VALUE, color(ChatFormatting.GOLD)),
        new RegexHighlighter(NUMERIC_VALUE, color(ChatFormatting.LIGHT_PURPLE)),
        new RegexHighlighter(NULL_VALUE, color(ChatFormatting.RED)),
        new RegexHighlighter(MULTILINE_KEY, MULTILINE_KEY_ERROR, true),
        new RegexHighlighter(BAD_CLOSER, BAD_CLOSER_ERROR),
        UnbalancedTokenHighlighter.INSTANCE
    };

    @SuppressWarnings("unused") // Reflective invocation
    public CaveLinter() {
        super(HIGHLIGHTERS);
    }
}
