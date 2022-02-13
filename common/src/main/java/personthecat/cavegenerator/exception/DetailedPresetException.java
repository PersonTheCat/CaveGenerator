package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.hjson.JsonObject;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.util.HjsonUtils;
import personthecat.cavegenerator.util.DetailedPresetLinter;

public abstract class DetailedPresetException extends FormattedException {

    protected final String name;
    protected final JsonObject details;

    protected DetailedPresetException(final String name, final JsonObject details) {
        super(name);
        this.name = name;
        this.details = details;
    }

    @Override
    public abstract @NotNull String getCategory();

    @Override
    public abstract @NotNull Component getTitleMessage();

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.name);
    }

    @Override
    public @NotNull Component getDetailMessage() {
        return DetailedPresetLinter.INSTANCE.lint(this.details.toString(HjsonUtils.NO_CR));
    }
}
