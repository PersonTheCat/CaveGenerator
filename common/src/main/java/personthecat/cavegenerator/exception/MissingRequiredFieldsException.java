package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.hjson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class MissingRequiredFieldsException extends DetailedPresetException {

    public MissingRequiredFieldsException(final String name, final JsonObject details) {
        super(name, details);
    }

    @Override
    public @NotNull String getCategory() {
        return "cg.errorMenu.requiredFields";
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorMenu.xRequiredFields", this.name);
    }
}
