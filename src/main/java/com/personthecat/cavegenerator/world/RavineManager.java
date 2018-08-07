package com.personthecat.cavegenerator.world;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.Values;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

public class RavineManager extends MapGenBase
{	
	@Override
	public void generate(World world, int x, int z, ChunkPrimer primer)
	{
		int dimension = world.provider.getDimension();
		
		if (CaveInit.isAnyGeneratorEnabledForDimension(dimension))
		{
			ReplaceVanillaCaveGen.previousRavineGen.generate(world, x, z, primer);
			
			return;
		}
		
		super.generate(world, x, z, primer);
	}
	
	@Override
	protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer)
	{
		for (CaveGenerator generator : CaveInit.GENERATORS.values())
		{
			Biome centerBiome = world.getBiome(new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8));
			
			if (generator.canGenerate(centerBiome, world.provider.getDimension()))
			{
				if (rand.nextInt(generator.rInverseChance) == 0)
				{
					double x = (double) (chunkX * 16 + rand.nextInt(16)),
					       y = (double) (rand.nextInt(rand.nextInt(generator.rMaxHeight) + 8) + generator.rMaxHeight - generator.rMinHeight),
					       z = (double) (chunkZ * 16 + rand.nextInt(16));

					float slopeXZ = generator.rStartingSlopeXZ,
			              slopeY = generator.rStartingSlopeY,
			              scale = generator.rStartingScale;
					
					slopeXZ += rand.nextFloat() * Values.PI_TIMES_2;
					slopeY += generator.rStartingSlopeYRandFactor * (rand.nextFloat() - 0.5F);
					scale += generator.rStartingScaleRandFactor * (rand.nextFloat() * 2.0F + rand.nextFloat());

					generator.addRavine(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale, slopeXZ, slopeY, 0, generator.rStartingDistance, generator.rStartingScaleY);
				}
			}
		}
	}
}