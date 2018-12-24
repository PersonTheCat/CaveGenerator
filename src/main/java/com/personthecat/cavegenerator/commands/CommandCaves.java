package com.personthecat.cavegenerator.commands;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.Main;
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

public class CommandCaves extends CommandBase {
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
            "`enable [<name> | all]` Enables the preset with name <name>.\n" +
            "`disable [<name> | all]` Disables the preset with name <name>.";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) { // The user did not specify which command to run. Inform them and stop.
            sender.sendMessage(new TextComponentString("Error: You need to supply an argument."));
            return;
        }
        switch (args[0]) {
            case "reload" :
                reload(sender);
                break;
            case "test" :
                test(sender);
                break;
            case "combine" :
                combine(sender, ArrayUtils.subarray(args, 1, args.length));
                break;
            case "default" :
                sender.sendMessage(new TextComponentString("Invalid argument."));
        }
    }

    /** Reloads all presets from the disk. */
    private void reload(ICommandSender sender) {
        CaveInit.initPresets(Main.instance.presets)
            .handleIfPresent((e) -> { // That didn't work. Forward the error to the user.
                sender.sendMessage(new TextComponentString(e.getMessage()));
            })
            .andThen(() -> { // All is well. Inform the user of success.
                sender.sendMessage(new TextComponentString("Successfully reloaded caves."));
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
                sender.sendMessage(new TextComponentString(
                    "Build error: Person must have typed \"night_vision\" incorrectly. Please let him know."
                ));
            }
        }
    }

    /** Combines two jsons using PresetCombiner */
    private void combine(ICommandSender sender, String[] args) {
        // To-do
    }
}