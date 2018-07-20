package com.personthecat.cavegenerator.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class CommonMethods
{
	public static Biome getBiome(String biomeName)
	{
		return ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeName));
	}
	
	public static Biome getBiome(int biomeNumber)
	{
		return Biome.getBiomeForId(biomeNumber);
	}
	
	public static Biome[] getBiomes(Type biomeType)
	{
		return BiomeDictionary.getBiomes(biomeType).toArray(new Biome[0]);
	}
	
	public static IBlockState getBlockState(String registryName)
	{
		String[] split = registryName.split(":");
		
		ResourceLocation location = null;
		int meta = 0;
		
		if (StringUtils.isNumeric(split[split.length - 1]))
		{
			meta = Integer.parseInt(split[split.length - 1]);
			location = new ResourceLocation(registryName.replaceAll(":" + split[split.length - 1], ""));
		}
		
		else if (split.length == 1 || split.length == 2)
		{
			location = new ResourceLocation(registryName);
		}
		
		else System.err.println("Syntax error: Could not determine blockstate from " + registryName);
		
		return ForgeRegistries.BLOCKS.getValue(location).getStateFromMeta(meta);
	}
	
	public static void copyPresetFiles()
	{
		File exampleFolder = new File(Loader.instance().getConfigDir().getPath() + "/cavegenerator/example_presets");
		
		if (!exampleFolder.exists())
		{
			exampleFolder.mkdirs();
			
			for (String fileName : new String[] {"flooded_vanilla",	"large_caves", "spirals", "tunnels"})
			{
				copyPreset("assets/cavegenerator/presets/" + fileName, exampleFolder.getPath() + "/" + fileName);
			}
		}
		
		File presetFolder = new File(Loader.instance().getConfigDir().getPath() + "/cavegenerator/presets");
		
		if (!presetFolder.exists())
		{
			presetFolder.mkdir();
			
			copyPreset("assets/cavegenerator/presets/vanilla", presetFolder.getPath() + "/vanilla");
		}
	}
	
	private static void copyPreset(String fromLocation, String toLocation)
	{
		try
		{
			InputStream copyMe = Minecraft.class.getClassLoader().getResourceAsStream(fromLocation + ".json");
			FileOutputStream output = new FileOutputStream(toLocation + ".json");
			
			copyStream(copyMe, output, 1024);
			
			output.close();
		}
		
		catch (IOException e) {System.err.println("Error: Could not copy example presets.");}
	}
	
	private static void copyStream(InputStream input, OutputStream output, int bufferSize) throws IOException
    {
		byte[] buffer = new byte[bufferSize];
		int length;

		while ((length = input.read(buffer)) > 0)
		{
			output.write(buffer, 0, length);
		}
    }
}