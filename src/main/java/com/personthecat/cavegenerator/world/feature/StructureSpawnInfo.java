package com.personthecat.cavegenerator.world.feature;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

import com.personthecat.cavegenerator.util.Direction;

public class StructureSpawnInfo
{
	private final String structure;
	private final PlacementSettings settings;
	private final BlockPos offset;
	private final IBlockState[] sources;
	private final Direction[] directions;
	private final double minBurialPercentage, chance;
	private final int frequency, minHeight, maxHeight;
	
	private BlockPos[] additionalAirMatchers = new BlockPos[0];
	private boolean debugSpawns = false;
	
	public StructureSpawnInfo(String structure, PlacementSettings settings, BlockPos offset, 
							  IBlockState[] sources, Direction[] directions, double minBurialPercentage, 
							  int frequency, double chance, int minHeight, int maxHeight)
	{
		this.structure = structure;
		this.settings = settings;
		this.offset = offset;
		this.sources = sources;
		this.directions = directions;
		this.minBurialPercentage = minBurialPercentage;
		this.frequency = frequency;
		this.chance = chance;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
	}
	
	public Template getStructure(World world)
	{
		return StructureSpawner.getTemplate(structure, world);
	}
	
	public String getStructureName()
	{
		return structure;
	}
	
	public PlacementSettings getSettings()
	{
		return settings;
	}
	
	public BlockPos getOffset()
	{
		return offset;
	}

	public IBlockState[] getMatchers()
	{
		return sources;
	}

	public Direction[] getDirections()
	{
		return directions;
	}

	public double getMinBurialPercentage()
	{
		return minBurialPercentage;
	}

	public int getFrequency()
	{
		return frequency;
	}

	public double getChance()
	{
		return chance;
	}
	
	public int getMinHeight()
	{
		return minHeight;
	}
	
	public int getMaxHeight()
	{
		return maxHeight;
	}
	
	public void setAdditionalAirMatchers(BlockPos[] matchers)
	{
		this.additionalAirMatchers = matchers;
	}
	
	public BlockPos[] getAdditionalAirMatchers()
	{
		return additionalAirMatchers;
	}
	
	public void setDebugSpawns()
	{
		this.debugSpawns = true;
	}
	
	public boolean shouldDebugSpawns()
	{
		return debugSpawns;
	}
}