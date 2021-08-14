package personthecat.cavegenerator;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import personthecat.catlib.command.LibCommandRegistrar;
import personthecat.cavegenerator.commands.CommandCave;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.util.Reference;

public class CaveGenerator implements ModInitializer {

    @Override
    public void onInitialize() {
        AutoConfig.register(Cfg.class, GsonConfigSerializer::new);
        LibCommandRegistrar.registerCommands(Reference.MOD_DESCRIPTOR, true, CommandCave.class);
    }
}
