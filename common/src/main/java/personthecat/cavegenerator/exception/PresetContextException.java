package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.FormattedException;

public class PresetContextException extends FormattedException {

    public PresetContextException(final Throwable cause) {
        super(cause);
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.presetErrors";
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TranslatableComponent("cg.errorText.presetContext");
    }

    @Override
    public @Nullable Component getTooltip() {
        return new TextComponent(this.getMessage());
    }
}
