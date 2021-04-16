package com.personthecat.cavegenerator.data;

import com.personthecat.cavegenerator.util.HjsonMapper;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonObject;

import static com.personthecat.cavegenerator.util.CommonMethods.invert;

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

    public static RoomSettings from(JsonObject json, OverrideSettings overrides) {
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().decorators(decorators));
    }

    public static RoomSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static RoomSettings copyInto(JsonObject json, RoomSettingsBuilder builder) {
        final RoomSettings original = builder.build();
        return new HjsonMapper(json)
            .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapFloat(Fields.scale, builder::scale)
            .mapFloat(Fields.stretch, builder::stretch)
            .mapFloat(Fields.chance, f -> builder.chance(invert(f)))
            .release(builder::build);
    }

}
