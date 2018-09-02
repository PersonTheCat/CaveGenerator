package com.personthecat.cavegenerator.world.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.Loader;

import static com.personthecat.cavegenerator.Main.logger;

public class StructureSpawner
{
	public static final Map<String, Template> STRUCTURES = new HashMap<>();
	
	public static void loadAllStructures()
	{
		File dir = new File(Loader.instance().getConfigDir() + "/cavegenerator/structures");
		
		for (File nbt : dir.listFiles())
		{
			if (nbt.getName().endsWith(".nbt"))
			{
				Template template = new Template();
				
				try
				{
					template.read(CompressedStreamTools.readCompressed(new FileInputStream(nbt)));
				}
				catch (IOException e)
				{
					throw new RuntimeException("Error: Could not read structure file " + nbt.getName());
				}
				
				logger.info("Successfully loaded " + nbt.getName());
				
				if (template.getSize().getX() > 15 || template.getSize().getZ() > 15)
				{
					logger.warn("Large structures are not yet fully supported. Expect cascading generation lag caused by " + nbt.getName() + ".");
				}
				
				STRUCTURES.put(nbt.getName().replaceAll(".nbt", ""), template);
			}
		}
	}
	
	public static Template getTemplate(String fileOrResource, World world)
	{
		fileOrResource = fileOrResource.replaceAll(".nbt", "");
		
		if (STRUCTURES.containsKey(fileOrResource))
		{
			return STRUCTURES.get(fileOrResource);
		}
		else
		{
			if (!world.isRemote)
			{
				WorldServer worldServer = (WorldServer) world;
				MinecraftServer mcServer = world.getMinecraftServer();
				TemplateManager manager = worldServer.getStructureTemplateManager();
				ResourceLocation location = new ResourceLocation(fileOrResource);
				
				Template template = null;
				
				try
				{
					template = manager.get(mcServer, location);
				}
				catch (NullPointerException e)
				{
					templateNotFoundError(fileOrResource);
				}
				
				if (template == null)
				{
					templateNotFoundError(fileOrResource);
				}
				
				return template;
			}
			else throw new RuntimeException(
					"Error: No template could be found with the name " + fileOrResource + ". "
				  + "Make sure this name refers to a structure under /cavegenerator/structures/ or an existing registry name.");
		}
	}
	
	private static  void templateNotFoundError(String name)
	{
		throw new RuntimeException("Error: no structure named " + name + " was found. Please verify that this refers to a valid structure.");
	}
	
	/**
	 * Does not account for offsets. Must be calculated beforehand.
	 */
	public static void SpawnStructure(Template template, PlacementSettings settings, World world, BlockPos pos)
	{
		if (template != null)
		{
			IBlockState state = world.getBlockState(pos);
			
			world.notifyBlockUpdate(pos, state, state, 3);
			
			template.addBlocksToWorld(world, pos, settings);
		}
	}
}