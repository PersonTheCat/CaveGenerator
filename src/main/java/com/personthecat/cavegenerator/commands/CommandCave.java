package com.personthecat.cavegenerator.commands;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.PresetCombiner;
import com.personthecat.cavegenerator.config.PresetReader;
import net.minecraft.command.CommandBase;
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
import org.hjson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.io.SafeFileIO.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

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
        { "test", "Applies night vision and gamemode 3 for easy", "cave viewing." },
        { "combine <preset.path> <preset>", "Copies the first", "path into the second preset." },
        { "list", "Displays a list of all presets, with buttons", "for enabling / disabling." },
        { "enable <name>", "Enables the preset with name <name>." },
        { "disable <name>", "Disables the preset with name <name>." },
        { "new <name>", "Generates a new preset file with name", "<name>" },
        { "fixindent <name>", "Replaces all tabs inside of the", "preset <name> with spaces." },
        { "tojson <name>", "Backs up and converts the specified", "file from hjson to standard JSON." },
        { "tohjson <name>", "Backs up and converts the specified", "file from standard JSON to hjson." },
    };
    /** The number of lines to occupy each page of the help message. */
    private static final int USAGE_LENGTH = 5;
    /** The header to be used by the help message / usage text. */
    private static final String USAGE_HEADER = " --- Cave Command Usage (X / Y) ---";
    /** The help message / usage text. */
    private static final ITextComponent[] USAGE_MSG = createHelpMessage();
    /** New line character */
    private static final String NEW_LINE = System.getProperty("line.separator");

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
            case "test" : test(sender); break;
            case "combine" : combine(sender, args); break;
            case "enable" : setCaveEnabled(sender, args, true); break;
            case "disable" : setCaveEnabled(sender, args, false); break;
            case "list" : list(sender); break;
            case "new" : newPreset(sender, args); break;
            case "fixindent" : fixIndent(sender, args); break;
            case "tojson" : convert(sender, args, true); break;
            case "tohjson" : convert(sender, args, false); break;
            case "page" :
            case "help" : helpCommand(sender, args); break;
            default : displayHelp(sender, 1);
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
        Main.instance.loadGenerators(sender.getEntityWorld());
        Main.instance.loadGenerators(sender.getServer().getWorld(0));
        sendMessage(sender, "Successfully reloaded caves. View the log for diagnostics.");
    }

    /** Applies Night Vision and gamemode 3 to @param sender. */
    private static void test(ICommandSender sender) {
        // Get the entity from the sender.
        Entity ent = sender.getCommandSenderEntity();
        // Verify that this was sent by a player.
        if (ent instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) ent;
            // Update gamemode.
            player.setGameType(GameType.SPECTATOR);
            // Apply night vision.
            Optional<PotionEffect> nightVision = getNightVision();
            if (nightVision.isPresent()) {
                player.addPotionEffect(nightVision.get());
            } else {
                sendMessage(sender,
                    "Build error: Person must have typed \"night_vision\" incorrectly. Please let him know."
                );
            }
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
        Optional<File> located = CaveInit.locatePreset(args[0]);
        if (located.isPresent()) {
            File preset = located.get();
            // Logic could be improved.
            PresetReader.getPresetJson(preset).ifPresent(cave -> {
                setOrAdd(cave, "enabled", enabled).setComment("enabled",
                    "Whether the preset is enabled globally.");
                // Try to write the updated preset to the disk.
                writeJson(cave, preset)
                    .expectF("Error writing to %s", preset.getName());
            });
        } else {
            throw runExF("No preset was found named %s", args[0]);
        }
        sendMessage(sender, "Preset " + (enabled ? "enabled" : "disabled") + " successfully.");
    }

    /** A command for listing and enabling / disabling presets. */
    private static void list(ICommandSender sender) {
        ITextComponent msg = tcs("") // Parent has no formatting.
            .appendSibling(VIEW_BUTTON.createCopy());

        safeListFiles(CaveInit.PRESET_DIR).ifPresent(files -> {
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
        if (safeFileExists(presetFile, "Unable to read from the preset directory.")) {
            sendMessage(sender, "This preset already exists.");
            return;
        }
        final JsonObject preset = new JsonObject()
            .add("enabled", true);
        writeJson(preset, presetFile);
        sendMessage(sender, "Finished writing a new preset file.");
    }

    /** Replaces all tabs inside of the specified preset with spaces. */
    private static void fixIndent(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        File presetFile = CaveInit.locatePreset(args[0])
            .orElseThrow(() -> runExF("No preset named %s found.", args[0]));
        List<String> lines = safeContents(presetFile)
            .orElseThrow(() -> runExF("Unable to read contents of %s.", presetFile.getName()));

        StringBuilder updated = new StringBuilder();
        int numCorrections = 0;
        for (String line : lines) {
            if (line.contains("\t")) {
                line = line.replace("\t", "  ");
                numCorrections++;
            }
            updated.append(line);
            updated.append(NEW_LINE);
        }

        if (numCorrections > 0) {
            safeWrite(presetFile, updated.toString())
                .throwIfPresent();
            sendMessage(sender, "Successfully updated " + numCorrections + " lines.");
        } else {
            sendMessage(sender, "There were no lines to update.");
        }
    }

    /** Converts the specified file to between JSON and hjson. */
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
    private static void helpCommand(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        displayHelp(sender, Integer.parseInt(args[0]));
    }

    private static ITextComponent getListElementText(File file) {
        String fileName = noExtension(file);
        if (isPresetEnabled(file)) {
            return tcs(" * " + fileName + " (Enabled) ")
                .appendSibling(disableButton(fileName));
        } else {
            return tcs(" * " + fileName + " (Disabled) ")
                .appendSibling(enableButton(fileName));
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
        List<TextComponentString> msgs = new ArrayList<>();
        final int numLines = getNumElements(USAGE_TEXT) - USAGE_TEXT.length;
        final int numPages = numLines / USAGE_LENGTH - 1;
        // The actual pages..
        for (int i = 0; i < USAGE_TEXT.length; i += USAGE_LENGTH) {
            final TextComponentString header = getUsageHeader((i / USAGE_LENGTH) + 1, numPages);
            // The elements on each pages.
            for (int j = i; j < i + USAGE_LENGTH; j++) {
                final String[] full = USAGE_TEXT[j];
                // Append the required elements.
                header.appendText("\n");
                appendUsageText(header, full[0], full[1]);
                // Append any extra lines below.
                for (int k  = 2; k < full.length; k++) {
                    header.appendText("\n");
                    header.appendText((full[k]));
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
    private static ITextComponent enableButton(String fileName) {
        final Style style = ENABLE_BUTTON_STYLE
            .createDeepCopy()
            .setClickEvent(clickToRun("/cave enable " + fileName + " && reload && list"));
        return tcs("[ENABLE]")
            .setStyle(style);
    }

    /** Creates a new enable button. */
    private static ITextComponent disableButton(String fileName) {
        final Style style = DISABLE_BUTTON_STYLE
            .createDeepCopy()
            .setClickEvent(clickToRun("/cave disable " + fileName + " && reload && list"));
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

    /** Shorthand for sending a message to the input user. */
    private static void sendMessage(ICommandSender user, String msg) {
        user.sendMessage(new TextComponentString(msg));
    }

    /** Shorthand method for creating TextComponentStrings. */
    private static TextComponentString tcs(String s) {
        return new TextComponentString(s);
    }

    /** Gets the file name, minus the extension. */
    private static String noExtension(File file) {
        return noExtension(file.getName());
    }

    /** Removes any extensions from the input filename. */
    private static String noExtension(String name) {
        return name.split(Pattern.quote("."))[0];
    }

    private static Optional<PotionEffect> getNightVision() {
        Potion potion = Potion.getPotionFromResourceLocation("night_vision");
        return full(new PotionEffect(potion, Integer.MAX_VALUE, 0, true, false));
    }

    /** Ensures that at least `num` arguments are present. */
    private static void requireArgs(String[] args, int num) {
        if (args.length < num) {
            throw runEx("Insufficient arguments for this command.");
        }
    }
}