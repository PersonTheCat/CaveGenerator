package com.personthecat.cavegenerator.commands;

import com.personthecat.cavegenerator.CaveInit;

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
		return "/reloadcaves";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		try
		{
			CaveInit.init();
		}
		catch(RuntimeException e)
		{
			sender.sendMessage(new TextComponentString(e.getMessage()));
			
			return;
		}
		
		sender.sendMessage(new TextComponentString("Successfully reloaded caves."));
	}
}