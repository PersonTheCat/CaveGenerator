package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.PathUtils;

import java.io.File;

public class InvalidPresetArgumentException extends PresetLoadException {

    private final String msg;
    private final String name;

    public InvalidPresetArgumentException(final String name, final Throwable cause) {
        super(name, cause);
        this.msg = cause.getMessage();
        this.name = name;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.getMessage());
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.invalidArgument", this.name);
    }

    @Override
    public @Nullable Component getTooltip() {
        return new TextComponent(this.msg);
    }
}
