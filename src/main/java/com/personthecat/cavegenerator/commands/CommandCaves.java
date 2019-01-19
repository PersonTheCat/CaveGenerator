package com.personthecat.cavegenerator.commands;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.config.PresetReader;
import com.personthecat.cavegenerator.gui.CavePresetGui;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.Optional;

import static com.personthecat.cavegenerator.util.SafeFileIO.*;
import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

public class CommandCaves extends CommandBase {
    private static final String NO_ACCESS = "Currently unable to access preset directory.";

    @Override
    public String getName() {
        return "cave";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return
            "Options: \n" +
            "`reload` Reloads the current presets from the disk.\n" +
            "`test` Applies night vision and gamemode 3 for easy viewing.\n" +
            "`combine <preset.path> <preset>`\n" +
            "`edit <preset_name>`\n" +
            "`enable <name>` Enables the preset with name <name>.\n" +
            "`disable <name>` Disables the preset with name <name>.";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) { // The user did not specify which command to run. Inform them and stop.
            sendMessage(sender, "Error: You need to supply an argument.");
            return;
        }
        try {
            // Process, forwarding errors to the user.
            String[] slice = ArrayUtils.subarray(args, 1, args.length);
            handle(server, sender, args[0], slice);
        } catch (RuntimeException e) {
            sendMessage(sender, e.getMessage());
        }
    }

    public void handle(MinecraftServer server, ICommandSender sender, String command, String[] args) {
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
            case "edit":
                edit(sender, args);
                break;
            case "enable":
                setCaveEnabled(args, true);
                sendMessage(sender, "Preset enabled successfully.");
                break;
            case "disable":
                setCaveEnabled(args, false);
                sendMessage(sender, "Preset disabled successfully.");
                break;
            default:
                sendMessage(sender, "Invalid argument.");
        }
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
                player.addPotionEffect(new PotionEffect(potion, 999999999, 0, true, false));
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

    private void edit(ICommandSender sender, String[] args) {
        requireArgs(args, 1);
        Minecraft.getMinecraft().displayGuiScreen(new CavePresetGui("Preset Creator"));
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

    /** Shorthand for sending a message to the input user. */
    private void sendMessage(ICommandSender user, String msg) {
        user.sendMessage(new TextComponentString(msg));
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