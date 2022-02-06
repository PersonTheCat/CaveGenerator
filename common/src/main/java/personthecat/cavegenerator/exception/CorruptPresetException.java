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

    public CorruptPresetException(final File root, final File file, final Throwable cause) {
        super(PathUtils.getRelativePath(root, file), cause);
        this.name = file.getName();
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
