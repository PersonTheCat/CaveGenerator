package com.personthecat.cavegenerator.proxy;

import net.minecraftforge.fml.server.FMLServerHandler;

public class CommonProxy
{
	public String getSavesDirectory()
	{
		return FMLServerHandler.instance().getSavesDirectory().getPath();
	}
}