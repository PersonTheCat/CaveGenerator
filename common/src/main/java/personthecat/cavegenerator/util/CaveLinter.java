package personthecat.cavegenerator.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import personthecat.catlib.util.SyntaxLinter;

import java.util.regex.Pattern;

/**
 * A bare-bones Cave expression linter for displaying some JSON data in the chat.
 *
 * This class is <em>not intended</em> to be a foolproof utility. It is only
 * designed for a few scenarios and can highlight keys and documentation.
 */
public class CaveLinter extends SyntaxLinter {

    /** Identifies constant value keys to be highlighted. */
    private static final Pattern CONSTANT = Pattern.compile("(\\\"[A-Z\\s._-]*\\\"|[A-Z._-]+)\\s*(?=:)|[-_\\w./]+\\s*(?:::|[aA][sS])\\s*[A-Z._-]+(.*\\s[aA][sS]\\s+[A-Z._-]+)?", Pattern.MULTILINE);

    /** Identifies function keys to be highlighted. */
    private static final Pattern FUNCTION = Pattern.compile("\\b\\w+\\(\\)\\s*:|[-_\\w./]+\\s*::\\s*\\w+\\(\\)|.*\\s[aA][sS]\\s+\\w+\\(\\)", Pattern.MULTILINE);

    /** Identifies function calls to be highlighted. */
    private static final Pattern CALL = Pattern.compile("\\B\\$\\w+\\((?:.*\\)\\B)?", Pattern.MULTILINE);

    /** Identifies all other references to be highlighted. */
    private static final Pattern REFERENCE = Pattern.compile("\\B\\$\\w+", Pattern.MULTILINE);

    /** Identifies function arguments to be highlighted. */
    private static final Pattern ARGUMENT = Pattern.compile("\\B@\\d+(?:\\s*\\?(?:\\s*\\(.*\\))?)?", Pattern.MULTILINE);

    private static final Target[] TARGETS = {
        new Target(MULTILINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new Target(LINE_TODO, color(ChatFormatting.YELLOW)),
        new Target(LINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new Target(MULTILINE_COMMENT, color(ChatFormatting.GRAY)),
        new Target(LINE_COMMENT, color(ChatFormatting.GRAY)),
        new Target(CONSTANT, color(ChatFormatting.DARK_PURPLE)),
        new Target(FUNCTION, color(ChatFormatting.GOLD)),
        new Target(KEY, color(ChatFormatting.AQUA)),
        new Target(CALL, color(ChatFormatting.GOLD).withUnderlined(true)),
        new Target(REFERENCE, color(ChatFormatting.DARK_PURPLE).withUnderlined(true)),
        new Target(ARGUMENT, color(ChatFormatting.DARK_GREEN).withUnderlined(true)),
        new Target(BOOLEAN_VALUE, color(ChatFormatting.GOLD)),
        new Target(NUMERIC_VALUE, color(ChatFormatting.LIGHT_PURPLE)),
        new Target(NULL_VALUE, color(ChatFormatting.RED))
    };

    @SuppressWarnings("unused") // Reflective invocation
    public CaveLinter() {
        super(TARGETS);
    }
}
