package com.personthecat.cavegenerator.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.personthecat.cavegenerator.CaveInit;

import net.minecraft.block.state.IBlockState;

public class BlockFiller
{
	private Direction[] directions;
	
	private double chance;
	
	private IBlockState fillWith;
	
	private IBlockState[] matchers;
	
	private int minHeight, maxHeight;
	
	private Preference preference;
	
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
	
	public boolean hasDirections()
	{
		return directions.length > 0;
	}
	
	public Direction[] getDirections()
	{
		return directions;
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
	
	public static enum Direction
	{
		UP,
		DOWN,
		SIDE,
		ALL;
		
		public static Direction fromString(String s)
		{
			for (Direction d : values())
			{
				if (d.toString().equalsIgnoreCase(s))
				{
					return d;
				}
			}
			
			throw new RuntimeException(
				"Error: Direction \"" + s + "\" does not exist."
			  + "The following are valid options:\n\n"
			  +  Arrays.toString(values()));
		}
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
