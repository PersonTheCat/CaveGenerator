package com.personthecat.cavegenerator.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;

import com.personthecat.cavegenerator.Main;

import static com.personthecat.cavegenerator.Main.logger;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
		
		else logger.warn("Syntax error: Could not determine blockstate from " + registryName);
		
		return ForgeRegistries.BLOCKS.getValue(location).getStateFromMeta(meta);
	}
	
	/**
	 * Tests each corner and then center for any biome on the list.
	 * Faster than testing all 256 coordinates, more accurate than
	 * testing the center.
	 */
	public static boolean isAnyBiomeInChunk(Biome[] biomes, World world, int chunkX, int chunkZ)
	{
		for (BlockPos pos : new BlockPos[] {
		     new BlockPos((chunkX * 16) + 0, 0, (chunkZ * 16) + 0),
		     new BlockPos((chunkX * 16) + 0, 0, (chunkZ * 16) + 15),
		     new BlockPos((chunkX * 16) + 15, 0, (chunkZ * 16) + 0),
		     new BlockPos((chunkX * 16) + 15, 0, (chunkZ * 16) + 15),
		     new BlockPos((chunkX * 16) + 8, 0, (chunkZ * 16) + 8)
		})
		{
			Biome current = world.getBiome(pos);
			
			for (Biome biome : biomes)
			{
				if (biome.equals(current)) return true;
			}
		}
		
		return false;
	}
	
	public static void copyPresetFiles()
	{
		File exampleFolder = new File(Loader.instance().getConfigDir().getPath() + "/cavegenerator/example_presets");
		
		if (!exampleFolder.exists()) exampleFolder.mkdirs();
		
		for (String fileName : new String[] {"flooded_vanilla",	"large_caves", "spirals", "tunnels", 
		                                     "caverns", "stalactites", "stone_layers", "ravines", 
		                                     "stone_clusters", "stalactites_large", "vanilla",
		                                     "underground_forest"})
		{
			copyFile("assets/cavegenerator/presets/" + fileName + ".json", exampleFolder.getPath() + "/" + fileName + ".json");
		}
		
		File presetFolder = new File(Loader.instance().getConfigDir().getPath() + "/cavegenerator/presets");
		
		if (!presetFolder.exists())
		{
			presetFolder.mkdir();
			
			copyFile("assets/cavegenerator/presets/vanilla.json", presetFolder.getPath() + "/vanilla.json");
		}
	}
	
	public static void copyExampleStructures()
	{
		File dir = new File(Loader.instance().getConfigDir() + "/cavegenerator/structures");
		
		if (!dir.exists())
		{
			dir.mkdirs();
			
			for (String fileName : new String[] {"hanging_spawner", "red_mushroom"})
			{
				copyFile("assets/cavegenerator/structures/" + fileName + ".nbt", dir.getPath() + "/" + fileName + ".nbt");
			}
		}
	}
	
	private static void copyFile(String fromLocation, String toLocation)
	{
		try
		{
			InputStream copyMe = Main.class.getClassLoader().getResourceAsStream(fromLocation);
			FileOutputStream output = new FileOutputStream(toLocation);
			
			copyStream(copyMe, output, 1024);
			
			output.close();
		}
		
		catch (IOException e) { logger.warn("Error: Could not copy example files."); }
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