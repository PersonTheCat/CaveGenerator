package personthecat.cavegenerator.world.feature;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.exception.MissingTemplateException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;
import static personthecat.cavegenerator.io.ModFolders.STRUCTURE_DIR;

@Log4j2
public class StructureSpawner {

    /**
     * Loads each structures out of <code>cavegenerator/structures</code> into a map.
     * This map will be used as the mod's main structure registry later on.
     *
     * @return A map of filename (no extension) -> template
     */
    public static Map<String, StructureTemplate> initStructures() {
        final Map<String, StructureTemplate> structures = new HashMap<>();

        for (final File f : listFiles(STRUCTURE_DIR, f -> "nbt".equals(extension(f)))) {
            final StructureTemplate template = loadStructure(f);
            warnIfLarge(template, f);
            structures.put(noExtension(f), template);
        }

        return structures;
    }

    /**
     * Loads a vanilla {@link StructureTemplate} from the disk as a .nbt file.
     *
     * @throws UncheckedIOException If the file does not load correctly.
     * @param nbt The file containing the serialized NBT structure data.
     * @return A regular {@link StructureTemplate}.
     */
    private static StructureTemplate loadStructure(final File nbt) {
        final StructureTemplate template = new StructureTemplate();
        try {
            template.load(NbtIo.readCompressed(nbt));
        } catch (final IOException e) {
            // Todo: configure crashing here.
            throw new UncheckedIOException("Loading " + nbt.getName(), e);
        }
        return template;
    }

    /**
     * Checks a template to make the size cannot cause cascading generation lag. If the feature
     * is wider than 15 blocks horizontally, a warning will be emitted to the log file.
     *
     * @param template The vanilla {@link StructureTemplate} being loaded from the disk.
     * @param nbt      The file which the structure was loaded from.
     */
    private static void warnIfLarge(final StructureTemplate template, final File nbt) {
        final BlockPos size = template.getSize();
        if (size.getX() > 15 || size.getZ() > 15) {
            log.warn("{} is >= 16 blocks wide. This may not work right.", nbt.getName());
        }
    }

    /**
     * Attempts to locate a {@link StructureTemplate} when given either a filename or a resource
     * location.
     *
     * @param id    The filename (no extension) <b>or</b> a resource ID.
     * @param level A server world providing access to the regular data pack structures.
     * @return A standard {@link StructureTemplate}, either from file or data pack.
     */
    public static StructureTemplate getTemplate(final String id, final ServerLevel level) {
        final StructureTemplate template = CaveRegistries.STRUCTURES.getOptional(id)
            .orElseGet(() -> level.getServer().getStructureManager().get(new ResourceLocation(id)));
        if (template == null) {
            throw new MissingTemplateException(id);
        }
        return template;
    }

    /**
     * Places the blocks from this structure in the world after sending some block updates.
     *
     * @param template The structure template being spawned in the world.
     * @param cfg      The settings used to create the structure.
     * @param level    The world target for the structure to spawn inside of.
     * @param pos      The corner coordinate of the structure being spawned.
     * @param rand     A RNG used for any random block placement needed.
     */
    public static void spawnStructure(final StructureTemplate template, final StructurePlaceSettings cfg,
            final WorldGenRegion level, final BlockPos pos, final Random rand) {

        level.blockUpdated(pos, level.getBlockState(pos).getBlock());
        template.placeInWorld(level, pos, cfg, rand);
    }
}
