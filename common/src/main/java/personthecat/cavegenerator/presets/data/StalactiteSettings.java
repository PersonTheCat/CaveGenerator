package personthecat.cavegenerator.presets.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.presets.CavePreset;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@Builder
@FieldNameConstants
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@ParametersAreNonnullByDefault
public class StalactiteSettings {

    /** Default spawn conditions for all stalactite generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(11, 55)).build();

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** The required state to make the body of this structure. */
    BlockState state;

    /** Whether this structure should spawn upward or downward, i.e. stalagmite or stalactite. */
    @Default Type type = Type.STALACTITE;

    /** The general width of this structure. */
    @Default Size size = Size.MEDIUM;

    /** The 0-1 chance that this spawner should run in any given chunk. */
    @Default double chance = 0.167F;

    /** The possible lengths to generate. */
    @Default Range length = Range.of(3, 5);

    /** The minimum amount of free space above or below  */
    @Default int space = 3;

    /** Whether all sides should have the same length. */
    @Default boolean symmetrical = true;

    /** Source blocks to check for before spawning. */
    @Default List<BlockState> matchers = Collections.emptyList();

    /** The default noise settings to be optionally used for stalactites. */
    public static final NoiseRegionSettings DEFAULT_NOISE = NoiseRegionSettings.builder()
        .frequency(0.025f).threshold(Range.of(-0.425F)).build();

    public static StalactiteSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static StalactiteSettings from(final JsonObject json) {
        return copyInto(json, builder().conditions(DEFAULT_CONDITIONS));
    }

    private static StalactiteSettings copyInto(final JsonObject json, final StalactiteSettingsBuilder builder) {
        return new HjsonMapper<>(CavePreset.Fields.stalactites, StalactiteSettingsBuilder::build)
            .mapRequiredState(Fields.state, StalactiteSettingsBuilder::state)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, builder.conditions$value)))
            .mapEnum(Fields.type, Type.class, StalactiteSettingsBuilder::type)
            .mapEnum(Fields.size, Size.class, StalactiteSettingsBuilder::size)
            .mapFloat(Fields.chance, StalactiteSettingsBuilder::chance)
            .mapRange(Fields.length, StalactiteSettingsBuilder::length)
            .mapInt(Fields.space, StalactiteSettingsBuilder::space)
            .mapBool(Fields.symmetrical, StalactiteSettingsBuilder::symmetrical)
            .mapStateList(Fields.matchers, StalactiteSettingsBuilder::matchers)
            .create(builder, json);
    }

    public enum Type {
        STALAGMITE,
        STALACTITE,
        SPELEOTHEM
    }

    public enum Size {
        SMALL,
        MEDIUM,
        LARGE,
        GIANT
    }
}