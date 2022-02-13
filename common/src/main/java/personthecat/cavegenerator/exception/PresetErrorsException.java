package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.hjson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class PresetErrorsException extends DetailedPresetException {

    public PresetErrorsException(final String name, final JsonObject details) {
        super(name, details);
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.presetErrors";
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.xPresetErrors", this.name);
    }
}
