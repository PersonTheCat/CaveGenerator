package com.personthecat.cavegenerator.world.feature;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.feature.LargeStalactite.Type;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.fml.common.IWorldGenerator;

public class CaveFeatureGenerator implements IWorldGenerator
{
	//To-do: Pre-calculate this.
	private static NoiseGeneratorSimplex noise;
	
	@Override
	public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider)
	{
		for (CaveGenerator generator : CaveInit.GENERATORS.values())
		{			
			if (generator.enabledGlobally && generator.canGenerateInDimension(world.provider.getDimension()))
			{
				for (LargeStalactite stalactite : generator.stalactites)
				{
					if (stalactite.useNoise()) noise = new NoiseGeneratorSimplex(new Random(rand.nextLong()));
					
					double probability = stalactite.getProbability();
					
					if (stalactite.getType().equals(Type.STALACTITE))
					{
						if (probability >= 65) generateStalactites(generator, stalactite, rand, chunkX, chunkZ, world);
						
						else generateStalactitesByQuadrant(generator, stalactite, rand, chunkX, chunkZ, world);
					}
					else
					{
						if (probability >= 65) generateStalagmites(generator, stalactite, rand, chunkX, chunkZ, world);
						
						else generateStalagmitesByQuadrant(generator, stalactite, rand, chunkX, chunkZ, world);          
					}
				}
			}
		}
	}
	
	private void generateStalactites(CaveGenerator cave, LargeStalactite gen, Random rand, int chunkX, int chunkZ, World world)
	{
		if (cave.canGenerateInBiome(world.getBiome(new BlockPos(8, 0, 8))))
		{
			double probability = gen.getProbability();
			
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					if (!gen.useNoise() || get2DNoiseFractal(noise, x, z, 1, 40, 1.0) > 0.15)
					{
						boolean previouslyAir = false;

						for (int y = gen.getMinHeight(); y < gen.getMaxHeight(); y++)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								gen.generate(world, rand, new BlockPos(x, y - 1, z));
							}
							
							previouslyAir = currentlyAir;
						}
					}
				}
			}
		}
	}
	
	private void generateStalactitesByQuadrant(CaveGenerator cave, LargeStalactite gen, Random rand, int chunkX, int chunkZ, World world)
	{
		int xOrigin = chunkX * 16, zOrigin = chunkZ * 16;
		
		generateStalactiteQuadrant(cave, gen, rand, xOrigin, zOrigin, world);
		generateStalactiteQuadrant(cave, gen, rand, xOrigin + 8, zOrigin, world);
		generateStalactiteQuadrant(cave, gen, rand, xOrigin, zOrigin + 8, world);
		generateStalactiteQuadrant(cave, gen, rand, xOrigin + 8, zOrigin + 8, world);
	}
	
	private void generateStalactiteQuadrant(CaveGenerator cave, LargeStalactite gen, Random rand, int posX, int posZ, World world)
	{
		if (cave.canGenerateInBiome(world.getBiome(new BlockPos(posX + 4, 0, posZ + 4))))
		{	
			if (!gen.useNoise() || get2DNoiseFractal(noise, posX + 4, posZ + 4, 1, 40, 1.0) > 0.15)
			{
				double probability = gen.getProbability();
				
				for (int x = posX; x < posX + 8; x++)
				{
					for (int z = posZ; z < posZ + 8; z++)
					{
						boolean previouslyAir = false;

						for (int y = gen.getMinHeight(); y < gen.getMaxHeight(); y++)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								gen.generate(world, rand, new BlockPos(x, y - 1, z));
							}
							
							previouslyAir = currentlyAir;
						}
					}
				}
			}
		}
	}
	
	private void generateStalagmites(CaveGenerator cave, LargeStalactite gen, Random rand, int chunkX, int chunkZ, World world)
	{
		if (cave.canGenerateInBiome(world.getBiome(new BlockPos(8, 0, 8))))
		{
			double probability = gen.getProbability();
			
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					if (!gen.useNoise() || get2DNoiseFractal(noise, x, z, 1, 40, 1.0) > 0.15)
					{
						boolean previouslyAir = false;

						for (int y = gen.getMaxHeight(); y > gen.getMinHeight(); y--)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								gen.generate(world, rand, new BlockPos(x, y + 1, z));
							}
							
							previouslyAir = currentlyAir;
						}
					}
				}
			}
		}
	}
	
	private void generateStalagmitesByQuadrant(CaveGenerator cave, LargeStalactite gen, Random rand, int chunkX, int chunkZ, World world)
	{
		int xOrigin = chunkX * 16, zOrigin = chunkZ * 16;
		
		generateStalagmiteQuadrant(cave, gen, rand, xOrigin, zOrigin, world);
		generateStalagmiteQuadrant(cave, gen, rand, xOrigin + 8, zOrigin, world);
		generateStalagmiteQuadrant(cave, gen, rand, xOrigin, zOrigin + 8, world);
		generateStalagmiteQuadrant(cave, gen, rand, xOrigin + 8, zOrigin + 8, world);
	}
	
	private void generateStalagmiteQuadrant(CaveGenerator cave, LargeStalactite gen, Random rand, int posX, int posZ, World world)
	{
		if (cave.canGenerateInBiome(world.getBiome(new BlockPos(posX + 4, 0, posZ + 4))))
		{	
			if (!gen.useNoise() || get2DNoiseFractal(noise, posX + 4, posZ + 4, 1, 40, 1.0) > 0.15)
			{
				double probability = gen.getProbability();
				
				for (int x = posX; x < posX + 8; x++)
				{
					for (int z = posZ; z < posZ + 8; z++)
					{
						boolean previouslyAir = false;

						for (int y = gen.getMaxHeight(); y > gen.getMinHeight(); y--)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								gen.generate(world, rand, new BlockPos(x, y + 1, z));
							}
							
							previouslyAir = currentlyAir;
						}
					}
				}
			}
		}
	}
	
	private double get2DNoiseFractal(NoiseGeneratorSimplex generator, double x, double z, int octaves, double frequency, double amplitude)
	{
		double gain = 1.0F, sum = 0.0F;
		
		for (int i = 0; i < octaves; i++)
		{
			sum += generator.getValue(x * gain / frequency, z * gain / frequency) * amplitude / gain;
			gain *= 2.0F;
		}
		
		return sum;
	}
}