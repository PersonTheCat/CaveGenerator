package personthecat.cavegenerator.io;

import lombok.experimental.UtilityClass;
import personthecat.catlib.util.McUtils;
import personthecat.cavegenerator.util.Reference;

import java.io.File;

@UtilityClass
public class ModFolders {
    public static final File CG_DIR = new File(McUtils.getConfigDir(), Reference.MOD_ID);
    public static final File BACKUP_DIR = new File(CG_DIR, "backups");
    public static final File EXAMPLE_DIR = new File(CG_DIR, "examples");
    public static final File PRESET_DIR = new File(CG_DIR, "presets");
    public static final File IMPORT_DIR = new File(CG_DIR, "imports");
    public static final File GENERATED_DIR = new File(CG_DIR, "generated");
    public static final File STRUCTURE_DIR = new File(CG_DIR, "structures");

    public static File root(final String name) {
        return new File(CG_DIR, name);
    }

    public static File examples(final String name) {
        return new File(EXAMPLE_DIR, name);
    }

    public static File presets(final String name) {
        return new File(PRESET_DIR, name);
    }

    public static File imports(final String name) {
        return new File(IMPORT_DIR, name);
    }

    public static File generated(final String name) {
        return new File(GENERATED_DIR, name);
    }

    public static File structures(final String name) {
        return new File(STRUCTURE_DIR, name);
    }
}
