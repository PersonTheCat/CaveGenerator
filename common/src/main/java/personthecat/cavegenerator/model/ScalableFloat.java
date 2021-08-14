package personthecat.cavegenerator.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.util.HjsonMapper;
import personthecat.catlib.util.HjsonUtils;

import static personthecat.catlib.exception.Exceptions.jsonFormatEx;

/**
 * Contains settings for how a particular floating point value is intended to
 * change throughout its lifetime.
 */
@FieldNameConstants
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class ScalableFloat {
    float startVal;
    float startValRandFactor;
    float factor;
    float randFactor;
    float exponent;

    public static ScalableFloat fromValue(final JsonValue json, final ScalableFloat defaults) {
        if (json.isObject()) {
            return fromObject(json.asObject(), defaults);
        }
        return fromArray(HjsonUtils.asOrToArray(json), defaults);
    }

    public static ScalableFloat fromObject(final JsonObject json, final ScalableFloat defaults) {
        return new HjsonMapper<>("<scalable>", ScalableFloatBuilder::build)
            .mapFloat(Fields.startVal, ScalableFloatBuilder::startVal)
            .mapFloat(Fields.startValRandFactor, ScalableFloatBuilder::startValRandFactor)
            .mapFloat(Fields.factor, ScalableFloatBuilder::factor)
            .mapFloat(Fields.randFactor, ScalableFloatBuilder::randFactor)
            .mapFloat   (Fields.exponent, ScalableFloatBuilder::exponent)
            .create(defaults.toBuilder(), json);
    }

    public static ScalableFloat fromArray(final JsonArray json, final ScalableFloat defaults) {
        for (final JsonValue value : json) {
            if (!value.isNumber()) {
                throw jsonFormatEx("Expected number value in scalable float type");
            }
        }
        return new ScalableFloat(
            json.size() > 0 ? json.get(0).asFloat() : defaults.startVal,
            json.size() > 1 ? json.get(1).asFloat() : defaults.startValRandFactor,
            json.size() > 2 ? json.get(2).asFloat() : defaults.factor,
            json.size() > 3 ? json.get(3).asFloat() : defaults.randFactor,
            json.size() > 4 ? json.get(4).asFloat() : defaults.exponent
        );
    }
}
