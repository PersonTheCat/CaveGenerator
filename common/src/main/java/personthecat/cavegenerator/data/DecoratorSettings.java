package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class DecoratorSettings {

    /** The placeholder name of this feature. */
    private static final String FEATURE_NAME = "<decorator>";

    /** Common default settings to be used by all world carvers. */
    public static final DecoratorSettings DEFAULTS = builder().build();

    /** All of the blocks which can be replaced by this decorator. */
    @Default List<BlockState> replaceableBlocks =
        Arrays.asList(Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.GRASS.defaultBlockState());

    /** Whether to include the blocks from various other features in this list. */
    @Default boolean replaceDecorators = true;

    /** Whether to indiscriminately replace all non-bedrock blocks. */
    @Default boolean replaceSolidBlocks = false;

    /** A list of blocks for this carver to place instead of air. */
    @Default List<CaveBlockSettings> caveBlocks = Collections.emptyList();

    /** A list of blocks to replace the walls of this carver with. */
    @Default List<WallDecoratorSettings> wallDecorators = Collections.emptyList();

    /** A variant of wall decorators which can spawn multiple layers deep on the floor only. */
    @Default List<PondSettings> ponds = Collections.emptyList();

    /** A variant of wall decorators that can spawn multiple layers deep without directionality. */
    @Default ShellSettings shell = ShellSettings.builder().build();

    /** An internal-only list of decorators from <em>everywhere else</em>. */
    @Default List<BlockState> globalDecorators = Collections.emptyList();

    private static final HjsonMapper<DecoratorSettingsBuilder, DecoratorSettings> MAPPER =
        new HjsonMapper<>(FEATURE_NAME, DecoratorSettingsBuilder::build)
            .mapStateList(Fields.replaceableBlocks, DecoratorSettingsBuilder::replaceableBlocks)
            .mapBool(Fields.replaceDecorators, DecoratorSettingsBuilder::replaceDecorators)
            .mapBool(Fields.replaceSolidBlocks, DecoratorSettingsBuilder::replaceSolidBlocks)
            .mapArray(Fields.caveBlocks, CaveBlockSettings::from, DecoratorSettingsBuilder::caveBlocks)
            .mapArray(Fields.wallDecorators, WallDecoratorSettings::from, DecoratorSettingsBuilder::wallDecorators)
            .mapArray(Fields.ponds, PondSettings::from, DecoratorSettingsBuilder::ponds)
            .mapObject(Fields.shell, (b, o) -> b.shell(ShellSettings.from(o)));

    public static DecoratorSettings from(final JsonObject json, final DecoratorSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static DecoratorSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static DecoratorSettings copyInto(final JsonObject json, final DecoratorSettingsBuilder builder) {
        return MAPPER.create(builder, json);
    }
}
