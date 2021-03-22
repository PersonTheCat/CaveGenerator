package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.util.HjsonTools;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static com.personthecat.cavegenerator.util.CommonMethods.f;

/**
 * This class contains a series of high level utilities to be used for updating old JSON presets
 * to contain the current field names and syntax standards. It is designed to be used in a builder
 * pattern and can handle renaming fields and collapsing nested objects into single objects.
 */
public class FieldHistory {

    public static ObjectResolver withPath(String... path) {
        return new StaticObjectResolver(path);
    }

    public static ObjectResolver recursive(String key) {
        return new RecursiveObjectResolver(key);
    }

    public static abstract class ObjectResolver {
        private final List<Updater> updates = new LinkedList<>();

        public final ObjectResolver history(String... names) {
            updates.add(new RenameHistory(this, names));
            return this;
        }

        public final ObjectResolver collapse(String outer, String inner) {
            updates.add(new PathCollapseHelper(this, outer, inner));
            return this;
        }

        public final ObjectResolver toRange(String minKey, Number minDefault, String maxKey, Number maxDefault, String newKey) {
            updates.add(new RangeConverter(this, minKey, minDefault, maxKey, maxDefault, newKey));
            return this;
        }

        public final ObjectResolver markRemoved(String key, String version) {
            updates.add(new RemovedFieldNotifier(this, key, version));
            return this;
        }

        public final ObjectResolver renameValue(String key, String from, String to) {
            updates.add(new FieldRenameHelper(this, key, from, to));
            return this;
        }

        public final ObjectResolver transform(String key, MemberTransformation transformation) {
            updates.add(new MemberTransformationHelper(this, key, transformation));
            return this;
        }

        public final void updateAll(JsonObject json) {
            for (Updater update : updates) {
                update.update(json);
            }
        }

        /**
         * Executes an operation for each last container when given a path. Each path element
         * is treated as <em>either</em> an object <em>or</em> an array.
         *
         * @param json The parent JSON file being operated on.
         * @param fn What to do for each element at <code>path[path.length - 1]</code>
         */
        public abstract void forEach(JsonObject json, Consumer<JsonObject> fn);
    }

    public static class StaticObjectResolver extends ObjectResolver {
        private final String[] path;

        private StaticObjectResolver(String[] path) {
            this.path = path;
        }

        @Override
        public void forEach(JsonObject json, Consumer<JsonObject> fn) {
            forEachContainer(json, 0, fn);
        }

        private void forEachContainer(JsonObject container, int index, Consumer<JsonObject> fn) {
            if (index < path.length) {
                for (JsonObject o : HjsonTools.getRegularObjects(container, path[index])) {
                    forEachContainer(o, index + 1, fn);
                }
            } else if (index == path.length) {
                fn.accept(container);
            }
        }
    }

    public static class RecursiveObjectResolver extends ObjectResolver {
        private final String key;

        private RecursiveObjectResolver(String key) {
            this.key = key;
        }

        @Override
        public void forEach(JsonObject json, Consumer<JsonObject> fn) {
            for (JsonObject.Member member : json) {
                final JsonValue value = member.getValue();
                if (member.getName().equals(key)) {
                    HjsonTools.getRegularObjects(json, key).forEach(fn);
                }
                if (value.isObject()) {
                    forEach(value.asObject(), fn);
                } else if (value.isArray()) {
                    forEachInArray(value.asArray(), fn);
                }
            }
        }

        private void forEachInArray(JsonArray array, Consumer<JsonObject> fn) {
            for (JsonValue value : array) {
                if (value.isObject()) {
                    forEach(value.asObject(), fn);
                } else if (value.isArray()) {
                    forEachInArray(value.asArray(), fn);
                }
            }
        }
    }

    public interface Updater {
        void update(JsonObject json);
    }

    public static class RenameHistory implements Updater {
        private final ObjectResolver resolver;
        private final String[] history;

        private RenameHistory(ObjectResolver resolver, String[] history) {
            this.resolver = resolver;
            this.history = history;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::renameFields);
        }

        private void renameFields(JsonObject json) {
            final String current = history[history.length - 1];
            for (int i = 0; i < history.length - 1; i++) {
                final String key = history[i];
                final JsonValue original = json.get(key);
                if (original != null) {
                    json.set(current, original);
                    json.remove(key);
                }
            }
        }
    }

    public static class PathCollapseHelper implements Updater {
        private final ObjectResolver resolver;
        private final String outer;
        private final String inner;

        private PathCollapseHelper(ObjectResolver resolver, String outer, String inner) {
            this.resolver = resolver;
            this.outer = outer;
            this.inner = inner;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::collapse);
        }

        private void collapse(JsonObject json) {
            final JsonValue outerValue = json.get(outer);
            if (outerValue != null && outerValue.isObject()) {
                final JsonValue innerValue = outerValue.asObject().get(inner);
                if (innerValue != null) {
                    json.set(outer, innerValue);
                }
            }
        }
    }

    public static class RangeConverter implements Updater {

        private final ObjectResolver resolver;
        private final String minKey;
        private final Number minDefault;
        private final String maxKey;
        private final Number maxDefault;
        private final String newKey;

        private RangeConverter(ObjectResolver resolver, String minKey, Number minDefault, String maxKey, Number maxDefault, String newKey) {
            this.resolver = resolver;
            this.minKey = minKey;
            this.minDefault = minDefault;
            this.maxKey = maxKey;
            this.maxDefault = maxDefault;
            this.newKey = newKey;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::convert);
        }

        private void convert(JsonObject json) {
            if (json.has(minKey) || json.has(maxKey)) {
                if (minDefault instanceof Double || minDefault instanceof Float) {
                    final float min = HjsonTools.getFloatOr(json, minKey, minDefault.floatValue());
                    final float max = HjsonTools.getFloatOr(json, maxKey, maxDefault.floatValue());
                    json.set(newKey, getRange(min, max));
                } else {
                    final int min = HjsonTools.getIntOr(json, minKey, minDefault.intValue());
                    final int max = HjsonTools.getIntOr(json, maxKey, maxDefault.intValue());
                    json.set(newKey, getRange(min, max));
                }
                json.remove(minKey);
                json.remove(maxKey);
            }
        }

        private JsonValue getRange(float min, float max) {
            if (min == max) {
                return JsonValue.valueOf(min);
            }
            return new JsonArray().add(min).add(max).setCondensed(true);
        }

        private JsonValue getRange(int min, int max) {
            if (min == max) {
                return JsonValue.valueOf(min);
            }
            return new JsonArray().add(min).add(max).setCondensed(true);
        }
    }

    public static class RemovedFieldNotifier implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final String version;

        private RemovedFieldNotifier(ObjectResolver resolver, String key, String version) {
            this.resolver = resolver;
            this.key = key;
            this.version = version;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::markRemoved);
        }

        private void markRemoved(JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null) {
                json.setCondensed(false);
                value.setEOLComment(f("Removed in {}. You can delete this field.", version));
            }
        }
    }

    public static class FieldRenameHelper implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final String from;
        private final String to;

        private FieldRenameHelper(ObjectResolver resolver, String key, String from, String to) {
            this.resolver = resolver;
            this.key = key;
            this.from = from;
            this.to = to;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::renameValue);
        }

        private void renameValue(JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null && value.isString() && from.equalsIgnoreCase(value.asString())) {
                json.set(key, to);
            }
        }
    }

    @FunctionalInterface
    public interface MemberTransformation {
        Pair<String, JsonValue> transform(String name, JsonValue value);
    }

    public static class MemberTransformationHelper implements Updater {
        private final ObjectResolver resolver;
        private final String key;
        private final MemberTransformation transformation;

        private MemberTransformationHelper(ObjectResolver resolver, String key, MemberTransformation transformation) {
            this.resolver = resolver;
            this.key = key;
            this.transformation = transformation;
        }

        @Override
        public void update(JsonObject json) {
            resolver.forEach(json, this::transform);
        }

        private void transform(JsonObject json) {
            final JsonValue value = json.get(key);
            if (value != null) {
                final Pair<String, JsonValue> updated = transformation.transform(key, value);
                json.remove(key);
                json.set(updated.getKey(), updated.getValue());
            }
        }
    }

}
