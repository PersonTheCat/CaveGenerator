package personthecat.cavegenerator.presets.validator;

import personthecat.catlib.data.JsonPath;

import java.util.List;

@FunctionalInterface
interface ValidationFunction<T> {
    void apply(final ValidationContext ctx, final T t, final JsonPath.Stub path);

    static <T> void validate(ValidationContext ctx, List<T> t, JsonPath.Stub path, ValidationFunction<T> f) {
        for (int i = 0; i < t.size(); i++) {
            f.apply(ctx, t.get(i), path.index(i));
        }
    }
}
