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
	private final boolean useNoise;
	private final double probability;
	private final IBlockState state;
	private final int maxLength, minHeight, maxHeight;
	private final Type type;
	
	public LargeStalactite(int maxLength, double probability, IBlockState state, int minHeight, int maxHeight, Type type)
	{
		this.useNoise = false;
		this.maxLength = maxLength;
		this.probability = probability;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.state = state;
		this.type = type;
	}
	
	public LargeStalactite(boolean useNoise, int maxLength, double probability, IBlockState state, int minHeight, int maxHeight, Type type)
	{
		this.useNoise = useNoise;
		this.maxLength = maxLength;
		this.probability = probability;
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
			BlockPos newPos = type.equals(Type.STALACTITE) ? pos.down() : pos.up();
			
			if (!world.getBlockState(newPos).equals(Values.BLK_AIR) || localRand.nextInt(2) == 0)
			{
				break;
			}
			
			world.setBlockState(newPos, state, 16);
		}
		
		return true;
	}
	
	public boolean useNoise()
	{
		return useNoise;
	}
	
	public double getProbability()
	{
		return probability;
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