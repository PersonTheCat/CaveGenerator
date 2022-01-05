package personthecat.cavegenerator.model;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import personthecat.catlib.serialization.DynamicCodec;

import java.util.List;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.CodecUtils.easyList;
import static personthecat.catlib.serialization.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.DynamicField.field;

/**
 * Contains settings for how a particular floating point value is intended to
 * change throughout its lifetime.
 */
@FieldNameConstants
@AllArgsConstructor
@Builder(toBuilder = true)
public class ScalableFloat {

    public final float startVal;
    public final float startValRandFactor;
    public final float factor;
    public final float randFactor;
    public final float exponent;

    public static final DynamicCodec<ScalableFloatBuilder, ScalableFloat, ScalableFloat> OBJECT_CODEC =
        dynamic(ScalableFloat::builder, ScalableFloatBuilder::build).create(
            field(Codec.FLOAT, Fields.startVal, s -> s.startVal, (s, v) -> s.startVal = v),
            field(Codec.FLOAT, Fields.startValRandFactor, s -> s.startValRandFactor, (s, v) -> s.startValRandFactor = v),
            field(Codec.FLOAT, Fields.factor, s -> s.factor, (s, v) -> s.factor = v),
            field(Codec.FLOAT, Fields.randFactor, s -> s.randFactor, (s, v) -> s.randFactor = v),
            field(Codec.FLOAT, Fields.exponent, s -> s.exponent, (s, v) -> s.exponent = v)
        );

    public static Codec<ScalableFloat> defaultedCodec(final ScalableFloat defaults) {
        return simpleEither(defaultedObject(defaults), defaultedArray(defaults));
    }

    public static Codec<ScalableFloat> defaultedObject(final ScalableFloat defaults) {
        return OBJECT_CODEC.withBuilder(defaults::toBuilder);
    }

    public static Codec<ScalableFloat> defaultedArray(final ScalableFloat defaults) {
        return easyList(Codec.FLOAT).xmap(list -> fromArray(list, defaults), ScalableFloat::toFloats);
    }

    public static ScalableFloat fromArray(final List<Float> floats, final ScalableFloat defaults) {
        return new ScalableFloat(
            floats.size() > 0 ? floats.get(0) : defaults.startVal,
            floats.size() > 1 ? floats.get(1) : defaults.startValRandFactor,
            floats.size() > 2 ? floats.get(2) : defaults.factor,
            floats.size() > 3 ? floats.get(3) : defaults.randFactor,
            floats.size() > 4 ? floats.get(4) : defaults.exponent
        );
    }

    public List<Float> toFloats() {
        return FloatArrayList.wrap(new float[] {
            this.startVal, this.startValRandFactor, this.factor, this.randFactor, this.exponent
        });
    }
}
