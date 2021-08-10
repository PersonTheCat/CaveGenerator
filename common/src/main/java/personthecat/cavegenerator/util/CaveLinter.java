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
    private static final Pattern CONSTANT = Pattern.compile("[-_A-Z]+\\s*:|[-_\\w./]+\\s*::\\s*[-_A-Z]+|.*\\s[aA][sS]\\s+[-_A-Z]+", Pattern.MULTILINE);

    /** Identifies function keys to be highlighted. */
    private static final Pattern FUNCTION = Pattern.compile("\\b\\w+\\(\\)\\s*:|[-_\\w./]+\\s*::\\s*\\w+\\(\\)|.*\\s[aA][sS]\\s+\\w+\\(\\)", Pattern.MULTILINE);

    /** Identifies function calls to be highlighted. */
    private static final Pattern CALL = Pattern.compile("\\B\\$\\w+\\((?:.*\\)\\B)?", Pattern.MULTILINE);

    /** Identifies all other references to be highlighted. */
    private static final Pattern REFERENCE = Pattern.compile("\\B\\$\\w+", Pattern.MULTILINE);

    /** Identifies function arguments to be highlighted. */
    private static final Pattern ARGUMENT = Pattern.compile("\\B@\\d+(?:\\s*\\?(?:\\s*\\(.*\\))?)?", Pattern.MULTILINE);

    private static final Target[] TARGETS = {
        new Target(MULTILINE_DOC, Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withItalic(true)),
        new Target(LINE_TODO, Style.EMPTY.withColor(ChatFormatting.YELLOW)),
        new Target(LINE_DOC, Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withItalic(true)),
        new Target(MULTILINE_COMMENT, Style.EMPTY.withColor(ChatFormatting.GRAY)),
        new Target(LINE_COMMENT, Style.EMPTY.withColor(ChatFormatting.GRAY)),
        new Target(CONSTANT, Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)),
        new Target(FUNCTION, Style.EMPTY.withColor(ChatFormatting.GOLD)),
        new Target(KEY, Style.EMPTY.withColor(ChatFormatting.AQUA)),
        new Target(CALL, Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true)),
        new Target(REFERENCE, Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withUnderlined(true)),
        new Target(ARGUMENT, Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withUnderlined(true))
    };

    public CaveLinter() {
        super(TARGETS);
    }
}
