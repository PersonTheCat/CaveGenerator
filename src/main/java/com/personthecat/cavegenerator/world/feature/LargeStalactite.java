package com.personthecat.cavegenerator.world.feature;

import java.util.Random;

import com.personthecat.cavegenerator.util.Values;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraft.world.gen.feature.WorldGenerator;

public class LargeStalactite extends WorldGenerator
{
	private final double chance;
	private final IBlockState state;
	private final int maxLength, minHeight, maxHeight;
	private final Type type;
	
	private boolean spawnInPatches;
	private double patchThreshold = 0.15;
	private int patchSpacing = 40;
	
	public LargeStalactite(int maxLength, double chance, IBlockState state, int minHeight, int maxHeight, Type type)
	{
		this.maxLength = maxLength;
		this.chance = chance;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.state = state;
		this.type = type;
	}	
	
	@Override
	public boolean generate(World world, Random localRand, BlockPos pos)
	{
		world.setBlockState(pos, state, 16);
		
		for (int i = 1; i < maxLength; i++)
		{
			pos = type.equals(Type.STALACTITE) ? pos.down() : pos.up();
			
			if (world.getBlockState(pos).isOpaqueCube() || localRand.nextInt(2) == 0)
			{
				break;
			}
			
			world.setBlockState(pos, state, 16);
		}
		
		return true;
	}
	
	public void setSpawnInPatches()
	{
		this.spawnInPatches = true;
	}
	
	public boolean shouldSpawnInPatches()
	{
		return spawnInPatches;
	}
	
	public void setPatchThreshold(double threshold)
	{
		this.patchThreshold = threshold;
	}
	
	public double getPatchThreshold()
	{
		return patchThreshold;
	}
	
	public void setPatchSpacing(int spacing)
	{
		this.patchSpacing = spacing;
	}
	
	public int getPatchSpacing()
	{
		return patchSpacing;
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
	
	public Type getType()
	{
		return type;
	}
	
	public static enum Type
	{
		STALAGMITE,
		STALACTITE;
	}
}