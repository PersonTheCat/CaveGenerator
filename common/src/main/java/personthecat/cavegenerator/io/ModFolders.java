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
}
