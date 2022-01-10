package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.Nullable;
import personthecat.cavegenerator.world.config.RoomConfig;

import java.util.Random;

import static personthecat.catlib.util.Shorthand.invert;
import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;

@Builder(toBuilder = true)
@FieldNameConstants
public class RoomSettings {
    @NonNull public final DecoratorSettings decorators;
    @Nullable public final Float scale;
    @Nullable public final Float stretch;
    @Nullable public final Integer chance;

    public static final Codec<RoomSettings> CODEC = dynamic(RoomSettings::builder, RoomSettingsBuilder::build).create(
        extend(DecoratorSettings.CODEC, Fields.decorators, s -> s.decorators, (s, d) -> s.decorators = d),
        field(Codec.FLOAT, Fields.scale, s -> s.scale, (s, f) -> s.scale = f),
        field(Codec.FLOAT, Fields.stretch, s -> s.stretch, (s, f) -> s.stretch = f),
        field(Codec.DOUBLE, Fields.chance, s -> invert(s.chance), (s, c) -> s.chance = invert(c))
    );

    public RoomSettings withOverrides(final OverrideSettings o) {
        return this.toBuilder().decorators(this.decorators.withOverrides(o)).build();
    }

    public RoomConfig compile(final Random rand, final long seed) {
        final float scale = this.scale != null ? this.scale : 6.0F;
        final float stretch = this.stretch != null ? this.stretch : 0.5F;
        final int chance = this.chance != null ? this.chance : 10;

        return new RoomConfig(this.decorators.compile(rand, seed), scale, stretch, chance);
    }
}
