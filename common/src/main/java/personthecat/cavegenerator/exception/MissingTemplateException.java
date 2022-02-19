package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.FormattedException;

import static personthecat.catlib.util.Shorthand.f;

public class MissingTemplateException extends FormattedException {

    private final String name;

    public MissingTemplateException(final String name) {
        super(f("No template named \"{}\" was found. This must be a filename or resource ID.", name));
        this.name = name;
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.structureErrors";
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.name);
    }

    @Override
    public @Nullable Component getTooltip() {
        return new TranslatableComponent("cg.errorText.missingStructure");
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.xMissingStructure", this.name);
    }
}
