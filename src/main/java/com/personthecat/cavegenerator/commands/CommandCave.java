package com.personthecat.cavegenerator.commands;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.CavePreset;
import com.personthecat.cavegenerator.config.PresetCombiner;
import com.personthecat.cavegenerator.config.PresetCompressor;
import com.personthecat.cavegenerator.config.PresetReader;
import com.personthecat.cavegenerator.noise.CachedNoiseHelper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hjson.JsonObject;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.personthecat.cavegenerator.io.SafeFileIO.backup;
import static com.personthecat.cavegenerator.io.SafeFileIO.ensureDirExists;
import static com.personthecat.cavegenerator.io.SafeFileIO.rename;
import static com.personthecat.cavegenerator.io.SafeFileIO.copy;
import static com.personthecat.cavegenerator.io.SafeFileIO.fileExists;
import static com.personthecat.cavegenerator.io.SafeFileIO.listFiles;
import static com.personthecat.cavegenerator.util.CommonMethods.empty;
import static com.personthecat.cavegenerator.util.CommonMethods.extension;
import static com.personthecat.cavegenerator.util.CommonMethods.f;
import static com.personthecat.cavegenerator.util.CommonMethods.full;
import static com.personthecat.cavegenerator.util.CommonMethods.noExtension;
import static com.personthecat.cavegenerator.util.CommonMethods.nullable;
import static com.personthecat.cavegenerator.util.CommonMethods.runEx;
import static com.personthecat.cavegenerator.util.CommonMethods.runExF;
import static com.personthecat.cavegenerator.util.CommonMethods.toArray;
import static com.personthecat.cavegenerator.util.HjsonTools.getBool;
import static com.personthecat.cavegenerator.util.HjsonTools.writeJson;

public class CommandCave extends CommandBase {

    /** A tooltip for the enable button. */
    private static final HoverEvent ENABLE_BUTTON_HOVER =
        new HoverEvent(HoverEvent.Action.SHOW_TEXT, tcs("Enable this preset."));

    /** A tooltip for the disable button. */
    private static final HoverEvent DISABLE_BUTTON_HOVER =
        new HoverEvent(HoverEvent.Action.SHOW_TEXT, tcs("Disable this preset."));

    /** A tooltip for the view button. */
    private static final HoverEvent VIEW_BUTTON_HOVER =
        new HoverEvent(HoverEvent.Action.SHOW_TEXT, tcs("Open preset directory."));

    /** The action to be performed by the view button when clicked. */
    private static final ClickEvent VIEW_BUTTON_CLICK =
        clickToOpen(Loader.instance().getConfigDir() + "/cavegenerator/presets");

    /** The text formatting to be used for the enable button. */
    private static final Style ENABLE_BUTTON_STYLE = new Style()
        .setColor(TextFormatting.GREEN)
        .setHoverEvent(ENABLE_BUTTON_HOVER);

    /** The text formatting to be used for this disable button. */
    private static final Style DISABLE_BUTTON_STYLE = new Style()
        .setColor(TextFormatting.RED)
        .setHoverEvent(DISABLE_BUTTON_HOVER);

    /** The text formatting to be used for the view button. */
    private static final Style VIEW_BUTTON_STYLE = new Style()
        .setColor(TextFormatting.GRAY)
        .setUnderlined(true)
        .setBold(true)
        .setHoverEvent(VIEW_BUTTON_HOVER)
        .setClickEvent(VIEW_BUTTON_CLICK);

    /** The text formatting to be used for the command usage header. */
    private static final Style USAGE_HEADER_STYLE = new Style()
        .setColor(TextFormatting.GREEN)
        .setBold(true);

    /** The text formatting to be used for displaying command usage. */
    private static final Style USAGE_STYLE = new Style()
        .setColor(TextFormatting.GRAY);

    /** The button used for opening the preset directory. */
    private static final ITextComponent VIEW_BUTTON = tcs("\n --- [OPEN PRESET DIRECTORY] ---")
        .setStyle(VIEW_BUTTON_STYLE);

    /** The actual text to be used by the help messages. */
    private static final String[][] USAGE_TEXT = {
        { "reload", "Reloads the current presets from the the", "disk." },
        { "list [<dir>]", "Displays a list of all presets, with buttons", "for enabling / disabling." },
        { "test", "Applies night vision and gamemode 3 for easy", "cave viewing." },
        { "untest", "Removes night vision and puts you in gamemode 1." },
        { "jump [<k>]", "Teleports the player 1,000 * k blocks in each", "direction."},
        { "open [<name>]", "Opens a preset in your default text editor."},
        { "combine <preset.path> <preset>", "Copies the first", "path into the second preset." },
        { "enable <name> [<dir>]", "Enables the preset with name <name>." },
        { "disable <name> [<dir>]", "Disables the preset with name <name>." },
        { "new <name>", "Generates a new preset file with name", "<name>" },
        { "copy <name> [<dir>]", "Recursively copies presets out of", "config/cavegenerator." },
        { "move <name> <dir>", "Moves a preset out of the preset", "directory." },
        { "delete <name>", "Moves a preset to the backup directory." },
        { "clean [<dir>]", "Moves disabled presets to the backup directory."},
        { "rename <name> <new>", "Renames a preset, ignoring its extension." },
        { "expand <name> [<as>]", "Writes the expanded copy of this preset", "under /generated" },
        { "compress <name> [<as>]", "Writes a compressed version of this", "preset under /generated" },
        { "tojson <name>", "Backs up and converts the specified", "file from hjson to standard JSON." },
        { "tohjson <name>", "Backs up and converts the specified", "file from standard JSON to hjson." },
    };

    /** The number of lines to occupy each page of the help message. */
    private static final int USAGE_LENGTH = 5;

    /** The header to be used by the help message / usage text. */
    private static final String USAGE_HEADER = " --- Cave Command Usage (X / Y) ---";

    /** The help message / usage text. */
    private static final ITextComponent[] USAGE_MSG = createHelpMessage();

    @Override
    public String getName() {
        return "cave";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cave <subcommand>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        // The user did not specify which command to run. Inform them and stop.
        if (args.length == 0) {
            displayHelp(sender, 1);
            return;
        }
        // Allow multiple commands to be separated by `&&`.
        final int splitIndex = ArrayUtils.lastIndexOf(args, "&&");
        if (splitIndex > 0 && args.length > splitIndex) {
            // Split the arguments into multiple arrays.
            String[] runFirst = ArrayUtils.subarray(args, 0, splitIndex);
            args = ArrayUtils.subarray(args, splitIndex + 1, args.length);
            // Execute the first half of the arguments and then continue with the rest.
            this.execute(server, sender, runFirst);
        }
        try { // Process, forwarding errors to the user.
            String[] slice = ArrayUtils.subarray(args, 1, args.length);
            handle(server, sender, args[0], slice);
        } catch (RuntimeException e) {
            sendMessage(sender, e.getMessage());
        }
    }

    private static void handle(MinecraftServer server, ICommandSender sender, String command, String[] args) {
        switch (command) {
            case "reload" : reload(sender); break;
            case "list" : list(sender, args); break;
            case "test" : test(sender); break;
            case "untest": untest(sender); break;
            case "jump" : jump(sender, args); break;
            case "open" : open(sender, args); break;
            case "combine" : combine(sender, args); break;
            case "enable" : setCaveEnabled(sender, args, true); break;
            case "disable" : setCaveEnabled(sender, args, false); break;
            case "new" : newPreset(sender, args); break;
            case "copy": copyPreset(sender, args); break;
            case "move": movePreset(sender, args); break;
            case "delete": deletePreset(sender, args); break;
            case "clean": cleanPresets(sender, args); break;
            case "rename": renamePreset(sender, args); break;
            case "expand" : writeExpanded(sender, args); break;
            case "compress" : writeCompressed(sender, args); break;
            case "tojson" : convert(sender, args, true); break;
            case "tohjson" : convert(sender, args, false); break;
            case "page" :
            case "help" : helpCommand(sender, args); break;
            default : helpCommand(sender, command);
        }
    }

    /** Sends the formatted command usage to the user. */
    private static void displayHelp(ICommandSender sender, int page) {
        if (page > USAGE_MSG.length || page <= 0) {
            sendMessage(sender, "Invalid page #.");
            return;
        }
        sender.sendMessage(USAGE_MSG[page - 1]);
    }

    /** Reloads all presets from the disk. */
    private static void reload(ICommandSender sender) {
        CaveInit.initPresets(Main.instance.presets);
        Main.instance.generators.clear();
        CachedNoiseHelper.removeAll();
        if (sender.getEntityWorld().provider.getDimension() != 0) {
            Main.instance.loadGenerators(sender.getServer().getWorld(0));
        }
        Main.instance.loadGenerators(sender.getEntityWorld());
        sendMessage(sender, "Successfully reloaded caves. View the log for diagnostics.");
    }

    /** Applies night vision and gamemode 3 to the sender. */
    private static void test(ICommandSender sender) {
        // Get the entity from the sender.
        final Entity entity = sender.getCommandSenderEntity();
        // Verify that this was sent by a player.
        if (entity instanceof EntityPlayer) {
            final EntityPlayer player = (EntityPlayer) entity;
            player.setGameType(GameType.SPECTATOR);
            player.addPotionEffect(getNightVision());
        }
    }

    /** Removes night vision and gamemode 3 from the sender. */
    private static void untest(ICommandSender sender) {
        final Entity entity = sender.getCommandSenderEntity();
        if (entity instanceof EntityPlayer) {
            final EntityPlayer player = (EntityPlayer) entity;
            player.setGameType(GameType.CREATIVE);
            player.removeActivePotionEffect(getNightVision().getPotion());
        }
    }

    /** Teleports the player 1,000 * k blocks in each direction. */
    private static void jump(ICommandSender sender, String[] args) {
        final ICommandManager mgr = Objects.requireNonNull(sender.getServer()).getCommandManager();
        int distance = 1000;
        if (args.length > 0) {
            distance *= Float.parseFloat(args[0]);
        }
        mgr.executeCommand(sender,  f("/tp ~{} ~ ~{}", distance, distance));
    }

    /** Opens a preset in the default text editor. */
    private static void open(ICommandSender sender, String[] args) {
        final Optional<File> located;
        if (args.length > 0) {
            located = CaveInit.locatePreset(args[0]);
        } else {
            located = full(CaveInit.PRESET_DIR);
        }
        if (located.isPresent()) {
            try {
                Desktop.getDesktop().open(located.get());
            } catch (IOException e) {
                sendMessage(sender, e.getMessage());
            }
        } else {
            sendMessage(sender, "File not found.");
        }
    }

    /** Combines two jsons using PresetCombiner */
    private static void combine(ICommandSender sender, String[] args) {
        requireArgs(args, 2);
        PresetCombiner.combine(args[0], args[1]);
        sendMessage(sender, "Finished combining presets. The original was moved to the backup directory.");
    }

    /** Sets whether the specified preset is enabled. */
    private static void setCaveEnabled(ICommandSender sender, String[] args, boolean enabled) {
        requireArgs(args, 1);
        final File dir = args.length > 1 ? new File(CaveInit.CG_DIR, args[1]) : CaveInit.PRESET_DIR;
        final Optional<File> located = CaveInit.locatePreset(dir, args[0]);
        if (located.isPresent()) {
            final File preset = located.get();
            // Logic could be improved.
            PresetReader.getPresetJson(preset).ifPresent(cave -> {
                if (cave.has("enabled")) {
                    cave.set("enabled", enabled);
                } else {
                    cave.add("enabled", enabled, "Whether the preset is enabled globally.");
                }
                final File f = enabled && dir != CaveInit.PRESET_DIR
                    ? new File(CaveInit.PRESET_DIR, preset.getName())
                    : preset;
                // Try to write the updated preset to the disk.
                writeJson(cave, f).expectF("Error writing to {}", f.getName());
            });
        } else {
            throw runExF("No preset was found named {}", args[0]);
        }
        sendMessage(sender, "Preset " + (enabled ? "enabled" : "disabled") + " successfully.");
    }

    /** A command for listing and enabling / disabling presets. */
    private static void list(ICommandSender sender, String[] args) {
        final File dir = args.length > 0 ? new File(CaveInit.CG_DIR, args[0]) : CaveInit.PRESET_DIR;
        if (!dir.exists()) {
            displayDirectories(sender);
            return;
        }
        ITextComponent msg = tcs("") // Parent has no formatting.
            .appendSibling(VIEW_BUTTON.createCopy());

        listFiles(dir).ifPresent(files -> {
            msg.appendSibling(tcs("\n"));
            for (File file : files) {
                if (CaveInit.validExtension(file)) {
                    msg.appendSibling(tcs("\n"));
                    msg.appendSibling(getListElementText(file));
                }
            }
        });
        sender.sendMessage(msg);
    }

    /** Generates a new cave preset with a name from args[0] */
    private static void newPreset(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        final String presetName = noExtension(args[0]) + ".cave";
        final File presetFile = new File(CaveInit.PRESET_DIR, presetName);
        if (fileExists(presetFile, "Unable to read from the preset directory.")) {
            sendMessage(sender, "This preset already exists.");
            return;
        }
        final JsonObject preset = new JsonObject()
            .add("enabled", true);
        writeJson(preset, presetFile);
        sendMessage(sender, "Finished writing a new preset file.");
    }

    /** Copies a preset into the preset directory. */
    private static void copyPreset(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        final Optional<File> located;
        if (args.length > 1) {
            final File dir = new File(CaveInit.CG_DIR, args[1]);
            located = CaveInit.locatePreset(dir, args[0]);
        } else {
            located = locateRecursively(CaveInit.CG_DIR, args[0]);
        }
        if (located.isPresent()) {
            final File f = located.get();
            copy(f, CaveInit.PRESET_DIR).throwIfPresent();
            final String relativePath = f.getPath().replace(CaveInit.CG_DIR.getPath(), "");
            sendMessage(sender, "Successfully copied " + relativePath);
        } else {
            sendMessage(sender, "File not found.");
        }
    }

    /** Recursively locates a valid cave preset inside of the mod directory. */
    private static Optional<File> locateRecursively(File dir, String name) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                final Optional<File> inDir = locateRecursively(f, name);
                if (inDir.isPresent()) {
                    return inDir;
                }
            } else if (CaveInit.validExtension(f) && noExtension(f).equals(name)) {
                return full(f);
            }
        }
        return empty();
    }

    /** Moves a preset out of the preset directory. */
    private static void movePreset(ICommandSender sender, String[] args) {
        requireArgs(args, 2);
        final Optional<File> located = CaveInit.locatePreset(args[0]);
        if (located.isPresent()) {
            final File f = located.get();
            final File dir = new File(CaveInit.CG_DIR, args[1]);
            if (!dir.exists()) {
                ensureDirExists(dir).throwIfPresent();
                sendMessage(sender, f("Created folder: {}", dir.getName()));
            }
            copy(f, dir).throwIfPresent();
            if (!f.delete()) {
                sendMessage(sender, "Original could not be deleted.");
            }
            sendMessage(sender, f("The file was moved to {}/{}", dir.getName(), f.getName()));
        } else {
            sendMessage(sender, "File not found.");
        }
    }

    /** Makes a backup of and deletes a preset in the presets directory. */
    private static void deletePreset(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        final Optional<File> located = CaveInit.locatePreset(args[0]);
        if (located.isPresent()) {
            backup(located.get(), true);
            sendMessage(sender, located.get().getName() + " was moved to backups.");
        } else {
            sendMessage(sender, "File not found.");
        }
    }

    private static void cleanPresets(ICommandSender sender, String[] args) {
        final File dir = args.length > 0
            ? new File(CaveInit.CG_DIR, args[0])
            : CaveInit.PRESET_DIR;
        if (!dir.exists()) {
            displayDirectories(sender);
            return;
        }
        if (dir.equals(CaveInit.BACKUP_DIR)) {
            if (args.length > 1 && "force".equalsIgnoreCase(args[1])) {
                for (File f : dir.listFiles(CaveInit::validExtension)) {
                    if (!f.delete()) {
                        throw runExF("Error deleting {}", f.getName());
                    }
                }
            } else {
                sendMessage(sender, "Rerun with \"force\" to delete backups.");
            }
            return;
        }
        int numDisabled = 0;
        for (File f : dir.listFiles(CaveInit::validExtension)) {
            if (!isPresetEnabled(f)) {
                backup(f, true);
                numDisabled++;
            }
        }
        if (numDisabled > 0) {
            sendMessage(sender, f("{} preset(s) were moved to backups."));
        } else {
            sendMessage(sender, f("There are no disabled presets in {}.", dir.getName()));
        }
    }

    /** Renames a preset, ignoring its extension. */
    private static void renamePreset(ICommandSender sender, String[] args) {
        requireArgs(args, 2);
        final Optional<File> located = CaveInit.locatePreset(args[0]);
        if (located.isPresent()) {
            final File file = located.get();
            final String name = noExtension(args[1]) + "." + extension(file);
            rename(file, name);
            sendMessage(sender, f("{} was renamed to {}", file.getName(), name));
        } else {
            sendMessage(sender, "File not found.");
        }
    }

    /** Writes the expanded version of this preset (removing variables) under /generated. */
    private static void writeExpanded(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        final String presetName = noExtension(args[0]);
        // No need to reparse this file. It's in memory.
        final CavePreset settings = nullable(Main.instance.presets.get(presetName))
            .orElseThrow(() -> runExF("Unable to find preset: {}", args[0]));
        final String newName = args.length > 1
            ? noExtension(args[1])
            : presetName;
        ensureDirExists(CaveInit.GENERATED_DIR)
            .expect("Error creating /generated directory.");
        final File expanded = new File(CaveInit.GENERATED_DIR, newName + ".cave");
        writeJson(settings.raw, expanded);
        sendMessage(sender, "Finished writing expanded preset file.");
    }

    /** Writes a compressed, regular JSON version of this preset. */
    private static void writeCompressed(ICommandSender sender, String[] args) {
        requireArgs(args,1);
        final String presetName = noExtension(args[0]);
        // Read the actual preset so that it may be expanded or not.
        final File preset = CaveInit.locatePreset(presetName)
            .orElseThrow(() -> runExF("Unable to find preset: {}", args[0]));
        final String newName = args.length > 1
            ? noExtension(args[1])
            : presetName;
        final JsonObject original = PresetReader.getPresetJson(preset)
            .orElseThrow(() -> runExF("Error reading {}", presetName));
        ensureDirExists(CaveInit.GENERATED_DIR)
            .expect("Error creating /generated directory.");
        final File compressed = new File(CaveInit.GENERATED_DIR, newName + ".json");
        // Manually write JSON
        try (FileWriter writer = new FileWriter(compressed)) {
            PresetCompressor.compress(original);
            original.writeTo(writer); // Plain JSON string.
            sendMessage(sender, "Finished writing expanded preset file.");
        } catch (IOException e) {
            sendMessage(sender, "Error writing new preset.");
        }
    }

    /** Converts the specified file to and from JSON or Hjson. */
    private static void convert(ICommandSender sender, String[] args, boolean toJson) {
        requireArgs(args, 1);
        final Optional<File> preset = CaveInit.locatePreset(args[0]);
        if (!preset.isPresent()) {
            sendMessage(sender, "No preset found named " + args[0]);
            return;
        }
        final File presetFile = preset.get();
        if (toJson == extension(presetFile).equals("json")) {
            sendMessage(sender, "Preset is already in the desired format.");
            return;
        }
        final Optional<JsonObject> json = PresetReader.getPresetJson(presetFile);
        if (!json.isPresent()) {
            sendMessage(sender, "The file could not be parsed.");
            return;
        }
        final String extension = toJson ? ".json" : ".cave";
        File newPreset = new File(CaveInit.PRESET_DIR, noExtension(presetFile) + extension);
        // The output file's extension determines the format.
        writeJson(json.get(), newPreset);
        backup(presetFile);
        presetFile.delete();
        sendMessage(sender, "Converted successfully. The original was moved to the backup directory.");
    }

    /** The standard help command, specifying the page number. */
    private static void helpCommand(ICommandSender sender, String... args) {
        requireArgs(args, 1);
        final String arg = args[0];
        final int page = StringUtils.isNumeric(arg) ? Integer.parseInt(arg) : 1;
        displayHelp(sender, page);
    }

    private static ITextComponent getListElementText(File file) {
        final String dir = file.getParentFile().getName();
        final String fileName = noExtension(file);
        if (isPresetEnabled(file)) {
            return tcs(" * " + fileName + " (Enabled) ")
                .appendSibling(disableButton(fileName, dir));
        } else {
            return tcs(" * " + fileName + " (Disabled) ")
                .appendSibling(enableButton(fileName, dir));
        }
    }

    /** Determines whether the input file points to a preset that is enabled. */
    private static boolean isPresetEnabled(File file) {
        return PresetReader.getPresetJson(file)
            .flatMap(preset -> getBool(preset, "enabled"))
            .orElse(true);
    }

    /** Generates the help message, displaying usage for each sub-command. */
    private static ITextComponent[] createHelpMessage() {
        final List<TextComponentString> msgs = new ArrayList<>();
        final int numLines = getNumElements(USAGE_TEXT) - USAGE_TEXT.length;
        final int numPages = numLines / USAGE_LENGTH - 2;
        // The actual pages..
        for (int i = 0; i < USAGE_TEXT.length; i += USAGE_LENGTH) {
            final TextComponentString header = getUsageHeader((i / USAGE_LENGTH) + 1, numPages);
            // The elements on each page.
            for (int j = i; j < i + USAGE_LENGTH && j < USAGE_TEXT.length; j++) {
                final String[] full = USAGE_TEXT[j];
                // Append the required elements.
                header.appendText("\n");
                appendUsageText(header, full[0], full[1]);
                // Append any extra lines below.
                for (int k  = 2; k < full.length; k++) {
                    header.appendSibling(tcs("\n " + full[k]).setStyle(USAGE_STYLE));
                }
            }
            msgs.add(header);
        }
        return toArray(msgs, TextComponentString.class);
    }

    private static int getNumElements(String[][] matrix) {
        int numElements = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                numElements++;
            }
        }
        return numElements;
    }

    private static TextComponentString getUsageHeader(int page, int max) {
        final String header = USAGE_HEADER
            .replace("X", String.valueOf(page))
            .replace("Y", String.valueOf(max));
        TextComponentString full = tcs("");
        TextComponentString headerTCS = tcs(header);
        headerTCS.setStyle(USAGE_HEADER_STYLE);
        full.appendSibling(headerTCS);
        return full;
    }

    /** A slightly neater way to append so many components to the help message. */
    private static void appendUsageText(ITextComponent msg, String command, String usage) {
        msg.appendSibling(usageText(command, usage));
    }

    /** Formats the input text to nicely display a command'spawnStructure usage. */
    private static ITextComponent usageText(String command, String usage) {
        ITextComponent msg = tcs(""); // Parent has no formatting.
        msg.appendSibling(tcs(command));
        msg.appendSibling(tcs(" :\n " + usage).setStyle(USAGE_STYLE));
        return msg;
    }

    /** Creates a new enable button. */
    private static ITextComponent enableButton(String fileName, String dir) {
        final Style style = ENABLE_BUTTON_STYLE
            .createDeepCopy()
            .setClickEvent(clickToRun("/cave enable " + fileName + " " + dir + " && reload && list"));
        return tcs("[ENABLE]")
            .setStyle(style);
    }

    /** Creates a new enable button. */
    private static ITextComponent disableButton(String fileName, String dir) {
        final Style style = DISABLE_BUTTON_STYLE
            .createDeepCopy()
            .setClickEvent(clickToRun("/cave disable " + fileName + " " + dir + " && reload && list"));
        return tcs("[DISABLE]")
            .setStyle(style);
    }

    /** A ClickEvent constructor for running commands. */
    private static ClickEvent clickToRun(String command) {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
    }

    /** A ClickEvent constructor for opening files. */
    private static ClickEvent clickToOpen(String path) {
        return new ClickEvent(ClickEvent.Action.OPEN_FILE, path);
    }

    private static void displayDirectories(ICommandSender user) {
        final List<String> names = new ArrayList<>();
        for (File f : CaveInit.CG_DIR.listFiles()) {
            if (f.isDirectory()) {
                names.add(f.getName());
            }
        }
        final String options = Arrays.toString(names.toArray(new String[0]));
        sendMessage(user, f("Invalid directory. Options: {}", options));
    }

    /** Shorthand for sending a message to the input user. */
    private static void sendMessage(ICommandSender user, String msg) {
        user.sendMessage(new TextComponentString(msg));
    }

    /** Shorthand method for creating TextComponentStrings. */
    private static TextComponentString tcs(String s) {
        return new TextComponentString(s);
    }

    private static PotionEffect getNightVision() {
        final Potion potion = Potion.getPotionFromResourceLocation("night_vision");
        Objects.requireNonNull(potion, "Build error: invalid potion ID.");
        return new PotionEffect(potion, Integer.MAX_VALUE, 0, true, false);
    }

    /** Ensures that at least `num` arguments are present. */
    private static void requireArgs(String[] args, int num) {
        if (args.length < num) {
            throw runEx("Insufficient arguments for this command.");
        }
    }
}