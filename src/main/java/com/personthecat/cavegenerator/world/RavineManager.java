package com.personthecat.cavegenerator.world;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.util.Values;

import static com.personthecat.cavegenerator.world.ReplaceVanillaCaveGen.previousRavineGen;

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
		
		if (ConfigFile.runAlongsideOtherRavineGenerators)
		{
			previousRavineGen.generate(world, x, z, primer);
		}
		else if (!CaveInit.isAnyGeneratorEnabledForDimension(dimension))
		{
			previousRavineGen.generate(world, x, z, primer);
			
			return;
		}
		
		if (!generatorReferencesSet())
		{
			setGeneratorReferences(world, x, z);
		}
		
		super.generate(world, x, z, primer);
		completePreviousCaves(x, z, primer);
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
	
    protected void completePreviousCaves(int chunkX, int chunkZ, ChunkPrimer primer)
    {
    	for (CaveGenerator generator : CaveInit.GENERATORS.values())
    	{
    		generator.finishChunkWalls(chunkX, chunkZ, primer);
    	}
    }
    
    /**
     * Only checks the first generator.
     */
    private boolean generatorReferencesSet()
    {
    	for (CaveGenerator generator : CaveInit.GENERATORS.values())
    	{
    		return generator.referencesSet();
    	}
    	
    	return false;
    }
    
    /**
     * A workaround in case CaveManager is never used (mod compatibility).
     */
    protected void setGeneratorReferences(World world, int chunkX, int chunkZ)
    {
    	if (world != null)
    	{
        	int dimension = world.provider.getDimension();    		
    		
    		for (CaveGenerator generator : CaveInit.GENERATORS.values())
        	{
        		generator.world = world;
        		generator.rand = new Random(world.getSeed());
        		generator.range = range;
        	}
    	}
    }
}