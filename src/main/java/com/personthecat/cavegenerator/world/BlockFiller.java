package com.personthecat.cavegenerator.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.SimplexNoiseGenerator3D;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

public class BlockFiller
{
	private boolean spawnInPatches = false;
	
	private Direction[] directions;
	
	private double chance;
	
	private IBlockState fillWith;
	
	private IBlockState[] matchers;
	
	private int minHeight, maxHeight;
	
	private Preference preference;
	
	private SimplexNoiseGenerator3D noise;
	
	private int patchSpacing = 50;
	
	private double patchThreshold = -0.2;
	
	public BlockFiller(IBlockState state, double chance, int minHeight, int maxHeight, IBlockState[] matchers, Direction[] directions, Preference preference)
	{
		this.fillWith = state;
		this.chance = chance;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.matchers = matchers;
		this.directions = directions;
		this.preference = preference;
	}
	
	public void setSpawnInPatches()
	{
		this.spawnInPatches = true;
		
		this.noise = new SimplexNoiseGenerator3D(Block.getStateId(fillWith));
	}
	
	public boolean shouldSpawnInPatches()
	{
		return spawnInPatches;
	}
	
	public void setPatchSpacing(int frequency)
	{
		this.patchSpacing = frequency;
	}
	
	public int getPatchSpacing()
	{
		return patchSpacing;
	}
	
	public void setPatchThreshold(double threshold)
	{
		this.patchThreshold = threshold;
	}
	
	public double getPatchThreshold()
	{
		return patchThreshold;
	}
	
	public SimplexNoiseGenerator3D getNoise()
	{
		return noise;
	}
	
	public boolean hasDirections()
	{
		return directions.length > 0;
	}
	
	public Direction[] getDirections()
	{
		return directions;
	}
	
	public boolean hasDirection(Direction direction)
	{
		for (Direction dir : directions)
		{
			if (dir.equals(Direction.ALL))
			{
				return true;
			}
			else if (dir.equals(direction))
			{
				return true;
			}
		}
		
		return false;
	}

	public double getChance()
	{
		return chance;
	}
	
	public IBlockState getFillBlock()
	{
		return fillWith;
	}
	
	public static IBlockState[] getAllFillBlocks()
	{
		List<IBlockState> fillers = new ArrayList<>();
		
		for (CaveGenerator generator : CaveInit.GENERATORS.values())
		{
			if (generator.generateThroughFillers)
			{
				for (BlockFiller filler : generator.fillBlocks)
				{
					fillers.add(filler.fillWith);
				}
			}
		}
		
		return fillers.toArray(new IBlockState[0]);
	}
	
	public boolean hasMatchers()
	{
		return matchers.length > 0;
	}

	public IBlockState[] getMatchers()
	{
		return matchers;
	}

	public int getMinHeight()
	{
		return minHeight;
	}

	public int getMaxHeight()
	{
		return maxHeight;
	}
	
	public boolean canGenerateAtHeight(int y)
	{
		return y >= minHeight && y <= maxHeight;
	}

	public Preference getPreference()
	{
		return preference;
	}
	
	public static enum Preference
	{
		REPLACE_ORIGINAL,
		REPLACE_MATCH;
		
		public static Preference fromString(String s)
		{
			for (Preference p : values())
			{
				if (p.toString().equalsIgnoreCase(s))
				{
					return p;
				}
			}
			
			throw new RuntimeException(
				"Error: Matcher preference \"" + s + "\" does not exist."
			  + "The following are valid options:\n\n"
			  +  Arrays.toString(values()));
		}
	}
}
