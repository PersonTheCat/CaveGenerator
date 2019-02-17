package com.personthecat.cavegenerator.commands;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
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

import java.io.File;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.personthecat.cavegenerator.util.SafeFileIO.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

public class CommandCave extends CommandBase {
    /** A message to display when the preset directory is somehow unavailable. */
    private static final String NO_ACCESS = "Currently unable to access preset directory.";
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
    /** The header to be used by the help message / usage text. */
    private static final ITextComponent USAGE_HEADER = tcs("\n --- Cave Command Usage ---\n")
        .setStyle(USAGE_HEADER_STYLE);
    /** The help message / usage text. */
    private static final ITextComponent USAGE_TEXT = createHelpMessage();

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
            helpCommand(sender);
            return;
        }
        // Allow multiple commands to be separated by `&&`.
        final int splitIndex = ArrayUtils.indexOf(args, "&&");
        if (splitIndex > 0 && args.length > splitIndex) {
            // Split the arguments into multiple arrays.
            String[] runFirst = ArrayUtils.subarray(args, 0, splitIndex);
            args = ArrayUtils.subarray(args,splitIndex + 1, args.length);
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

    private void handle(MinecraftServer server, ICommandSender sender, String command, String[] args) {
        switch (command) {
            case "reload":
                reload(sender);
                break;
            case "test":
                test(sender);
                break;
            case "combine":
                combine(sender, args);
                break;
            case "enable":
                setCaveEnabled(args, true);
                sendMessage(sender, "Preset enabled successfully.");
                break;
            case "disable":
                setCaveEnabled(args, false);
                sendMessage(sender, "Preset disabled successfully.");
                break;
            case "list":
                list(sender);
                break;
            default:
                helpCommand(sender);
        }
    }

    /** Sends the formatted command usage to the user. */
    private void helpCommand(ICommandSender sender) {
        sender.sendMessage(USAGE_TEXT);
    }

    /** Reloads all presets from the disk. */
    private void reload(ICommandSender sender) {
        CaveInit.initPresets(Main.instance.presets)
            .handleIfPresent((e) -> { // That didn't work. Forward the error to the user.
                sendMessage(sender, e.getMessage());
            })
            .andThen(() -> { // All is well. Inform the user of success.
                sendMessage(sender, "Successfully reloaded caves.");
            });
    }

    /** Applies Night Vision and gamemode 3 to @param sender. */
    private void test(ICommandSender sender) {
        // Get the entity from the sender.
        Entity ent = sender.getCommandSenderEntity();
        // Verify that this was sent by a player.
        if (ent instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) ent;
            player.setGameType(GameType.SPECTATOR);
            // Scary possible null values.
            Potion potion = Potion.getPotionFromResourceLocation("night_vision");
            // Begone, I say!
            if (potion != null) {
                player.addPotionEffect(new PotionEffect(potion, Integer.MAX_VALUE, 0, true, false));
            } else {
                sendMessage(sender,
                    "Build error: Person must have typed \"night_vision\" incorrectly. Please let him know."
                );
            }
        }
    }

    /** Combines two jsons using PresetCombiner */
    private void combine(ICommandSender sender, String[] args) {
        requireArgs(args, 2);
        // To-do
    }

    /** Sets whether the specified preset is enabled. */
    private void setCaveEnabled(String[] args, boolean enabled) {
        requireArgs(args, 1);
        Optional<File> located = locatePreset(args[0]);
        if (located.isPresent()) {
            File preset = located.get();
            // Logic could be improved.
            PresetReader.getPresetJson(preset).ifPresent(cave -> {
                // Determine whether the field is present.
                if (getBool(cave, "enabled").isPresent()) {
                    cave.set("enabled", enabled);
                } else {
                    cave.add("enabled", enabled);
                }
                cave.setComment("enabled",
                    "Whether the preset is enabled globally."
                );
                // Try to write the updated preset to the disk.
                writeJson(cave, preset)
                    .expectF("Error writing to %s", preset.getName());
            });
        } else {
            throw runExF("No preset was found named %s", args[0]);
        }
    }

    /** A command for listing and enabling / disabling presets. */
    private void list(ICommandSender sender) {
        ITextComponent msg = tcs("") // Parent has no formatting.
            .appendSibling(VIEW_BUTTON.createCopy());

        safeListFiles(CaveInit.DIR).ifPresent(files -> {
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

    private ITextComponent getListElementText(File file) {
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
    private boolean isPresetEnabled(File file) {
        return PresetReader.getPresetJson(file)
            .flatMap(preset -> getBool(preset, "enabled"))
            .orElse(false);
    }

    private static ITextComponent createHelpMessage() {
        ITextComponent msg = tcs("");
        msg.appendSibling(USAGE_HEADER);
        msg.appendSibling(usageText("reload", "Reloads the current presets from the disk.\n"));
        msg.appendSibling(usageText("test", "Applies night vision and gamemode 3 for easy cave viewing.\n"));
        msg.appendSibling(usageText("combine <preset.path> <into_preset>", "Copies the first path into the second preset.\n"));
        msg.appendSibling(usageText("list", "Displays a list of all presets, with buttons for enabling / disabling.\n"));
        msg.appendSibling(usageText("enable <name>", "Enables the preset with name <name>.\n"));
        msg.appendSibling(usageText("disable <name>", "Disables the preset with name <name>."));
        return msg;
    }

    private static ITextComponent usageText(String command, String usage) {
        ITextComponent msg = tcs(""); // Parent has no formatting.
        msg.appendSibling(tcs(command).setStyle(USAGE_STYLE));
        msg.appendSibling(tcs(" : " + usage));
        return msg;
    }

    /** Creates a new enable button. */
    private ITextComponent enableButton(String fileName) {
        final Style style = ENABLE_BUTTON_STYLE
            .createDeepCopy()
            .setClickEvent(clickToRun("/cave enable " + fileName + " && list"));
        return tcs("[ENABLE]")
            .setStyle(style);
    }

    /** Creates a new enable button. */
    private ITextComponent disableButton(String fileName) {
        final Style style = DISABLE_BUTTON_STYLE
            .createDeepCopy()
            .setClickEvent(clickToRun("/cave disable " + fileName + " && list"));
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
    private void sendMessage(ICommandSender user, String msg) {
        user.sendMessage(new TextComponentString(msg));
    }

    /** Shorthand method for creating TextComponentStrings. */
    private static TextComponentString tcs(String s) {
        return new TextComponentString(s);
    }

    /** Gets the file name, minus the extension. */
    private String noExtension(File file) {
        return file.getName()
            .split(Pattern.quote("."))[0];
    }

    /** Attempts to locate a preset using each of the possible extensions. */
    private Optional<File> locatePreset(String preset) {
        for (String ext : CaveInit.EXTENSIONS) {
            Optional<File> found = tryExtension(preset, ext);
            if (found.isPresent()) {
                return found;
            }
        }
        return empty();
    }

    /** Attempts to locate a preset using a specific extension. */
    private Optional<File> tryExtension(String preset, String extension) {
        File presetFile = new File(CaveInit.DIR, preset + "." + extension);
        if (safeFileExists(presetFile, NO_ACCESS)) {
            return full(presetFile);
        }
        return empty();
    }

    /** Ensures that at least one argument is present. */
    private void requireArgs(String[] args, int num) {
        if (args.length < num) {
            throw runEx("Insufficient arguments for this command.");
        }
    }
}