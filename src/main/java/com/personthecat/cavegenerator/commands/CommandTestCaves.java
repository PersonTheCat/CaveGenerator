package com.personthecat.cavegenerator.commands;


import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;

public class CommandTestCaves extends CommandBase
{
	@Override
	public String getName()
	{
		return "testcaves";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "/testcaves";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		Entity ent = sender.getCommandSenderEntity();
		
		if (ent instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer) ent;
			
			player.setGameType(GameType.SPECTATOR);
			player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("night_vision"), 999999999, 0, true, false));
		}
	}
}