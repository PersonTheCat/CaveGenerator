package personthecat.cavegenerator.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.PathUtils;

import java.io.File;

public class CorruptPresetException extends PresetLoadException {

    private final String name;

    public CorruptPresetException(final String name, final Throwable cause) {
        super(name, cause);
        this.name = name;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.getMessage());
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("cg.errorText.xPresetCorrupt", this.name);
    }

    @Override
    public @Nullable Component getTooltip() {
        return new TranslatableComponent("cg.errorText.presetCorrupt");
    }
}
