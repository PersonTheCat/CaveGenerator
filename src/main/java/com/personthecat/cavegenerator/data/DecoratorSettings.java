package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@FieldNameConstants
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class DecoratorSettings {

    /** Common default settings to be used by all world carvers. */
    public static final DecoratorSettings DEFAULTS = builder().build();

    /** All of the blocks which can be replaced by this decorator. */
    @Default List<IBlockState> replaceableBlocks =
        Arrays.asList(Blocks.STONE.getDefaultState(), Blocks.DIRT.getDefaultState(), Blocks.GRASS.getDefaultState());

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
    @Default List<IBlockState> globalDecorators = Collections.emptyList();

    public static DecoratorSettings from(JsonObject json, DecoratorSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static DecoratorSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static DecoratorSettings copyInto(JsonObject json, DecoratorSettingsBuilder builder) {
        return new HjsonMapper(json)
            .mapStateList(Fields.replaceableBlocks, builder::replaceableBlocks)
            .mapBool(Fields.replaceDecorators, builder::replaceDecorators)
            .mapBool(Fields.replaceSolidBlocks, builder::replaceSolidBlocks)
            .mapArray(Fields.caveBlocks, CaveBlockSettings::from, builder::caveBlocks)
            .mapArray(Fields.wallDecorators, WallDecoratorSettings::from, builder::wallDecorators)
            .mapArray(Fields.ponds, PondSettings::from, builder::ponds)
            .mapObject(Fields.shell, o -> builder.shell(ShellSettings.from(o)))
            .release(builder::build);
    }
}
