package personthecat.cavegenerator.presets.reader;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import personthecat.cavegenerator.mixin.BlockRotProcessorAccessor;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.ofEnum;
import static personthecat.catlib.serialization.DynamicField.field;

public class StructureSettingsReader {

    public static final Codec<StructurePlaceSettings> CODEC = dynamic(StructurePlaceSettings::new).create(
        field(Codec.FLOAT, "integrity", StructureSettingsReader::getIntegrity, StructureSettingsReader::setIntegrity),
        field(ofEnum(Mirror.class), "mirror", StructurePlaceSettings::getMirror, StructurePlaceSettings::setMirror),
        field(Codec.BOOL, "ignoreEntities", StructurePlaceSettings::isIgnoreEntities, StructurePlaceSettings::setIgnoreEntities),
        field(Codec.BOOL, "hasGravity", StructureSettingsReader::hasGravity, StructureSettingsReader::setHasGravity)
    );

    public static float getIntegrity(final StructurePlaceSettings placement) {
        for (final StructureProcessor processor : placement.getProcessors()) {
            if (processor instanceof BlockRotProcessor) {
                return ((BlockRotProcessorAccessor) processor).getIntegrity();
            }
        }
        return 1.0F;
    }

    public static void setIntegrity(final StructurePlaceSettings placement, final float integrity) {
        placement.getProcessors().removeIf(p -> p instanceof BlockRotProcessor);
        placement.getProcessors().add(new BlockRotProcessor(integrity));
    }

    public static boolean hasGravity(final StructurePlaceSettings placement) {
        for (final StructureProcessor processor : placement.getProcessors()) {
            if (processor instanceof GravityProcessor) {
                return true;
            }
        }
        return false;
    }

    public static void setHasGravity(final StructurePlaceSettings placement, final boolean hasGravity) {
        placement.getProcessors().removeIf(p -> p instanceof GravityProcessor);
        placement.getProcessors().add(new GravityProcessor(Heightmap.Types.MOTION_BLOCKING, 1));
    }
}
