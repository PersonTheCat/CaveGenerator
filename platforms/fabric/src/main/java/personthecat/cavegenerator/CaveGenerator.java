package personthecat.cavegenerator;

import net.fabricmc.api.ModInitializer;
import personthecat.catlib.command.LibCommandRegistrar;
import personthecat.cavegenerator.commands.CommandCave;
import personthecat.cavegenerator.util.Reference;

public class CaveGenerator implements ModInitializer {

    @Override
    public void onInitialize() {
        LibCommandRegistrar.registerCommands(Reference.MOD_DESCRIPTOR, true, CommandCave.class);
    }
}
