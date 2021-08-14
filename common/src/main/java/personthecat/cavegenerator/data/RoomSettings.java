package personthecat.cavegenerator.data;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;
import personthecat.catlib.util.HjsonMapper;

import static personthecat.catlib.util.Shorthand.invert;

@Builder
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class RoomSettings {

    /** Default decorator settings for all room generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** Cave blocks and wall decorators applied to these rooms. */
    @Default DecoratorSettings decorators = DEFAULT_DECORATORS;

    /** The radius in blocks. */
    @Default float scale = 6.0F;

    /** A vertical ratio of scale. */
    @Default float stretch = 0.5F;

    /** The 1/x chance of this room spawning at any tunnel system origin. */
    @Default int chance = 10;

    public static RoomSettings from(final JsonObject json, final OverrideSettings overrides) {
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().decorators(decorators));
    }

    public static RoomSettings from(final JsonObject json) {
        return copyInto(json, builder());
    }

    private static RoomSettings copyInto(final JsonObject json, final RoomSettingsBuilder builder) {
        final RoomSettings original = builder.build();
        return new HjsonMapper<>(OverrideSettings.Fields.rooms, RoomSettingsBuilder::build)
            .mapSelf((b, o) -> b.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapFloat(Fields.scale, RoomSettingsBuilder::scale)
            .mapFloat(Fields.stretch, RoomSettingsBuilder::stretch)
            .mapFloat(Fields.chance, (b, f) -> b.chance(invert(f)))
            .create(builder, json);
    }

}
