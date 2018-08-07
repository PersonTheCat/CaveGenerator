package com.personthecat.cavegenerator.proxy;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.server.FMLServerHandler;

public class ClientProxy extends CommonProxy
{
	@Override
	public String getSavesDirectory()
	{
		return new File(Minecraft.getMinecraft().mcDataDir, "saves").getPath();
	}
}