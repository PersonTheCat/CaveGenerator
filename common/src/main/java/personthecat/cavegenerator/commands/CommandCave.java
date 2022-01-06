package personthecat.cavegenerator.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.annotations.Node;
import personthecat.catlib.command.annotations.Node.DoubleRange;
import personthecat.catlib.command.annotations.Node.ListInfo;
import personthecat.catlib.command.annotations.Node.StringValue;
import personthecat.catlib.command.arguments.ArgumentSuppliers;
import personthecat.catlib.exception.Exceptions;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.FeatureSupport;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.PathUtils;
import personthecat.catlib.util.ResourceArrayLinter;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.exception.CaveOutputException;
import personthecat.cavegenerator.init.PresetLoadingContext;
import personthecat.cavegenerator.io.ModFolders;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.PresetCompressor;
import personthecat.cavegenerator.presets.lang.CaveLangExtension;
import personthecat.cavegenerator.presets.lang.ReferenceHelper;
import personthecat.cavegenerator.util.Calculator;
import personthecat.cavegenerator.util.Reference;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static personthecat.catlib.command.CommandUtils.clickToOpen;
import static personthecat.catlib.command.CommandUtils.clickToRun;
import static personthecat.catlib.command.CommandUtils.displayOnHover;
import static personthecat.catlib.exception.Exceptions.cmdEx;
import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.PathUtils.*;
import static personthecat.catlib.util.Shorthand.full;
import static java.util.Optional.empty;

public class CommandCave {

    private static final HoverEvent ENABLE_BUTTON_HOVER = displayOnHover("Enable this preset.");
    private static final HoverEvent DISABLE_BUTTON_HOVER = displayOnHover("Disable this preset.");
    private static final HoverEvent EXPLORE_BUTTON_HOVER = displayOnHover("Explore this directory.");
    private static final HoverEvent VIEW_BUTTON_HOVER = displayOnHover("Open preset directory.");
    private static final ClickEvent VIEW_BUTTON_CLICK = clickToOpen(ModFolders.PRESET_DIR);

    private static final Style ENABLE_BUTTON_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN)
        .withHoverEvent(ENABLE_BUTTON_HOVER);

    private static final Style DISABLE_BUTTON_STYLE = Style.EMPTY
        .withColor(ChatFormatting.RED)
        .withHoverEvent(DISABLE_BUTTON_HOVER);

    private static final Style ENABLED_INDICATOR_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GREEN);

    private static final Style DISABLED_INDICATOR_STYLE = Style.EMPTY
        .withColor(ChatFormatting.RED);

    private static final Style EXPLORE_BUTTON_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY)
        .withHoverEvent(EXPLORE_BUTTON_HOVER);

    private static final Style VIEW_BUTTON_STYLE = Style.EMPTY
        .withColor(ChatFormatting.GRAY)
        .withUnderlined(true)
        .withBold(true)
        .withHoverEvent(VIEW_BUTTON_HOVER)
        .withClickEvent(VIEW_BUTTON_CLICK);

    private static final MutableComponent VIEW_BUTTON = new TextComponent("\n --- [OPEN PRESET DIRECTORY] ---")
        .withStyle(VIEW_BUTTON_STYLE);

    private static final String DISTANCE_ARG = "k";
    private static final String FILE_ARG = "file";
    private static final String DISPLAY_ARG = "display";
    private static final String NAME_ARG = "name";
    private static final String RAW_ARG = "raw";
    private static final String EXP_ARG = "exp";
    private static final String FEATURE_ARG = "feature";
    private static final String ENABLED_KEY = CavePreset.ENABLED_KEY;

    private final JsonObject memory = new JsonObject();

    @ModCommand(
        description = "Reloads all of the current presets from the disk."
    )
    private void reload(final CommandContextWrapper ctx) {
        PresetLoadingContext.reset();
        CaveRegistries.reloadAll();
        CachedNoiseHelper.removeAll();
        ctx.sendMessage("Successfully reloaded caves. View the log for diagnostics.");
    }

    @ModCommand(
        description = "Displays a list of all presets with buttons to enable or disable them.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class, optional = true)
        }
    )
    private void list(final CommandContextWrapper ctx) {
        final File dir = ctx.getOptional(FILE_ARG, File.class).orElse(ModFolders.PRESET_DIR);
        if (!dir.isDirectory()) {
            throw cmdEx("Expected a directory: {}", dir.getName());
        }
        final MutableComponent msg = new TextComponent("") // No formatting on parent.
            .append(VIEW_BUTTON.copy())
            .append("\n");

        // Sort every feature to be displayed in order.
        final List<File> directories = new ArrayList<>();
        final List<File> enabled = new ArrayList<>();
        final List<File> disabled = new ArrayList<>();
        directories.add(dir.getParentFile());

        for (final File file : listFiles(dir)) {
            if (Reference.VALID_EXTENSIONS.contains(extension(file))) {
                (isPresetEnabled(file) ? enabled : disabled).add(file);
            } else if (file.isDirectory()) {
                directories.add(file);
            }
        }
        directories.forEach(f -> msg.append(getViewDirectoryText(f)));
        enabled.forEach(f -> msg.append(getListElementText(f, true)));
        disabled.forEach(f -> msg.append(getListElementText(f, false)));
        ctx.sendMessage(msg);
    }

    @ModCommand(
        description = "Teleports the player 1,000 * k blocks in each direction",
        branch = {
            @Node(name = DISTANCE_ARG, doubleRange = @DoubleRange(min = 1.0), optional = true)
        }
    )
    private void jump(final CommandContextWrapper ctx) {
        final double distance = 1000.0 * ctx.getOptional(DISTANCE_ARG, Double.class).orElse(1.0);
        ctx.execute("/tp ~{} ~ ~{}", distance, distance);
    }

    @ModCommand(
        description = {
            "Enables the given preset file. If this file is not in the presets folder,",
            "a copy of the file will be created in the presets folder and the original",
            "file will remain untouched."
        },
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = DISPLAY_ARG, isBoolean = true, optional = true)
        }
    )
    private void enable(final CommandContextWrapper ctx) {
        enableOrDisable(ctx, true);
    }

    @ModCommand(
        description = "Disables the given preset file.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = DISPLAY_ARG, isBoolean = true, optional = true)
        }
    )
    private void disable(final CommandContextWrapper ctx) {
        enableOrDisable(ctx, false);
    }

    private static void enableOrDisable(final CommandContextWrapper ctx, final boolean enabled) {
        final File f = ctx.getFile(FILE_ARG);
        final JsonObject preset = PresetLoadingContext.readJson(f);

        if (preset == null) {
            ctx.sendError("Error reading preset.");
        } else {
            final File output = enabled && !ModFolders.PRESET_DIR.equals(f.getParentFile())
                ? new File(ModFolders.PRESET_DIR, f.getName()) : f;

            markPresetEnabledOnDisk(preset, output, enabled);
            ctx.sendMessage("Preset {} successfully.", (enabled ? "enabled" : "disabled"));

            if (ctx.getOptional(DISPLAY_ARG, Boolean.class).orElse(false)) {
                ctx.execute("/cave reload");
                ctx.execute("/cave list {}", getRelativePath(ModFolders.CG_DIR, f.getParentFile()));
            }
        }
    }

    @ModCommand(
        name = "new",
        description = "Generates a new preset with the given name. Extensions are optional.",
        branch = {
            @Node(name = NAME_ARG, stringValue = @StringValue)
        }
    )
    private void newPreset(final CommandContextWrapper ctx, String name) {
        if (PathUtils.extension(name).isEmpty()) {
            name += ".cave";
        }
        final JsonObject preset = new JsonObject().add(ENABLED_KEY, true);
        HjsonUtils.writeJson(preset, new File(ModFolders.PRESET_DIR, name));
        ctx.sendMessage("Finished writing {}", name);
    }

    @ModCommand(
        description = "Writes the expanded version of this preset to /generated.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = NAME_ARG, stringValue = @StringValue, optional = true)
        }
    )
    private void expand(final CommandContextWrapper ctx) {
        CaveLangExtension.expand(ctx.getFile(FILE_ARG))
            .ifErr(e -> ctx.sendError(e.getMessage()))
            .ifOk(j -> writeExpanded(ctx, j));
    }

    @ModCommand(
        description = "Writes a lightly compressed version of the given file to /generated",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = NAME_ARG, stringValue = @StringValue, optional = true)
        }
    )
    private void compress(final CommandContextWrapper ctx, final File file) throws IOException {
        final Optional<JsonObject> read = HjsonUtils.readSuppressing(file);
        if (!read.isPresent()) {
            throw Exceptions.cmdEx("Error reading {}", file.getName());
        }

        final String name = ctx.getOptional(NAME_ARG, String.class)
            .map(s -> noExtension(s) + ".json")
            .orElse(file.getName());

        FileIO.mkdirsOrThrow(ModFolders.GENERATED_DIR);
        final File output = new File(ModFolders.GENERATED_DIR, name);

        HjsonUtils.writeJson(PresetCompressor.compress(read.get()), output).throwIfErr();
        ctx.sendMessage("Finished writing generated/{}.", name);
    }

    @ModCommand(
        description = {
            "Defines a JSON member as a variable in memory. e.g.",
            "\"/cave set key: value\" or \"/cave set { k1: 'v1', k2: 'v2'\""
        },
        branch = {
            @Node(name = RAW_ARG, stringValue = @StringValue(StringValue.Type.GREEDY))
        }
    )
    private void set(final CommandContextWrapper ctx, final String raw) {
        final JsonValue value = JsonObject.readHjson(raw, HjsonUtils.NO_CR);
        if (!value.isObject()) {
            throw cmdEx("Missing key. (e.g. key: {})", raw);
        }
        final JsonObject data = value.asObject();
        if (data.isEmpty()) {
            throw cmdEx("Nothing to load.");
        }
        ctx.sendMessage("Writing...");
        for (final JsonObject.Member member : data) {
            this.memory.set(member.getName(), member.getValue());
            ctx.sendMessage(" * {}", member.getName());
        }
    }

    @ModCommand(
        name = "import",
        description = "Evaluates a Cave import expression and loads any required variables.",
        branch = {
            @Node(name = EXP_ARG, stringValue = @StringValue(StringValue.Type.GREEDY))
        }
    )
    private void importPreset(final CommandContextWrapper ctx, final String exp) {
        final JsonObject data = getImports(exp);
        if (data.isEmpty()) {
            throw cmdEx("Nothing to import.");
        }
        ctx.sendMessage("Loading...");
        for (final JsonObject.Member member : data) {
            this.memory.set(member.getName(), member.getValue());
            ctx.sendMessage(" * {}", member.getName());
        }
    }

    @ModCommand(
        description = "Evaluates a JSON, Cave, or arithmetic expression.",
        branch = {
            @Node(name = EXP_ARG, stringValue = @StringValue(StringValue.Type.GREEDY))
        }
    )
    private void eval(final CommandContextWrapper ctx, final String exp) {
        if (Calculator.isExpression(exp)) {
            ctx.sendMessage(String.valueOf(Calculator.evaluate(exp)));
            return;
        }
        final JsonValue result = ReferenceHelper.trySubstitute(this.memory, exp)
            .orElseGet(() -> JsonObject.readHjson(exp));
        ctx.sendLintedMessage(doEvaluate(result));
    }

    @ModCommand(
        description = "Prints JSON values and comments from working memory.",
        branch = {
            @Node(name = NAME_ARG, stringValue = @StringValue, intoList = @ListInfo, optional = true)
        }
    )
    private void print(final CommandContextWrapper ctx, final List<String> keys) {
        if (keys.isEmpty()) {
            ctx.sendLintedMessage(this.memory.toString(HjsonUtils.NO_CR));
            return;
        }
        final JsonObject output = new JsonObject();
        for (final String key : keys) {
            final Optional<Pair<String, JsonValue>> member = this.getMember(key);
            if (member.isPresent()) {
                final Pair<String, JsonValue> m = member.get();
                output.add(m.getKey(), m.getValue());
            } else if (key.contains("$")) {
                throw cmdEx("Not an evaluator. Use a key @{}", key);
            } else {
                output.add(key, "undefined");
            }
        }
        ctx.sendLintedMessage(output.toString(HjsonUtils.NO_CR));
    }

    @ModCommand(
        description = "Displays all of the variables in memory."
    )
    private void dir(final CommandContextWrapper ctx) {
        if (this.memory.isEmpty()) {
            ctx.sendMessage("[]");
            return;
        }
        final StringBuilder sb = new StringBuilder("[");
        for (final JsonObject.Member member : this.memory) {
            sb.append(' ').append(member.getName()).append(',');
        }
        ctx.sendMessage(sb.append(" ]").toString());
    }

    @ModCommand(
        description = "Saves the in-memory definitions to a file.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class)
        }
    )
    private void save(final CommandContextWrapper ctx, File file) throws IOException {
        if (ModFolders.CG_DIR.equals(file.getParentFile())) {
            file = new File(ModFolders.GENERATED_DIR, file.getName());
        }
        final File dir = file.getParentFile();
        if (extension(file).isEmpty()) {
            file = new File(dir, file.getName() + ".cave");
        }
        FileIO.mkdirsOrThrow(file.getParentFile());
        HjsonUtils.writeJson(this.memory, file).throwIfErr();
        ctx.sendMessage("Successfully wrote {}/{}", dir.getName(), file.getName());
    }

    @ModCommand(
        description = "Clears the in-memory definitions."
    )
    private void clear(final CommandContextWrapper ctx) {
        this.memory.clear();
        ctx.sendMessage("JSON data were cleared from memory.");
    }

    @ModCommand(
        name = "debug",
        arguments = "<features|carvers|structures>",
        description = "Displays a list of all current biome features or carvers.",
        linter = ResourceArrayLinter.class,
        branch = {
            @Node(name = FEATURE_ARG, registry = Feature.class)
        }
    )
    private void debugFeatures(final CommandContextWrapper ctx, final Feature<?> feature) {
        ctx.sendLintedMessage(Arrays.toString(FeatureSupport.getIds(feature).toArray()));
    }

    @ModCommand(
        linter = ResourceArrayLinter.class
    )
    private void debugCarvers(final CommandContextWrapper ctx) {
        ctx.sendLintedMessage(Arrays.toString(BuiltinRegistries.CONFIGURED_CARVER.keySet().toArray()));
    }

    @ModCommand(
        linter = ResourceArrayLinter.class
    )
    private void debugStructures(final CommandContextWrapper ctx) {
        ctx.sendLintedMessage(Arrays.toString(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.keySet().toArray()));
    }

    private static boolean isPresetEnabled(final File file) {
        if (!file.exists()) return false;

        return CaveRegistries.PRESETS.getOptional(noExtension(file))
            .map(p -> p.enabled)
            .orElseGet(() -> HjsonUtils.readSuppressing(file)
                .map(CavePreset::isEnabled)
                .orElse(false));
    }

    private static MutableComponent getViewDirectoryText(final File file) {
        return new TextComponent("\n / " + file.getName() + " ")
            .append(viewButton(file));
    }

    private static MutableComponent getListElementText(final File file, final boolean enabled) {
        final String filename = PathUtils.noExtension(file);
        final Style openButton = Style.EMPTY
            .withHoverEvent(displayOnHover(("Open " + file.getName() + ".")))
            .withClickEvent(clickToOpen(file));
        if (enabled) {
            return new TextComponent("\n")
                .append(new TextComponent(" + ").setStyle(ENABLED_INDICATOR_STYLE))
                .append(new TextComponent(filename + " ").setStyle(openButton))
                .append(disableButton(file));
        } else {
            return new TextComponent("\n")
                .append(new TextComponent(" - ").setStyle(DISABLED_INDICATOR_STYLE))
                .append(new TextComponent(filename + " ").setStyle(openButton))
                .append(enableButton(file));
        }
    }

    private static MutableComponent enableButton(final File f) {
        final String path = getRelativePath(ModFolders.CG_DIR, f);
        final Style style = ENABLE_BUTTON_STYLE
            .withClickEvent(clickToRun("/cave enable " + path + " true"));
        return new TextComponent("[ENABLE]").setStyle(style);
    }

    private static MutableComponent disableButton(final File f) {
        final String path = getRelativePath(ModFolders.CG_DIR, f);
        final Style style = DISABLE_BUTTON_STYLE
            .withClickEvent(clickToRun("/cave disable " + path + " true"));
        return new TextComponent("[DISABLE]").setStyle(style);
    }

    private static MutableComponent viewButton(final File dir) {
        final String path = getRelativePath(ModFolders.CG_DIR, dir);
        final Style style = EXPLORE_BUTTON_STYLE
            .withClickEvent(clickToRun("/cave list " + path));
        return new TextComponent("[VIEW]").setStyle(style);
    }

    private Optional<Pair<String, JsonValue>> getMember(String key) {
        JsonValue value = this.memory.get(key);
        if (value == null) {
            value = this.memory.get(key += "()");
            if (value == null) {
                return empty();
            }
        }
        return full(Pair.of(key, value));
    }

    private static String doEvaluate(final JsonValue value) {
        if (value.isArray()) {
            CaveLangExtension.calculateAll(value.asArray());
        } else if (value.isObject()) {
            CaveLangExtension.calculateAll(value.asObject());
        } else if (value.isString() && Calculator.isExpression(value.asString())) {
            return String.valueOf(Calculator.evaluate(value.asString()));
        }
        return value.toString(HjsonUtils.NO_CR);
    }

    private static JsonObject getImports(final String exp) {
        final JsonObject data = new JsonObject();
        // Generate a faux preset to be expanded.
        final JsonObject fauxPreset = new JsonObject()
                .set(CaveLangExtension.IMPORTS, exp)
                .set(CaveLangExtension.VARIABLES, data);
        CaveLangExtension.expandInPlace(fauxPreset);
        // The variables object was removed from the faux
        // preset, but the implicit VANILLA is still there.
        data.remove(CaveLangExtension.VANILLA);
        return data;
    }

    private static void writeExpanded(final CommandContextWrapper ctx, final JsonObject expanded) {
        FileIO.mkdirsOrThrow(ModFolders.GENERATED_DIR);
        final String name = ctx.getOptional(NAME_ARG, String.class)
            .map(s -> !extension(s).isEmpty() ? s : s + ".cave")
            .orElse(ctx.getFile(FILE_ARG).getName());
        HjsonUtils.writeJson(expanded, new File(ModFolders.GENERATED_DIR, name))
            .mapErr(CaveOutputException::new)
            .throwIfErr();
        ctx.sendMessage("Finished writing generated/{}.", name);
    }

    private static void markPresetEnabledOnDisk(final JsonObject preset, final File f, final boolean enabled) {
        if (preset.has(ENABLED_KEY)) {
            preset.set(ENABLED_KEY, enabled);
        } else {
            preset.add(ENABLED_KEY, enabled, "Whether this preset is enabled globally");
        }
        HjsonUtils.writeJson(preset, f).expect("Error writing to file: {}", f.getName());
    }
}