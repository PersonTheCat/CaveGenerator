package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.hjson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class UnusedFieldsException extends DetailedPresetException {

    public UnusedFieldsException(final String name, final JsonObject details) {
        super(name, details);
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.unusedFields";
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.xUnusedFields", this.name);
    }
}
