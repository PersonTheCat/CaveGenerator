package com.personthecat.cavegenerator.world;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Values;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

public class CaveManager extends MapGenBase
{
	public static CaveManager instance;
	
	private Random worldIndependentRandom = new Random(12345); //Prevents artifacting.
	
	public CaveManager()
	{
		instance = this;
	}
	
	public Random indRand()
	{
		return worldIndependentRandom;
	}
	
	public Random rand()
	{
		return rand;
	}
	
	public int range()
	{
		return range;
	}
	
	public World world()
	{
		return world;
	}
	
    /**
     * Recursively called by generate()
     * 
     * A few processes here could be simplified while producing similar results, but they would still break seeds.
     */
    protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer chunkPrimerIn)
    {   
        //These happen before the loop to avoid breaking seeds.
    	int randGenFrequency = rand.nextInt(rand.nextInt(rand.nextInt(15) + 1) + 1); 
        if (rand.nextInt(7) != 0) randGenFrequency = 0;
        
    	for (CaveGenerator generator : CaveInit.GENERATORS.values())
        {            
        	for (int i = 0; i < randGenFrequency; i++)
        	{
            	float chance = generator.generatorSelectionChance;
        		
        		if (chance == 100 || (chance != 0 && rand.nextFloat() * 100 <= chance))
        		{
            		double x = (chunkX * 16) + rand.nextInt(16),
                           y = rand.nextInt(rand.nextInt(generator.maxHeight - generator.minHeight) + generator.minHeight), //To-do: make this more accurate without breaking seeds.
                           z = (chunkZ * 16) + rand.nextInt(16);
             		
            		Biome biome = world.getBiome(new BlockPos(x, y, z));
            		
             		if (generator.biomes.length == 0 || ArrayUtils.contains(generator.biomes, biome))
             		{
                 		int branches = 1;

                 		if (this.rand.nextInt(generator.spawnInSystemInverseChance) == 0) 
                 		{
                 			generator.addRoom(rand.nextLong(), originalX, originalZ, chunkPrimerIn, x, y, y);
                 			branches += rand.nextInt(4);
                 		}

                 		runGenerator(generator, branches, chunkPrimerIn, originalX, originalZ, x, y, z);
             		}
        		}
        	}
        }
    }
    
    private void runGenerator(CaveGenerator generator, int frequency, ChunkPrimer primer, int originalX, int originalZ, double x, double y, double z)
    {
		for (int j = 0; j < frequency; j++)
		{
			float slopeXZ = generator.startingSlopeXZ,
         	      slopeY = generator.startingSlopeY,
         	      scale = generator.startingScale;
         	
        	slopeXZ += generator.startingSlopeXZRandFactor *(rand.nextFloat() * Values.PI_TIMES_2);
        	slopeY += generator.startingSlopeYRandFactor * (rand.nextFloat() - 0.5F);
        	scale += generator.startingScaleRandFactor * (rand.nextFloat() * 2.0F + rand.nextFloat());

        	if (this.rand.nextInt(10) == 0) //Randomly increase size. From vanilla. Needed for seeds.
        	{
        		scale *= rand.nextFloat() * rand.nextFloat() * 3.0F + 1.0F;
        	}

        	generator.addTunnel(rand.nextLong(), originalX, originalZ, primer, x, y, z, scale, slopeXZ, slopeY, 0, generator.startingDistance, generator.startingScaleY);
        }
    }
}