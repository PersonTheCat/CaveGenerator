package com.personthecat.cavegenerator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandReloadCaves extends CommandBase
{
	@Override
	public String getName()
	{
		return "reloadcaves";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "usage test";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		CaveInit.init();
		
		sender.sendMessage(new TextComponentString("Successfully reloaded caves."));
	}
}