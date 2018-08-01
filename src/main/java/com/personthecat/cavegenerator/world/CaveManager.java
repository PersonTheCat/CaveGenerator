package com.personthecat.cavegenerator.world;

import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.RandomChunkSelector;
import com.personthecat.cavegenerator.util.SimplexNoiseGenerator3D;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.StoneReplacer.StoneCluster;
import com.personthecat.cavegenerator.world.anticascade.CaveCompletion.ChunkCorrections;
import com.personthecat.cavegenerator.world.anticascade.CorrectionStorage;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

public class CaveManager extends MapGenBase
{
	protected static SimplexNoiseGenerator3D noise;
	protected static NoiseGeneratorSimplex noise2D1;
	protected static NoiseGeneratorSimplex noise2D2;

	protected static RandomChunkSelector selector;
	
	protected Random indRand = new Random(12345);
	
	@Override
	public void generate(World worldIn, int x, int z, ChunkPrimer primer)
	{
		this.world = worldIn;
		this.rand.setSeed(worldIn.getSeed());

		long scaleX = rand.nextLong();
		long scaleY = rand.nextLong();
		
		setupNoiseGenerators();
		setGeneratorReferences(x, z);

		// Only needs to happen once, before tunnels are placed.
		for (CaveGenerator generator : CaveInit.GENERATORS.values())
		{
			if (generator.enabledGlobally)
			{
				setGeneratorReferences(x, z);
				
				/*
				 * It would be slightly more efficient to generate these after tunnels,
				 * But then users would have to select between clusters and layers,
				 * not both.
				 */
				if (generator.stoneClusters.length > 0) //Section is currently unused.
				{
					for (StoneCluster cluster : generator.stoneClusters)
					{
						if (selector.getBooleanForCoordinates(cluster.getID(), x, z))
						{
							int y = rand.nextInt(128); //update this
							
							generator.addGiantCluster(rand.nextLong(), x, z, primer, y, cluster.getRadius(), cluster.getNoiseLevel(), cluster.getState());
						}
					}
				}
				
				if (generator.cavernsEnabled || generator.stoneLayers.length > 0)
				{
					if (generator.biomes.length == 0 || CommonMethods.isAnyBiomeInChunk(generator.biomes, worldIn, x, z))
					{
						generator.addNoiseFeatures(x, z, primer);
					}
				}
			}
		}

		for (int diameterX = x - range; diameterX <= x + range; diameterX++)
		{
			for (int diameterZ = z - range; diameterZ <= z + range; diameterZ++)
			{
				long scrambleX = (long) diameterX * scaleX;
				long scrambleZ = (long) diameterZ * scaleY;

				rand.setSeed(scrambleX ^ scrambleZ ^ worldIn.getSeed());

				recursiveGenerate(worldIn, diameterX, diameterZ, x, z, primer);
			}
		}

		completePreviousCaves(x, z, primer);
	}
	/**
	 * A few processes here could be simplified while producing similar results,
	 * but they would still break seeds. */
	@Override
	protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer)
	{
		for (CaveGenerator generator : CaveInit.GENERATORS.values())
		{
			if (generator.enabledGlobally)
			{
				int caveFrequency = 0;
				
				if (generator.tunnelFrequency != 0)
				{
					caveFrequency = rand.nextInt(rand.nextInt(rand.nextInt(generator.tunnelFrequency) + 1) + 1);
				}
				
				//Do it in this order to maintain seeds as much as possible, just for now.
				if (generator.spawnIsolatedInverseChance != 0 && rand.nextInt(generator.spawnIsolatedInverseChance) != 0)
				{
					caveFrequency = 0;
				}

				for (int i = 0; i < caveFrequency; i++)
				{
					double x = (chunkX * 16) + rand.nextInt(16),
					       y = rand.nextInt(rand.nextInt(generator.maxHeight - generator.minHeight) + generator.minHeight),
					       z = (chunkZ * 16) + rand.nextInt(16);
					
					Biome biome = world.getBiome(new BlockPos(x, y, z));
		
					if (generator.biomes.length == 0 || ArrayUtils.contains(generator.biomes, biome))
					{
						int branches = 1;
		
						if (this.rand.nextInt(generator.spawnInSystemInverseChance) == 0)
						{
							generator.addRoom(rand.nextLong(), originalX, originalZ, primer, x, y, y);
							branches += rand.nextInt(4);
						}
						
						runGenerator(generator, branches, primer, originalX, originalZ, x, y, z);
					}
				}
			}
		}
	}
    
    private void runGenerator(CaveGenerator generator, int numDirections, ChunkPrimer primer, int originalX, int originalZ, double x, double y, double z)
    {
		for (int j = 0; j < numDirections; j++)
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
    
    /**
     * Called after this chunk's caves have been generated to fill in any missing
     * pieces from previous caves. Theoretically decreases world gen speed as more
     * chunks are waiting to be completed. Not obvious after generating 20k blocks
     * in a straight line, but still needs work.
     */
    private void completePreviousCaves(int chunkX, int chunkZ, ChunkPrimer primer)
    {
    	int dimension = world.provider.getDimension();
    	
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
    
    /**
     * Needs to happen regardless of the chance in case selection is inconsistent.
     */
    private void setGeneratorReferences(int chunkX, int chunkZ)
    {
    	if (world != null)
    	{
        	int dimension = world.provider.getDimension();    		
    		
    		for (CaveGenerator generator : CaveInit.GENERATORS.values())
        	{
        		generator.world = world;
        		generator.rand = new Random(world.getSeed());
        		generator.range = range;
        		
        		generator.xPlusOne = CorrectionStorage.getCorrectionsForChunk(dimension, chunkX + 1, chunkZ);
        		generator.xMinusOne = CorrectionStorage.getCorrectionsForChunk(dimension, chunkX - 1, chunkZ);
        		generator.zPlusOne = CorrectionStorage.getCorrectionsForChunk(dimension, chunkX, chunkZ + 1);
        		generator.zMinusOne = CorrectionStorage.getCorrectionsForChunk(dimension, chunkX, chunkZ - 1);
        	}
    	}
    }
    
    private static World previousWorld;    
    
    /**
     * Avoid setting up generators every single time generate() is called.
     */
    private void setupNoiseGenerators()
    {
    	if (world != null && !world.equals(previousWorld))
    	{
    		previousWorld = world;
    		
    		initStaticGenerators(world);
			
			StoneReplacer.setWorld(world);
    	}
    }
    
    private static void initStaticGenerators(World world)
    {
    	long seed = world.getSeed();
    	
    	noise = new SimplexNoiseGenerator3D(seed);
    	noise2D1 = new NoiseGeneratorSimplex(new Random(seed));
    	noise2D2 = new NoiseGeneratorSimplex(new Random(seed >> 4));
    	
    	selector = new RandomChunkSelector(seed);
    }
}