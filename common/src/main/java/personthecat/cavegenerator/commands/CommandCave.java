package personthecat.cavegenerator.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
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
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.PathUtils;
import personthecat.cavegenerator.CaveRegistries;
import personthecat.cavegenerator.exception.CaveOutputException;
import personthecat.cavegenerator.init.CaveInit;
import personthecat.cavegenerator.presets.PresetReader;
import personthecat.cavegenerator.io.ModFolders;
import personthecat.cavegenerator.noise.CachedNoiseHelper;
import personthecat.cavegenerator.presets.CavePreset;
import personthecat.cavegenerator.presets.PresetCompressor;
import personthecat.cavegenerator.presets.lang.PresetExpander;
import personthecat.cavegenerator.presets.lang.ReferenceHelper;
import personthecat.cavegenerator.util.Calculator;
import personthecat.cavegenerator.util.CaveLinter;

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

@SuppressWarnings("unused") // Used by CatLib
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
    private static final String JSON_ARG = "json";
    private static final String ENABLED_KEY = CavePreset.Fields.enabled;

    private static final JsonObject MEMORY = new JsonObject();

    @ModCommand(
        description = "Reloads all of the current presets from the disk."
    )
    private static void reload(final CommandContextWrapper wrapper) {
        CaveRegistries.reloadAll();
        CachedNoiseHelper.removeAll();
        wrapper.sendMessage("Successfully reloaded caves. View the log for diagnostics.");
    }

    @ModCommand(
        description = "Displays a list of all presets with buttons to enable or disable them.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class, optional = true)
        }
    )
    private static void list(final CommandContextWrapper wrapper) {
        final File dir = wrapper.getOptional(FILE_ARG, File.class).orElse(ModFolders.PRESET_DIR);
        if (!dir.isDirectory()) {
            throw cmdEx("Expected a directory: {}", dir.getName());
        }
        final MutableComponent msg = new TextComponent("") // No formatting on parent.
            .append(VIEW_BUTTON.copy())
            .append("\n");

        // Sort all of the features to be displayed in order.
        final List<File> directories = new ArrayList<>();
        final List<File> enabled = new ArrayList<>();
        final List<File> disabled = new ArrayList<>();
        directories.add(dir.getParentFile());

        for (final File file : listFiles(dir)) {
            if (CaveInit.validExtension(file)) {
                (isPresetEnabled(file) ? enabled : disabled).add(file);
            } else if (file.isDirectory()) {
                directories.add(file);
            }
        }
        directories.forEach(f -> msg.append(getViewDirectoryText(f)));
        enabled.forEach(f -> msg.append(getListElementText(f, true)));
        disabled.forEach(f -> msg.append(getListElementText(f, false)));
        wrapper.sendMessage(msg);
    }

    @ModCommand(
        description = "Applies night vision and spectator mode for easy cave viewing."
    )
    private static void test(final CommandContextWrapper wrapper) {
        final Player player = wrapper.getPlayer();
        if (player != null) {
            player.setGameMode(GameType.SPECTATOR);

            final MobEffect nightVision = MobEffects.NIGHT_VISION;
            player.addEffect(new MobEffectInstance(nightVision, 999999999, 1, true, false));
        }
    }

    @ModCommand(
        description = "Removes night vision and puts you in the default game mode."
    )
    private static void untest(final CommandContextWrapper wrapper) {
        final Player player = wrapper.getPlayer();
        if (player != null) {
            final GameType mode = Optional.ofNullable(wrapper.getServer())
                .map(MinecraftServer::getDefaultGameType)
                .orElse(GameType.CREATIVE);

            player.setGameMode(mode);
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @ModCommand(
        arguments = "[<k>]",
        description = "Teleports the player 1,000 * k blocks in each direction",
        branch = {
            @Node(name = DISTANCE_ARG, doubleRange = @DoubleRange(min = 1.0), optional = true)
        }
    )
    private static void jump(final CommandContextWrapper wrapper) {
        final double distance = 1000.0 * wrapper.getOptional(DISTANCE_ARG, Double.class).orElse(1.0);
        wrapper.execute("/tp ~{} ~ ~{}", distance, distance);
    }

    @ModCommand(
        arguments = "<file> [<display>]",
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
    private static void enable(final CommandContextWrapper wrapper) {
        enableOrDisable(wrapper, true);
    }

    @ModCommand(
        arguments = "<file> [<display>]",
        description = "Disables the given preset file.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = DISPLAY_ARG, isBoolean = true, optional = true)
        }
    )
    private static void disable(final CommandContextWrapper wrapper) {
        enableOrDisable(wrapper, false);
    }

    private static void enableOrDisable(final CommandContextWrapper wrapper, final boolean enabled) {
        final File f = wrapper.getFile(FILE_ARG);
        final JsonObject preset = PresetReader.getPresetJson(f).orElse(null);

        if (preset == null) {
            wrapper.sendError("Error reading preset.");
        } else {
            final File output = enabled && !ModFolders.PRESET_DIR.equals(f.getParentFile())
                ? new File(ModFolders.PRESET_DIR, f.getName()) : f;

            markPresetEnabledOnDisk(preset, output, enabled);
            wrapper.sendMessage("Preset {} successfully.", (enabled ? "enabled" : "disabled"));

            if (wrapper.getOptional(DISPLAY_ARG, Boolean.class).orElse(false)) {
                wrapper.execute("/cave reload");
                wrapper.execute("/cave list {}", getRelativePath(ModFolders.CG_DIR, f.getParentFile()));
            }
        }
    }

    private static void markPresetEnabledOnDisk(final JsonObject preset, final File f, final boolean enabled) {
        if (preset.has(ENABLED_KEY)) {
            preset.set(ENABLED_KEY, enabled);
        } else {
            preset.add(ENABLED_KEY, enabled, "Whether this preset is enabled globally");
        }
        HjsonUtils.writeJson(preset, f).expect("Error writing to file: {}", f.getName());
    }

    @ModCommand(
        name = "new",
        arguments = "<name>",
        description = "Generates a new preset with the given name. Extensions are optional.",
        branch = {
            @Node(name = NAME_ARG, stringValue = @StringValue)
        }
    )
    private static void newPreset(final CommandContextWrapper wrapper) {
        String name = wrapper.getString(NAME_ARG);
        if (PathUtils.extension(name).isEmpty()) {
            name += ".cave";
        }
        final JsonObject preset = new JsonObject().add(ENABLED_KEY, true);
        HjsonUtils.writeJson(preset, new File(ModFolders.PRESET_DIR, name));
        wrapper.sendMessage("Finished writing {}", name);
    }

    @ModCommand(
        arguments = "<file> [<as>]",
        description = "Writes the expanded version of this preset to /generated.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = NAME_ARG, stringValue = @StringValue, optional = true)
        }
    )
    private static void expand(final CommandContextWrapper wrapper) {
        PresetExpander.expand(wrapper.getFile(FILE_ARG))
            .ifErr(e -> wrapper.sendError(e.getMessage()))
            .ifOk(j -> writeExpanded(wrapper, j));
    }

    private static void writeExpanded(final CommandContextWrapper wrapper, final JsonObject expanded) {
        FileIO.mkdirsOrThrow(ModFolders.GENERATED_DIR);
        final String name = wrapper.getOptional(NAME_ARG, String.class)
            .map(s -> !extension(s).isEmpty() ? s : s + ".cave")
            .orElse(wrapper.getFile(FILE_ARG).getName());
        HjsonUtils.writeJson(expanded, new File(ModFolders.GENERATED_DIR, name))
            .mapErr(CaveOutputException::new)
            .throwIfErr();
        wrapper.sendMessage("Finished writing generated/{}.", name);
    }

    @ModCommand(
        arguments = "<file> [<as>]",
        description = "Writes a lightly compressed version of the given file to /generated",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class),
            @Node(name = NAME_ARG, stringValue = @StringValue, optional = true)
        }
    )
    private static void compress(final CommandContextWrapper wrapper) throws IOException {
        final File file = wrapper.getFile(FILE_ARG);

        final Optional<JsonObject> read = HjsonUtils.readSuppressing(file);
        if (!read.isPresent()) {
            throw Exceptions.cmdEx("Error reading {}", file.getName());
        }

        final String name = wrapper.getOptional(NAME_ARG, String.class)
            .map(s -> noExtension(s) + ".json")
            .orElse(file.getName());

        FileIO.mkdirsOrThrow(ModFolders.GENERATED_DIR);
        final File output = new File(ModFolders.GENERATED_DIR, name);

        HjsonUtils.writeJson(PresetCompressor.compress(read.get()), output).throwIfErr();
        wrapper.sendMessage("Finished writing generated/{}.", name);
    }

    @ModCommand(
        arguments = "<member>",
        description = {
            "Defines a JSON member as a variable in memory. e.g.",
            "\"/cave set key: value\" or \"/cave set { k1: 'v1', k2: 'v2'\""
        },
        branch = {
            @Node(name = JSON_ARG, stringValue = @StringValue(StringValue.Type.GREEDY))
        }
    )
    private static void set(final CommandContextWrapper wrapper) {
        final String raw = wrapper.getString(JSON_ARG);
        final JsonValue value = JsonObject.readHjson(raw, HjsonUtils.NO_CR);
        if (!value.isObject()) {
            throw cmdEx("Missing key. (e.g. key: {})", raw);
        }
        final JsonObject data = value.asObject();
        if (data.isEmpty()) {
            throw cmdEx("Nothing to load.");
        }
        wrapper.sendMessage("Writing...");
        for (final JsonObject.Member member : data) {
            MEMORY.set(member.getName(), member.getValue());
            wrapper.sendMessage(" * {}", member.getName());
        }
    }

    @ModCommand(
        name = "import",
        arguments = "<exp>",
        description = "Evaluates a Cave import expression and loads any required variables.",
        branch = {
            @Node(name = JSON_ARG, stringValue = @StringValue(StringValue.Type.GREEDY))
        }
    )
    private static void importPreset(final CommandContextWrapper wrapper) {
        final JsonObject data = getImports(wrapper.getString(JSON_ARG));
        if (data.isEmpty()) {
            throw cmdEx("Nothing to import.");
        }
        wrapper.sendMessage("Loading...");
        for (final JsonObject.Member member : data) {
            MEMORY.set(member.getName(), member.getValue());
            wrapper.sendMessage(" * {}", member.getName());
        }
    }

    private static JsonObject getImports(final String exp) {
        final JsonObject data = new JsonObject();
        // Generate a faux preset to be expanded.
        final JsonObject fauxPreset = new JsonObject()
            .set(PresetExpander.IMPORTS, exp)
            .set(PresetExpander.VARIABLES, data);
        PresetExpander.expandInPlace(fauxPreset);
        // The variables object was removed from the faux
        // preset, but the implicit VANILLA is still there.
        data.remove(PresetExpander.VANILLA);
        return data;
    }

    @ModCommand(
        arguments = "<exp>",
        description = "Evaluates a JSON, Cave, or arithmetic expression.",
        branch = {
            @Node(name = JSON_ARG, stringValue = @StringValue(StringValue.Type.GREEDY))
        }
    )
    private static void eval(final CommandContextWrapper wrapper) {
        final String exp = wrapper.getString(JSON_ARG);
        if (Calculator.isExpression(exp)) {
            wrapper.sendMessage(String.valueOf(Calculator.evaluate(exp)));
            return;
        }
        final JsonValue result = ReferenceHelper.trySubstitute(MEMORY, exp)
            .orElseGet(() -> JsonObject.readHjson(exp));
        wrapper.sendLintedMessage(doEvaluate(result));
    }

    private static String doEvaluate(final JsonValue value) {
        if (value.isArray()) {
            PresetExpander.calculateAll(value.asArray());
        } else if (value.isObject()) {
            PresetExpander.calculateAll(value.asObject());
        } else if (value.isString() && Calculator.isExpression(value.asString())) {
            return String.valueOf(Calculator.evaluate(value.asString()));
        }
        return value.toString(HjsonUtils.NO_CR);
    }

    @ModCommand(
        arguments = "[<key1>] [<key2>] [...]",
        description = "Prints JSON values and comments from working memory.",
        branch = {
            @Node(name = NAME_ARG, stringValue = @StringValue, intoList = @ListInfo, optional = true)
        }
    )
    private static void print(final CommandContextWrapper wrapper) {
        final List<String> keys = wrapper.getList(NAME_ARG, String.class);
        if (keys.isEmpty()) {
            wrapper.sendLintedMessage(MEMORY.toString(HjsonUtils.NO_CR));
            return;
        }
        final JsonObject output = new JsonObject();
        for (final String key : keys) {
            final Optional<Pair<String, JsonValue>> member = getMember(key);
            if (member.isPresent()) {
                final Pair<String, JsonValue> m = member.get();
                output.add(m.getKey(), m.getValue());
            } else if (key.contains("$")) {
                throw cmdEx("Not an evaluator. Use a key @{}", key);
            } else {
                output.add(key, "undefined");
            }
        }
        wrapper.sendLintedMessage(output.toString(HjsonUtils.NO_CR));
    }

    private static Optional<Pair<String, JsonValue>> getMember(String key) {
        JsonValue value = MEMORY.get(key);
        if (value == null) {
            value = MEMORY.get(key += "()");
            if (value == null) {
                return empty();
            }
        }
        return full(Pair.of(key, value));
    }

    @ModCommand(
        description = "Displays all of the variables in memory."
    )
    private static void dir(final CommandContextWrapper wrapper) {
        if (MEMORY.isEmpty()) {
            wrapper.sendMessage("[]");
            return;
        }
        final StringBuilder sb = new StringBuilder("[");
        for (final JsonObject.Member member : MEMORY) {
            sb.append(' ').append(member.getName()).append(',');
        }
        wrapper.sendMessage(sb.append(" ]").toString());
    }

    @ModCommand(
        arguments = "<file>",
        description = "Saves the in-memory definitions to a file.",
        branch = {
            @Node(name = FILE_ARG, descriptor = ArgumentSuppliers.File.class)
        }
    )
    private static void save(final CommandContextWrapper wrapper) throws IOException {
        File output = wrapper.getFile(FILE_ARG);
        if (ModFolders.CG_DIR.equals(output.getParentFile())) {
            output = new File(ModFolders.GENERATED_DIR, output.getName());
        }
        final File dir = output.getParentFile();
        if (extension(output).isEmpty()) {
            output = new File(dir, output.getName() + ".cave");
        }
        FileIO.mkdirsOrThrow(output.getParentFile());
        HjsonUtils.writeJson(MEMORY, output).throwIfErr();
        wrapper.sendMessage("Successfully wrote {}/{}", dir.getName(), output.getName());
    }

    @ModCommand(
        description = "Clears the in-memory definitions."
    )
    private static void clear(final CommandContextWrapper wrapper) {
        MEMORY.clear();
        wrapper.sendMessage("JSON data were cleared from memory.");
    }

    private static boolean isPresetEnabled(final File file) {
        return PresetReader.getPresetJson(file)
            .flatMap(preset -> HjsonUtils.getBool(preset, "enabled"))
            .orElse(true);
    }

    private static MutableComponent getViewDirectoryText(File file) {
        return new TextComponent("\n / " + file.getName() + " ")
            .append(viewButton(file));
    }

    private static MutableComponent getListElementText(final File file, final boolean enabled) {
        final String dir = getRelativePath(ModFolders.CG_DIR, file.getParentFile());
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
}