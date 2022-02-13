package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.PathUtils;
import personthecat.catlib.util.SyntaxLinter;
import personthecat.cavegenerator.util.CaveLinter;

import java.io.File;

public class PresetSyntaxException extends PresetLoadException {

    private final String text;
    private final String name;

    public PresetSyntaxException(final String name, final String text, final Throwable cause) {
        super(name, cause);
        this.text = text;
        this.name = name;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.getMessage());
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.xPresetInvalid", this.name);
    }

    @Override
    public @Nullable Component getTooltip() {
        return new TranslatableComponent("cg.errorText.presetInvalid");
    }

    @Override
    public @NotNull Component getDetailMessage() {
        return CaveLinter.INSTANCE.lint(this.text.replace("\t", "  ").replace("\r", ""));
    }
}
