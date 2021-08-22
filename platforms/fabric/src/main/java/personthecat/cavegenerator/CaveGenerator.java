package personthecat.cavegenerator;

import net.fabricmc.api.ModInitializer;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@OverwriteClass
public class CaveGenerator implements ModInitializer {

    @Override
    public void onInitialize() {
        this.initCommon();
    }

    @Inherit
    public void initCommon() {
        throw new MissingOverrideException();
    }
}
