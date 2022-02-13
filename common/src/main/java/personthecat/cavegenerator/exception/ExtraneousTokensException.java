package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.hjson.JsonObject;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.util.CaveLinter;

import java.util.List;

public class ExtraneousTokensException extends PresetLoadException {

    private final String name;
    private final List<String> tokens;
    private final JsonObject json;

    public ExtraneousTokensException(final String name, final List<String> tokens, final JsonObject json) {
        super(name);
        this.name = name;
        this.tokens = tokens;
        this.json = json;
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.extraTokens";
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.name);
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.xExtraTokens", this.name);
    }

    @Override
    public @NotNull Component getDetailMessage() {
        final MutableComponent msg = new TextComponent("");
        for (final String token : this.tokens) {
            msg.append(token);
            msg.append("\n");
        }
        msg.append("\n");
        msg.append(CaveLinter.INSTANCE.lint(this.json.toString(HjsonUtils.NO_CR)));
        return msg;
    }
}
