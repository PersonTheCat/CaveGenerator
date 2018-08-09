package com.personthecat.cavegenerator.world;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.config.ConfigFile;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.anticascade.CorrectionStorage;
import com.personthecat.cavegenerator.world.anticascade.CaveCompletion.ChunkCorrections;

import net.minecraft.block.state.IBlockState;
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
		
		if (!CaveInit.isAnyGeneratorEnabledForDimension(dimension))
		{
			ReplaceVanillaCaveGen.previousRavineGen.generate(world, x, z, primer);
			
			return;
		}
		
		super.generate(world, x, z, primer);
		
		if (ConfigFile.decorateWallsOption == 1)
		{			
			completePreviousCaves(dimension, x, z, primer);
		}
		else if (ConfigFile.decorateWallsOption == 2)
		{
			completePreviousCaves(x, z, primer);
		}
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
     * Called after this chunk's caves have been generated to fill in any missing
     * pieces from previous caves. Theoretically decreases world gen speed as more
     * chunks are waiting to be completed. Not obvious after generating 20k blocks
     * in a straight line, but still needs work.
     */
    protected void completePreviousCaves(int dimension, int chunkX, int chunkZ, ChunkPrimer primer)
    {    	
    	ChunkCorrections previousCaveInfo = CorrectionStorage.getCorrectionsForChunk(dimension, chunkX, chunkZ);
    	
    	for (int x = 0; x < 16; x++)
    	{
    		for (int z = 0; z < 16; z++)
    		{
    			for (int y = 0; y < 256; y++)
    			{
    				IBlockState correction = previousCaveInfo.getCorrection(x, y, z);
    				
    				if (correction != null && !correction.equals(Values.BLK_AIR))
    				{
    					if (CaveGenerator.canReplaceLessSpecific(primer.getBlockState(x, y, z)))
    					{
    						primer.setBlockState(x, y, z, correction);
    					}		
    				}
    			}
    		}
    	}
    	
    	CorrectionStorage.removeCorrectionsFromWorld(dimension, previousCaveInfo);
    }
}