package com.personthecat.cavegenerator.world;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.Values;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

public class RavineManager extends MapGenBase
{	
	@Override
	protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer)
	{
		for (CaveGenerator generator : CaveInit.GENERATORS.values())
		{
			if (generator.enabledGlobally)
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