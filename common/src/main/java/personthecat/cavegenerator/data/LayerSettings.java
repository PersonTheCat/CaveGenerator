package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import personthecat.catlib.data.Range;
import personthecat.catlib.util.HjsonMapper;
import personthecat.cavegenerator.config.CavePreset;

import static personthecat.catlib.util.Shorthand.full;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class LayerSettings {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Fields.layers;

    /** The default noise values used for all layers. */
    private static final NoiseMapSettings DEFAULT_NOISE = NoiseMapSettings.builder()
        .frequency(0.015f).range(Range.of(-7, 7)).build();

    /** Default spawn conditions for all layer generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(0, 20)).ceiling(full(DEFAULT_NOISE)).build();

    /** Conditions for these layers to spawn. */
    @Default ConditionSettings conditions = DEFAULT_CONDITIONS;

    /** The block to spawn as a stone "layer" underground. */
    BlockState state;

    public static LayerSettings from(final JsonObject json, final OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static LayerSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }
        
    private static LayerSettings copyInto(final JsonObject json, final LayerSettingsBuilder builder) {
        final LayerSettings original = builder.build();
        return new HjsonMapper<>(CavePreset.Fields.layers, LayerSettingsBuilder::build)
            .mapRequiredState(Fields.state, LayerSettingsBuilder::state)
            .mapSelf((b, o) -> b.conditions(ConditionSettings.from(o, original.conditions)))
            .create(builder, json);
    }

}
