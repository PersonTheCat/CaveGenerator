package personthecat.cavegenerator;

import personthecat.catlib.command.LibCommandRegistrar;
import personthecat.cavegenerator.commands.CommandCave;
import personthecat.cavegenerator.config.Cfg;
import personthecat.cavegenerator.io.JarFiles;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustInherit;

@OverwriteTarget(required = true)
public class CaveGenerator {

    @PlatformMustInherit
    public void initCommon() {
        JarFiles.copyFiles();
        Cfg.register();
        LibCommandRegistrar.registerCommands(Reference.MOD_DESCRIPTOR, true, CommandCave.class);
    }
}
