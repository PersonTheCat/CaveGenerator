package personthecat.cavegenerator.commands;

import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.noise.CachedNoiseHelper;

@SuppressWarnings("unused") // Used by CatLib
public class CommandCave {

    @ModCommand(
        description = "Reloads all of the current presets from the disk."
    )
    private static void reload(final CommandContextWrapper wrapper) {
        CaveRegistries.PRESETS.reload();
        CaveRegistries.GENERATORS.reload();
        CaveRegistries.STRUCTURES.reload();
        CachedNoiseHelper.removeAll();
        wrapper.sendMessage("Successfully reloaded caves. View the log for diagnostics.");
    }
}