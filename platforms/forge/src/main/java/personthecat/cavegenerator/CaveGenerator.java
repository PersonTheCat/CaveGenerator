package personthecat.cavegenerator;

import net.minecraftforge.fml.common.Mod;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.cavegenerator.util.Reference;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
@Mod(Reference.MOD_ID)
public class CaveGenerator {

    public CaveGenerator() {
        this.initCommon();
    }

    @Inherit
    public void initCommon() {
        throw new MissingOverrideException();
    }
}
