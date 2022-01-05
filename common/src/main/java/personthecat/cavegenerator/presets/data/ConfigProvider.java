package personthecat.cavegenerator.presets.data;

import com.mojang.serialization.Codec;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.serialization.HjsonOps;

import java.util.Random;

public interface ConfigProvider<T, R> {
    Codec<T> codec();

    default T withOverrides(final OverrideSettings o) {
        return this.asTargetType();
    }

    default T withDefaults(final T t) {
        return this.asTargetType();
    }

    R compile(final Random rand, final long seed);

    @SuppressWarnings("unchecked")
    default T asTargetType() {
        return (T) this;
    }

    default JsonObject toJson() {
        return this.codec().encodeStart(HjsonOps.INSTANCE, this.asTargetType()).result()
            .map(JsonValue::asObject)
            .orElseGet(JsonObject::new);
    }
}
