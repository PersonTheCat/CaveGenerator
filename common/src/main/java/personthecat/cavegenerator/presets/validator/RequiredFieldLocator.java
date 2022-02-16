package personthecat.cavegenerator.presets.validator;

import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.ArrayUtils;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.JsonTransformer;
import personthecat.catlib.util.McUtils;
import personthecat.cavegenerator.exception.DetailedPresetException;
import personthecat.cavegenerator.exception.MissingRequiredFieldsException;
import personthecat.cavegenerator.presets.data.*;

public class RequiredFieldLocator {

    private final String message = generateMessage();
    private final JsonObject json;
    private boolean hasErrors = false;

    public RequiredFieldLocator(final JsonObject json) {
        this.json = (JsonObject) json.deepCopy();
        this.markAll();
        if (this.hasErrors) {
            this.trim();
        }
    }

    private static String generateMessage() {
        if (McUtils.isClientSide()) {
            return I18n.get("cg.errorText.missingRequired");
        }
        return "Missing required fields: ";
    }

    public boolean hasErrors() {
        return this.hasErrors;
    }

    public DetailedPresetException createScreen(final String name) {
        return new MissingRequiredFieldsException(name, this.json);
    }

    private void markAll() {
        this.markDecorators(this.json); // Top-level overrides
        this.markDecorators(this.json, OverrideSettings.Fields.branches);
        this.markDecorators(this.json, OverrideSettings.Fields.rooms);
        this.markDecorators(this.json, CaveSettings.Fields.tunnels);
        this.markDecorators(this.json, CaveSettings.Fields.tunnels, TunnelSettings.Fields.branches);
        this.markDecorators(this.json, CaveSettings.Fields.ravines);
        this.markDecorators(this.json, CaveSettings.Fields.caverns);
        this.markDecorators(this.json, CaveSettings.Fields.caverns, CavernSettings.Fields.branches);
        this.markDecorators(this.json, CaveSettings.Fields.burrows, BurrowSettings.Fields.branches);

        JsonTransformer.withPath(CaveSettings.Fields.clusters)
            .forEach(this.json, j -> this.mark(j, ClusterSettings.Fields.states));
        JsonTransformer.withPath(CaveSettings.Fields.layers)
            .forEach(this.json, j -> this.mark(j, LayerSettings.Fields.state));
        JsonTransformer.withPath(CaveSettings.Fields.structures)
            .forEach(this.json, j -> this.mark(j, StructureSettings.Fields.name));
    }

    private void markDecorators(final JsonObject json, final String... path) {
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.wallDecorators))
            .forEach(json, j -> this.mark(j, WallDecoratorSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.caveBlocks))
            .forEach(json, j -> this.mark(j, CaveBlockSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.add(path, DecoratorSettings.Fields.ponds))
            .forEach(json, j -> this.mark(j, PondSettings.Fields.states));
        JsonTransformer.withPath(ArrayUtils.addAll(path, DecoratorSettings.Fields.shell, ShellSettings.Fields.decorators))
            .forEach(json, j -> this.mark(j, ShellSettings.Decorator.Fields.states));
    }

    private void mark(final JsonObject json, final String... keys) {
        final StringBuilder missing = new StringBuilder();
        for (final String key : keys) {
            if (!json.has(key)) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(key);
            }
        }
        if (missing.length() > 0) {
            json.appendComment(ValidationError.INDICATOR + this.message + " " + missing);
            this.hasErrors = true;
        }
    }

    private void trim() {
        visitObjects(this.json);

        final JsonObject trimmed = HjsonUtils.skip(this.json, false);
        this.json.clear().addAll(trimmed);
    }

    private static boolean visitObjects(final JsonObject json) {
        boolean anyFlagged = false;
        for (final JsonObject.Member member : json) {
            if (member.getValue().isAccessed()) {
                anyFlagged = true;
            }
            if (!member.getValue().isArray()) {
                continue;
            }
            for (final JsonValue value : member.getValue().asArray()) {
                if (value.isObject()) {
                    if (visitObjects(value.asObject())) {
                        value.setAccessed(true);
                        anyFlagged = true;
                    }
                }
            }
        }
        return anyFlagged;
    }
}
