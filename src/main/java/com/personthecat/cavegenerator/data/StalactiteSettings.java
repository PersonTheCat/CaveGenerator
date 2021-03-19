package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.model.Range;
import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.block.state.IBlockState;
import org.hjson.JsonObject;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@Builder
@FieldNameConstants
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@ParametersAreNonnullByDefault
public class StalactiteSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.stalactites;

    /** Default spawn conditions for all stalactite generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(11, 55)).build();

    /** Conditions for these tunnels to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** The required state to make the body of this structure. */
    IBlockState state;

    /** Whether this structure should spawn upward or downward, i.e. stalagmite or stalactite. */
    @Default Type type = Type.STALACTITE;

    /** Required fields. Must be supplied by the constructor. */
    @Default boolean wide = true;

    /** The 0-1 chance that this spawner should run in any given chunk. */
    @Default double chance = 0.167f;

    /** The maximum length to generate. */
    @Default int maxLength = 3;

    /** Source blocks to check for before spawning. */
    @Default List<IBlockState> matchers = Collections.emptyList();

    /** The default noise settings to be optionally used for stalactites. */
    public static final NoiseRegionSettings DEFAULT_NOISE = NoiseRegionSettings.builder()
        .frequency(0.025f).scale(0.7125f).build();

    public static StalactiteSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static StalactiteSettings from(JsonObject json) {
        return copyInto(json, builder().conditions(DEFAULT_CONDITIONS));
    }

    private static StalactiteSettings copyInto(JsonObject json, StalactiteSettingsBuilder builder) {
        return new HjsonMapper(json)
            .mapRequiredState(Fields.state, FEATURE_NAME, builder::state)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, builder.conditions$value)))
            .mapEnum(Fields.type, Type.class, builder::type)
            .mapBool(Fields.wide, builder::wide)
            .mapFloat(Fields.chance, builder::chance)
            .mapInt(Fields.maxLength, builder::maxLength)
            .mapStateList(Fields.matchers, builder::matchers)
            .release(builder::build);
    }

    public enum Type {
        STALAGMITE,
        STALACTITE
    }
}