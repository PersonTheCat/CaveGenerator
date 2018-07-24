package com.personthecat.cavegenerator.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.CaveCompletion.BlockReplacement;
import com.personthecat.cavegenerator.world.CaveCompletion.ChunkCoordinates;
import com.personthecat.cavegenerator.world.CaveCompletion.ChunkCorrections;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

public class CaveManager extends MapGenBase
{
	public static CaveManager instance;
	
	private Random worldIndependentRandom = new Random(12345); //Prevents artifacting.

	SimplexNoiseGenerator3D noise;
	NoiseGeneratorSimplex noise2D1;
	NoiseGeneratorSimplex noise2D2;
	
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
	 * From vanilla / forge. Needs to be updated for the mod.
	 */
	@Override
    public void generate(World worldIn, int x, int z, ChunkPrimer primer)
    {
        this.world = worldIn;
        this.rand.setSeed(worldIn.getSeed());
        
        long scaleX = rand.nextLong();
        long scaleY = rand.nextLong();
        
        initNoiseGenerators();

        //Only needs to happen once, before tunnels are placed.
    	for (CaveGenerator generator : CaveInit.GENERATORS.values())
        {
    		float chance = generator.generatorSelectionChance;
    		
    		if (chance == 100 || (chance != 0 && rand.nextFloat() * 100 <= chance))
    		{
        		if (generator.cavernsEnabled)
            	{
                	if (generator.biomes.length == 0 || CommonMethods.isAnyBiomeInChunk(generator.biomes, worldIn, x, z))
                	{
                		generator.addCavern(x, z, primer);
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
     * Recursively called by generate()
     * 
     * A few processes here could be simplified while producing similar results, but they would still break seeds.
     */
	@Override
    protected void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer)
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
                 			generator.addRoom(rand.nextLong(), originalX, originalZ, primer, x, y, y);
                 			branches += rand.nextInt(4);
                 		}

                 		runGenerator(generator, branches, primer, originalX, originalZ, x, y, z);
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
    	
    	try
    	{
        	for (BlockReplacement replacement : previousCaveInfo.getReplacements())
        	{    		
        		IBlockState state = replacement.getState();
        		BlockPos pos = replacement.getPos();
        		
        		if (CaveGenerator.canReplaceLessSpecific(primer.getBlockState(pos.getX(), pos.getY(), pos.getZ())))
        		{
        			primer.setBlockState(pos.getX(), pos.getY(), pos.getZ(), state);
        		}
        	}
    	}
    	
    	catch (NullPointerException ignored) {}
    	
    	CorrectionStorage.removeCorrectionsFromWorld(dimension, previousCaveInfo);
    }
    
    private void initNoiseGenerators()
    {
    	if (noise == null)
    	{
    		noise = new SimplexNoiseGenerator3D(world().getSeed());
    	}
    	
    	if (noise2D1 == null)
    	{
    		noise2D1 = new NoiseGeneratorSimplex(new Random(world().getSeed()));
    	}
    	
    	if (noise2D2 == null)
    	{
    		noise2D2 = new NoiseGeneratorSimplex(new Random(world().getSeed() >> 4));
    	}
    }
}