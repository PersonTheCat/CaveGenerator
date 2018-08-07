package com.personthecat.cavegenerator.world;

import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.MoreObjects;
import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.SimplexNoiseGenerator3D;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.BlockFiller.Direction;
import com.personthecat.cavegenerator.world.BlockFiller.Preference;
import com.personthecat.cavegenerator.world.StoneReplacer.StoneCluster;
import com.personthecat.cavegenerator.world.StoneReplacer.StoneLayer;
import com.personthecat.cavegenerator.world.anticascade.CorrectionStorage;
import com.personthecat.cavegenerator.world.anticascade.CaveCompletion.BlockReplacement;
import com.personthecat.cavegenerator.world.anticascade.CaveCompletion.ChunkCorrections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import scala.actors.threadpool.Arrays;

public class CaveGenerator
{	
	public boolean 
	
		enabledGlobally = true,
		useBiomeBlacklist = false,
		useDimensionBlacklist = false,
		cavernsEnabled = false,
		fastCavernYSmoothing = false,
		generateThroughFillers = true,
		noiseYReduction = true; //Vanilla function
	
	/**
	 * Avoid repeatedly calling {@link CorrectionStorage#getCorrectionsForChunk()}.
	 */
	protected ChunkCorrections
	
		xPlusOne,
		xMinusOne,
		zPlusOne,
		zMinusOne;
	
	public Extension
	
		extBeginning = Extension.NONE,
		extRand = Extension.VANILLA_BRANCHES,
		extEnd = Extension.NONE;

	/**
	 * To-do: Organize some of these fields into separate classes?
	 */
	public float 
	
		//Room values
		roomScale = 6.0F,
		roomScaleY = 0.5F,
		
		//Normal tunnel values
		twistXZExponent = 1.0F,
		twistXZFactor = 0.75F,
		twistXZRandFactor = 4.0F, //Favor exaggerations in horizontal twist 2x vertical
		twistYExponent = 1.0F,
		twistYFactor = 0.9F,
		twistYRandFactor = 2.0F,
		scaleExponent = 1.0F,
		scaleFactor = 1.0F,
		scaleRandFactor = 0.0F,
		scaleYExponent = 1.0F,
		scaleYFactor = 1.0F,
		scaleYRandFactor = 0.0F,

		//Normal ravine values
		rTwistXZExponent = 1.0F,
		rTwistXZFactor = 0.5F,
		rTwistXZRandFactor = 4.0F,
		rTwistYExponent = 1.0F,
		rTwistYFactor = 0.8F,
		rTwistYRandFactor = 2.0F,
		rScaleExponent = 1.0F,
		rScaleFactor = 1.0F,
		rScaleRandFactor = 0.0F,
		rScaleYExponent = 1.0F,
		rScaleYFactor = 1.0F,
		rScaleYRandFactor = 0.0F,
		rNoiseYFactor = 0.7F, //Similar to noiseYReduction, but not random.
		
		//Starting tunnel values
		startingTwistXZ = 0.0F,
		startingTwistY = 0.0F,
		startingScale = 0.0F,
		startingScaleRandFactor = 1.0F,
		startingScaleY = 1.0F,
		startingSlopeXZ = 0.0F,
		startingSlopeXZRandFactor = 1.0F,
		startingSlopeY = 0.0F,
		startingSlopeYRandFactor = 0.25F,
		
		//Starting ravine values
		rStartingTwistXZ = 0.0F,
		rStartingTwistY = 0.0F,
		rStartingScale = 0.0F,
		rStartingScaleRandFactor = 2.0F,
		rStartingScaleY = 3.0F,
		rStartingSlopeXZ = 0.0F,
		rStartingSlopeXZRandFactor = 1.0F,
		rStartingSlopeY = 0.0F,
		rStartingSlopeYRandFactor = 0.25F,
		
		//Cavern things
		cavernSelectionThreshold = 0.6F, //Calculated from scale in PresetReader.
		cavernFrequency = 70.0F,
		cavernScaleY = 0.5F,
		cavernAmplitude = 1.0F; //Effect is very small.
	
	public int
		
		//Most unchanging values
		minHeight = 8,
		maxHeight = 128,
		
		lavaMaxHeight = 10,
		
		cavernMinHeight = 10,
		cavernMaxHeight = 50,
		
		rMinHeight = 20,
		rMaxHeight = 40,

		//Starting tunnel values
		startingDistance = 0,
		
		//Starting ravine Values
		rStartingDistance = 0,
		
		//Probabilities (1 / (# + 1))
		spawnInSystemInverseChance = 4,
		spawnIsolatedInverseChance = 7,
		tunnelFrequency = 15,
	
		rInverseChance = 50;

	public String
	
		extBeginningPreset,
		extRandPreset,
		extEndPreset;
	
	public int[] dimensions = new int[0];
	
	public Biome[] biomes = new Biome[0];
	
	public BlockFiller[] fillBlocks = new BlockFiller[0];
	
	public StoneCluster[] stoneClusters = new StoneCluster[0];
	
	/**
	 * Make sure to set these in order by maxHeight;
	 */
	public StoneLayer[] stoneLayers = new StoneLayer[0];
	
	private final float[] mut = new float[1024];
	
	private static Random indRand = new Random(12345); //Prevents artifacting.	
	
	protected Random rand;
	
	protected int range;
	
	protected World world;
	
	/**
	 * Neatly checks enabledGlobally, ...InBiome(), and ...InDimension().
	 */
	public boolean canGenerate(Biome biome, int dimension)
	{
		return enabledGlobally && canGenerateInBiome(biome) && canGenerateInDimension(dimension);
	}
	
	public boolean canGenerateInBiome(Biome biome)
	{
		if (biomes.length == 0) return true;
		
		if (useBiomeBlacklist)
		{
			return !ArrayUtils.contains(biomes, biome);
		}
		
		return ArrayUtils.contains(biomes, biome);
	}
	
	public boolean canGenerateInDimension(int dimension)
	{
		if (dimensions.length == 0) return true;
		
		if (useDimensionBlacklist)
		{
			return !ArrayUtils.contains(dimensions, dimension);
		}
		
		return ArrayUtils.contains(dimensions, dimension);
	}
	
	/**
	 * The type of tunnel to create at the specified location.
	 */
	public static enum Extension
	{
		VANILLA_BRANCHES,
		MATCHING_BRANCHES,
		PRESET,
		ROOM,
		ROOM2, //To-do
		STRUCTURE, //To-do
		NONE;
		
		public static Extension fromString(String s)
		{
			for (Extension e : values())
			{
				if (e.toString().equalsIgnoreCase(s))
				{
					return e;
				}
			}
			
			String[] split = s.split(":");
			
			if (split[0].equals("preset"))
			{
				if (CaveInit.isPresetRegistered(split[1]))
				{
					return PRESET;
				}
			}

			throw new RuntimeException(
					"Error: Extension \"" + s + "\" does not exist."
				  + "The following are valid options:\n\n"
				  +  Arrays.toString(values()) + "\n"
				  +  Arrays.toString(CaveInit.GENERATORS.keySet().toArray()));
		}
		
		private boolean place(CaveGenerator generator, String altPreset, Random rand, long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ, float scale, float slopeXZ, float slopeY, int startingPoint, int distance, double scaleY)
		{
//			System.out.println("placing extension...");
			
			switch(this)
			{
				case VANILLA_BRANCHES:
					
					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, rand.nextFloat() * 0.5F + 0.5F, slopeXZ - Values.PI_OVER_2, slopeY / 3.0F, startingPoint, distance, 1.0D);
					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, rand.nextFloat() * 0.5F + 0.5F, slopeXZ + Values.PI_OVER_2, slopeY / 3.0F, startingPoint, distance, 1.0D);
					
					return true;
					
				case MATCHING_BRANCHES:

					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ - Values.PI_OVER_2, slopeY / 3.0F, startingPoint, distance, scaleY);
					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ + Values.PI_OVER_2, slopeY / 3.0F, startingPoint, distance, scaleY);
					
					return true;
					
				case PRESET:
					
					CaveGenerator altGenerator = CaveInit.GENERATORS.get(altPreset);
					
					float direction = rand.nextBoolean() ? -Values.PI_OVER_2 : Values.PI_OVER_2;
					
					altGenerator.addTunnel(seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ + direction, slopeY, startingPoint, distance, scaleY);
					
					return true;

				case ROOM:

					generator.addRoom(seed, chunkX, chunkZ, primer, posX, posY, posZ);
					
					return true;

				default: return false;
			}
		}
	}
	
	protected void addRoom(long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ)
	{
		addTunnel(seed, chunkX, chunkZ, primer, posX, posY, posZ, 1.0F + rand.nextFloat() * roomScale, 0.0F, 0.0F, -1, -1, roomScaleY);
	}

	/**
	 * Mod of {@link MapGenCaves#addTunnel} by PersonTheCat. Supports object-specific replacement
	 * of most variables, as well as a few new equations to support additional shapes, noise control,
	 * and block replacement alternatives.
	 * 
	 * @param seed          The world's seed. Used to create a local Random object for regen parity.
	 * @param chunkX        Chunk x coordinate.
	 * @param chunkZ        Chunk z coordinate.
	 * @param primer        Provides information about the current chunk.
	 * @param posX          Block x coordinate.
	 * @param posY          Block y coordinate.
	 * @param posZ          Block z coordinate.
	 * @param scale         Overall cave size (radius?).
	 * @param slopeXZ       The horizontal angle of the cave.
	 * @param slopeY        The vertical angle of the cave.
	 * @param startingPoint Not entirely sure. Seems to indicate whether a room should be spawned instead of a tunnel. Also used for determining branch length.
	 * @param distance      The length of the tunnel. 0 = get distance from MapGenBase.
	 * @param scaleY        A vertical cave size multiple. 1.0 = same as scale.
	 */
	protected void addTunnel(long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ, float scale, float slopeXZ, float slopeY, int startingPoint, int distance, double scaleY)
	{
		float twistXZ = startingTwistXZ, twistY = startingTwistY;
		
		double centerX = chunkX * 16 + 8,
		       centerZ = chunkZ * 16 + 8;

		Random localRandom = new Random(seed);
		
		if (distance <= 0)
		{
			int chunkDistance = range * 16 - 16;
			distance = chunkDistance - localRandom.nextInt(chunkDistance / 4);
		}
		
		boolean isRoom = startingPoint == -1;
		if (isRoom) startingPoint = distance / 2;

		int randomSegmentIndex = localRandom.nextInt(distance / 2) + distance / 4,
		    position = startingPoint;

		for (boolean randomNoiseCorrection = localRandom.nextInt(6) == 0; position < distance; position++)
		{
			double stretchXZ = 1.5D + (MathHelper.sin(position * (float) Math.PI / distance) * scale),
			       stretchY = stretchXZ * scaleY;
			
			float cos = MathHelper.cos(slopeY),
			      sin = MathHelper.sin(slopeY);
			
			posX += MathHelper.cos(slopeXZ) * cos;
			posY += sin;
			posZ += MathHelper.sin(slopeXZ) * cos;
			
			if (noiseYReduction)
			{
				slopeY *= randomNoiseCorrection ? 0.92F : 0.7F;
			}
			
			//Slopes are recalculated on subsequent iterations.
			slopeXZ += twistXZ * 0.1F;
			slopeY += twistY * 0.1F;

			//Rotates the beginning of the line around the end.
			twistY = adjustTwist(twistY, localRandom, twistYExponent, twistYFactor, twistYRandFactor);
			
			//Positive is counterclockwise, negative is clockwise.
			twistXZ = adjustTwist(twistXZ, localRandom, twistXZExponent, twistXZFactor, twistXZRandFactor);

			scale = (float) adjustScale(scale, indRand, scaleExponent, scaleFactor, scaleRandFactor);			
			scaleY = adjustScale(scaleY, indRand, scaleYExponent, scaleYFactor, scaleYRandFactor);

			if (!isRoom && scale > 1.0F && distance > 0)
			{
				if (position == randomSegmentIndex)
				{
					if (extRand.place(this, extBeginningPreset, localRandom, seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ, slopeY, position, distance, scaleY))
					{
						return;
					}
				}
				
				if (position == startingPoint)
				{
					if (extBeginning.place(this, extRandPreset, localRandom, seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ, slopeY, position, distance, scaleY))
					{
						return;
					}
				}
				
				if (position == distance)
				{
					if (extEnd.place(this, extEndPreset, localRandom, seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ, slopeY, position, distance, scaleY))
					{
						return;
					}
				}
			}

			if (isRoom || localRandom.nextInt(4) != 0)
			{
				double fromCenterX = posX - centerX,
				       fromCenterZ = posZ - centerZ,
				       currentPos = distance - position,
				       adjustedScale = scale + 18.0F;

				if (((fromCenterX * fromCenterX) + (fromCenterZ * fromCenterZ) - (currentPos * currentPos)) > (adjustedScale * adjustedScale))
				{
					return;
				}

				if (posX >= centerX - 16.0D - stretchXZ * 2.0D && posZ >= centerZ - 16.0D - stretchXZ * 2.0D && posX <= centerX + 16.0D + stretchXZ * 2.0D && posZ <= centerZ + 16.0D + stretchXZ * 2.0D)
				{
					int startX = applyLimitXZ(MathHelper.floor(posX - stretchXZ) - chunkX * 16 - 1),
					    endX = applyLimitXZ(MathHelper.floor(posX + stretchXZ) - chunkX * 16 + 1),
					    startY = applyLimitY(MathHelper.floor(posY - stretchY) - 1),
					    endY = applyLimitY(MathHelper.floor(posY + stretchY) + 1),
					    startZ = applyLimitXZ(MathHelper.floor(posZ - stretchXZ) - chunkZ * 16 - 1),
					    endZ = applyLimitXZ(MathHelper.floor(posZ + stretchXZ) - chunkZ * 16 + 1);

					if (!shouldTestForWater(endY) || !testForWater(primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ))
					{
						if (fillBlocks != null)
						{
							if (shouldPregenerateAir()) //First generate air so it can be replaced. Only needed if blocks should be matched directionally.
							{
								replaceSection(true, primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);	
							}
							
							replaceSection(false, primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);
						}
						
						else replaceSection(false, primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);
					}
					
					if (isRoom)	break;
				}
			}
		}
	}
	
    protected void addRavine(long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ, float scale, float slopeXZ, float slopeY, int startingPoint, int distance, double scaleY)
    {
        Random localRandom = new Random(seed);
        
        double centerX = (double)(chunkX * 16 + 8);
        double centerZ = (double)(chunkZ * 16 + 8);
        
        float twistXZ = rStartingTwistXZ, twistY = rStartingTwistY;

		if (distance <= 0)
		{
			int chunkDistance = range * 16 - 16;
			distance = chunkDistance - localRandom.nextInt(chunkDistance / 4);
		}
		
        float f2 = 1.0F;

        for (int j = 0; j < 256; j++)
        {
            if (j == 0 || localRandom.nextInt(3) == 0)
            {
                f2 = 1.0F + localRandom.nextFloat() * localRandom.nextFloat();
            }

            this.mut[j] = f2 * f2;
        }

        for (; startingPoint < distance; startingPoint++)
        {
            double stretchXZ = 1.5D + (double) (MathHelper.sin((float) startingPoint * (float) Math.PI / (float) distance) * scale);
            double stretchY = stretchXZ * scaleY;
            
            stretchXZ *= ((double) localRandom.nextFloat() * 0.25D + 0.75D);
            stretchY *= ((double) localRandom.nextFloat() * 0.25D + 0.75D);
            
            float cos = MathHelper.cos(slopeY);
            float sin = MathHelper.sin(slopeY);
            
            posX += (double) (MathHelper.cos(slopeXZ) * cos);
            posY += (double) sin;
            posZ += (double) (MathHelper.sin(slopeXZ) * cos);
            
            slopeY *= rNoiseYFactor;
            
            slopeY += twistY * 0.05F;
            slopeXZ += twistXZ * 0.05F;
            
            twistY = adjustTwist(twistY, localRandom, rTwistYExponent, rTwistYFactor, rTwistYRandFactor);
            twistXZ = adjustTwist(twistXZ, localRandom, rTwistXZExponent, rTwistXZFactor, rTwistXZRandFactor);
            
			scale = (float) adjustScale(scale, indRand, rScaleExponent, rScaleFactor, rScaleRandFactor);			
			scaleY = adjustScale(scaleY, indRand, rScaleYExponent, rScaleYFactor, rScaleYRandFactor);

            if (localRandom.nextInt(4) != 0)
            {
                double fromCenterX = posX - centerX;
                double fromCenterZ = posZ - centerZ;
                double currentPos = (double)(distance - startingPoint);
                double adjustedScale = (double)(scale + 2.0F + 16.0F);

                if (fromCenterX * fromCenterX + fromCenterZ * fromCenterZ - currentPos * currentPos > adjustedScale * adjustedScale)
                {
                    return;
                }

                if (posX >= centerX - 16.0D - stretchXZ * 2.0D && posZ >= centerZ - 16.0D - stretchXZ * 2.0D && posX <= centerX + 16.0D + stretchXZ * 2.0D && posZ <= centerZ + 16.0D + stretchXZ * 2.0D)
                {
					int startX = applyLimitXZ(MathHelper.floor(posX - stretchXZ) - chunkX * 16 - 1),
					    endX = applyLimitXZ(MathHelper.floor(posX + stretchXZ) - chunkX * 16 + 1),
					    startY = applyLimitY(MathHelper.floor(posY - stretchY) - 1),
					    endY = applyLimitY(MathHelper.floor(posY + stretchY) + 1),
					    startZ = applyLimitXZ(MathHelper.floor(posZ - stretchXZ) - chunkZ * 16 - 1),
					    endZ = applyLimitXZ(MathHelper.floor(posZ + stretchXZ) - chunkZ * 16 + 1);

                    if (!shouldTestForWater(endY) || !testForWater(primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ))
                    {
                    	if (fillBlocks != null)
                    	{
                        	if (shouldPregenerateAir())
                        	{
                        		replaceSectionFromMut(true, primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);
                        	}
                        	
                        	replaceSectionFromMut(false, primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);
                    	}

                    	else replaceSectionFromMut(false, primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);
                    }
                }
            }
        }
    }
	
	/**
	 * Generates caverns directly based on the world seed using an open simplex
	 * noise generator. Would ideally be called as stone gets placed (?).
	 * 
	 * Where caverns cannot generate, stone layers are placed, if applicable.
	 */
	protected void addNoiseFeatures(int chunkX, int chunkZ, ChunkPrimer primer)
	{
		boolean airOnly = true;
		
		int layerMaxHeight = 0;
		
		if (stoneLayers.length > 0)
		{
			layerMaxHeight = stoneLayers[stoneLayers.length - 1].getMaxHeight();
		}
		
		int noiseMaxHeight = cavernMaxHeight > layerMaxHeight ? cavernMaxHeight : layerMaxHeight;
		
		for (int i = 0; i < (shouldPregenerateAir() ? 2 : 1); i++)
		{
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					for (int y = noiseMaxHeight + 2; y >= 6; y--)
					{
						decidePlace(airOnly, chunkX, chunkZ, primer, x, y, z);
					}
				}
			}
			
			airOnly = false;
		}
	}
	
	private void decidePlace(boolean airOnly, int chunkX, int chunkZ, ChunkPrimer primer, int x, int y, int z)
	{
		if (!airOnly || !primer.getBlockState(x, y, z).equals(Values.BLK_WATER))
		{
			double actualX = x + (chunkX * 16);
			double actualZ = z + (chunkZ * 16);

			if (cavernsEnabled)
			{
				int minY = cavernMinHeight, maxY = cavernMaxHeight;
				double selectionThreshold = cavernSelectionThreshold;
				
				if (fastCavernYSmoothing)
				{
					float fadeStrength = 0.5F;
					int fadeDistanceY = 10;
					
					if (y >= (maxHeight - fadeDistanceY))
					{
						selectionThreshold *= 1.0F - (fadeStrength * ((float) (y - (maxHeight - fadeDistanceY)) / (float) fadeDistanceY));
					}
				}
				
				double caveNoise = CaveManager.noise.getFractalNoise(actualX, (float) y / cavernScaleY, actualZ, 1, cavernFrequency, cavernAmplitude);
				
				if (caveNoise >= selectionThreshold)
				{
					//Don't immediately calculate both of these to save time.
					double floorNoise, ceilNoise;
					
					if (fastCavernYSmoothing)
					{
						replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, false);
						
						return;
					}
					else
					{
						//More blocks are likely to be above this point than to be below floor noise.
						ceilNoise = get2DNoiseFractal(CaveManager.noise2D2, actualX, actualZ, 1, cavernFrequency * 0.75, cavernAmplitude);
						
						if (y < cavernMaxHeight + (7.0 * ceilNoise - 7.0))
						{
							floorNoise = get2DNoiseFractal(CaveManager.noise2D1, actualX, actualZ, 1, cavernFrequency * 0.75, cavernAmplitude);
							
							if (y > cavernMinHeight + (3.0 * floorNoise + 3.0))
							{
								replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, false);
								
								return;
							}
						}
					}
				}
			}

			if (airOnly && primer.getBlockState(x, y, z).equals(Values.BLK_STONE))
			{			
				for (int i = 0; i < stoneLayers.length; i++)
				{
					StoneLayer layer = stoneLayers[i];
					
					double layerNoise = get2DNoiseFractal(layer.noise, actualX, actualZ, 1, 100.0, 1.0);
					
					if (y <= layerNoise * 5.0 + layer.getMaxHeight())
					{
						//Still have to return if there should be stone at this point.
						if (!layer.getState().equals(Values.BLK_STONE))
						{
							primer.setBlockState(x, y, z, layer.getState());
						}
						
						return;
					}
				}
			}	
		}
	}
	
	/**
	 * Experimental variant of {@link #addNoiseFeatures(int, int, ChunkPrimer)}.
	 * Supports shrinking cavern sizes near the borders of chunks that
	 * aren't valid candidates for spawning. Detection is not very good.
	 * Not yet worth the extra 1-2 seconds world gen time.
	 */
	protected void addCavernFadeOut(int chunkX, int chunkZ, ChunkPrimer primer)
	{
		boolean matchingBiomeNorth = true, matchingBiomeSouth = true, matchingBiomeEast = true, matchingBiomeWest = true,
				matchingNorthEast = true, matchingNorthWest = true, matchingSouthEast = true, matchingSouthWest = true;
				
		if (biomes.length > 0)
		{
			matchingBiomeNorth = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX, chunkZ - 1);
			matchingBiomeSouth = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX, chunkZ + 1);
			matchingBiomeEast = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX + 1, chunkZ);
			matchingBiomeWest = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX - 1, chunkZ);
			matchingNorthEast = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX + 1, chunkZ - 1);
			matchingNorthWest = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX - 1, chunkZ - 1);
			matchingSouthEast = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX + 1, chunkZ + 1);
			matchingSouthWest = CommonMethods.isAnyBiomeInChunk(biomes, world, chunkX - 1, chunkZ + 1);
		}
		
		int iterations = fillBlocks != null ? 2 : 1;
		
		boolean airOnly = true;
		
		for (int i = 0; i < iterations; i++)
		{
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					for (int y = cavernMaxHeight + 2; y >= 6; y--)
					{
						float actualX = x + (chunkX * 16);
						float actualZ = z + (chunkZ * 16);
						
						float fadeOut = 1.0F, fadeStrength = 0.5F;
						int fadeDistanceXZ = 15;
						
						/*
						 * For each coordinate closer to fadeDistance from the edge of the chunk, 
						 * increase the selection threshold until it hits 1.0.
						 */
						if (!matchingBiomeNorth || !matchingNorthEast || !matchingNorthWest)
						{
							if (z <= fadeDistanceXZ) fadeOut = 1.0F - (fadeStrength * ((float) (fadeDistanceXZ - z) / (float) fadeDistanceXZ));
						}
						else if (!matchingBiomeSouth || !matchingSouthEast || !matchingSouthWest)
						{
							if (z >= (16 - fadeDistanceXZ)) fadeOut = 1.0F - (fadeStrength * ((float) (z - (16 - fadeDistanceXZ)) / (float) fadeDistanceXZ));
						}
						if (!matchingBiomeEast || !matchingNorthEast || !matchingSouthEast)
						{
							if (x >= (16 - fadeDistanceXZ)) fadeOut *= fadeStrength * (fadeStrength * ((float) (x - (16 - fadeDistanceXZ)) / (float) fadeDistanceXZ));
						}
						else if (!matchingBiomeWest || !matchingNorthWest || !matchingSouthWest)
						{
							if (x <= fadeDistanceXZ) fadeOut *= fadeStrength * (fadeStrength * ((float) (fadeDistanceXZ - x) / (float) fadeDistanceXZ));
						}
						
						double floorNoise = get2DNoiseFractal(CaveManager.noise2D1, actualX, actualZ, 1, cavernFrequency * 0.75, cavernAmplitude),
					           ceilNoise = get2DNoiseFractal(CaveManager.noise2D2, actualX, actualZ, 1, cavernFrequency * 0.75, cavernAmplitude);
					
						if (y > (cavernMinHeight + (3.0 * floorNoise + 3.0)) && y < (cavernMaxHeight + (7.0 * ceilNoise - 7.0)))
						{
							double fractalNoise = CaveManager.noise.getFractalNoise(actualX, (float) y * 2.0, actualZ, 1, cavernFrequency, cavernAmplitude);
							
							if (fractalNoise * fadeOut >= cavernSelectionThreshold)
							{
								replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, false);
							}
						}
					}
				}
			}
			
			airOnly = false;
		}
	}
	
	/**
	 * To-Do: Give up and delete this?
	 */
	protected void addGiantCluster(long seed, int chunkX, int chunkZ, ChunkPrimer primer, int posY, int radius, int noise, IBlockState state)
	{
		SimplexNoiseGenerator3D localNoise = new SimplexNoiseGenerator3D(seed);
		
		int maxRange = radius + noise;
		int size = maxRange * 2;
		
		int heightMax = posY + maxRange, heightMin = posY - maxRange;		
		
		heightMax = heightMax > maxHeight ? maxHeight : heightMax;
		heightMin = heightMin < minHeight ? minHeight : heightMin;
		
		int storeX = chunkX, storeZ = chunkZ;

		ChunkCorrections corrections = null;
		
		for (int x = 0 - maxRange; x < maxRange; x++)
		{
			int actualX = chunkX + (x / 16);

			for (int z = 0 - maxRange; z < maxRange; z++)
			{
				int actualZ = chunkZ + (z / 16);
				
				//Avoid retrieving corrections every single iteration.
				if (actualX != storeX || actualZ != storeZ)
				{
					storeX = actualX;
					storeZ = actualZ;

					corrections = CorrectionStorage.getCorrectionsForChunk(world.provider.getDimension(), actualX, actualZ);
				}
				
				for (int y = heightMin; y < heightMax; y++)
				{
//					if (?)
					{
						int posX = x % 16, posZ = z % 16;
						
						if (posX < 0) posX = 15 + posX;
						
						if (posZ < 0) posZ = 15 + posZ;
						
						if (actualX == chunkX && actualZ == chunkZ)
						{
							if (canReplaceLessSpecific(primer.getBlockState(posX, y, posZ)))
							{
								primer.setBlockState(posX, y, posZ, state);
							}
						}
						else if (world.isChunkGeneratedAt(actualX, actualZ))
						{							
							BlockPos absolutePos = getBlockPosFromRelativeCoords(chunkX, chunkZ, x, y, z);
							
							if (canReplaceLessSpecific(world.getBlockState(absolutePos)))
							{
								world.setBlockState(absolutePos, state);
							}	
						}
						else corrections.addCorrection(posX, posY, posZ, state);
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
	
	private boolean testForWater(ChunkPrimer primer, double stretchXZ, double stretchY, int chunkX, int chunkZ, int x1, int x2, double posX, int y1, int y2, double posY, int z1, int z2, double posZ)
	{
		for (int x = x1; x < x2; x++)
		{
			double finalX = ((x + chunkX * 16) + 0.5D - posX) / stretchXZ;

			for (int z = z1; z < z2; z++)
			{
				double finalZ = ((z + chunkZ * 16) + 0.5D - posZ) / stretchXZ;

				if (((finalX * finalX) + (finalZ * finalZ)) < 1.0D)
				{
					for (int y = y2; y > y1; y--)
					{
						double finalY = ((y - 1) + 0.5D - posY) / stretchY;

						if ((finalY > -0.7D) && (((finalX * finalX) + (finalY * finalY) + (finalZ * finalZ)) < 1.0D))
						{
							if (isOceanBlock(primer, x, y, z, chunkX, chunkZ))
							{
								return true;
							}
						}
					}
				}
			}
		}
		
		return false;
	}
	
	protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ)
	{
		Block block = data.getBlockState(x, y, z).getBlock();
		
		return block.equals(Blocks.FLOWING_WATER) || block.equals(Blocks.WATER);
	}
	
	private boolean shouldPregenerateAir()
	{
		for (BlockFiller filler : fillBlocks)
		{
			if (filler.hasMatchers() && filler.hasDirections()) return true;
		}
		
		return false;
	}
	
	private boolean shouldTestForWater(int highestY)
	{
		for (BlockFiller filler : fillBlocks)
		{
			if (filler.getFillBlock().equals(Values.BLK_WATER) && highestY <= filler.getMaxHeight() + 1)
			{
				return false; //Ignore water that's supposed to exist.
			}
		}
		
		return true;
	}
	
	private int applyLimitXZ(int xz)
	{
		return xz < 0 ? 0 : xz > 16 ? 16 : xz;
	}
	
	private int applyLimitY(int y)
	{
		return y < 1 ? 1 : y > 248 ? 248 : y;
	}
	
	private float adjustTwist(float original, Random rand, float exponent, float factor, float randFactor)
	{
		original = (float) Math.pow(original, exponent);
		original *= factor;
		original += randFactor * (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat();
		
		return original;
	}
	
	private double adjustScale(double original, Random rand, float exponent, float factor, float randFactor)
	{
		original = (float) Math.pow(original, exponent);
		original *= factor;
		original += randFactor * (rand.nextFloat() - 0.5F);
		if (original < 0) original = 0;
		
		return original;
	}
	
	private void replaceSection(boolean airOnly, ChunkPrimer primer, double stretchXZ, double stretchY, int chunkX, int chunkZ, int x1, int x2, double posX, int y1, int y2, double posY, int z1, int z2, double posZ)
	{
		for (int x = x1; x < x2; x++)
		{
			double finalX = ((x + chunkX * 16) + 0.5 - posX) / stretchXZ;

			for (int z = z1; z < z2; z++)
			{
				double finalZ = ((z + chunkZ * 16) + 0.5 - posZ) / stretchXZ;

				if (((finalX * finalX) + (finalZ * finalZ)) < 1.0)
				{
					for (int y = y2; y > y1; y--)
					{
						double finalY = ((y - 1) + 0.5 - posY) / stretchY;

						if ((finalY > -0.7) && (((finalX * finalX) + (finalY * finalY) + (finalZ * finalZ)) < 1.0D))
						{
							if (isTopBlock(primer, x, y, z, chunkX, chunkZ))
							{
								replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, true);
							}

							else replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, false);
						}
					}
				}
			}
		}
	}
	
	private void replaceSectionFromMut(boolean airOnly, ChunkPrimer primer, double stretchXZ, double stretchY, int chunkX, int chunkZ, int x1, int x2, double posX, int y1, int y2, double posY, int z1, int z2, double posZ)
	{		
		for (int x = x1; x < x2; x++)
        {
            double finalX = ((double) (x + chunkX * 16) + 0.5 - posX) / stretchXZ;

            for (int z = z1; z < z2; z++)
            {
                double finalZ = ((double) (z + chunkZ * 16) + 0.5 - posZ) / stretchXZ;

                if (finalX * finalX + finalZ * finalZ < 1.0)
                {
                	for (int y = y2; y > y1; y--)
                    {
                		double finalY = ((double) (y - 1) + 0.5 - posY) / stretchY;
                        
                        if ((finalX * finalX + finalZ * finalZ) * (double) this.mut[y - 1] + finalY * finalY / 6.0 < 1.0)
                        {                        	
                        	if (isTopBlock(primer, x, y, z, chunkX, chunkZ))
                            {
                            	replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, true);
                            }

                            else replaceBlock(airOnly, primer, x, y, z, chunkX, chunkZ, false);
                        }
                    }
                }
            }
        }
	}
	
	/*
	 * From Forge docs: to help imitate vanilla generation.
	 */
	private boolean isExceptionBiome(Biome biome)
	{
		if (biome.equals(Biomes.BEACH) || biome.equals(Biomes.DESERT)) return true;
		
		return false;
	}
	
	/*
	 * Determine if the block at the specified location is the top block for the biome, we take into account
	 * Vanilla bugs to make sure that we generate the map the same way vanilla does.
	 */
	private boolean isTopBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ)
	{
		Biome biome = world.getBiome(new BlockPos(x + chunkX * 16, 0, z + chunkZ * 16));
		IBlockState state = data.getBlockState(x, y, z);

		return (isExceptionBiome(biome) ? state.getBlock() == Blocks.GRASS : state.getBlock() == biome.topBlock);
	}
	
	/**
	 * Digs out the current block, default implementation removes stone, filler, and top block
	 * Sets the block to lava if y is less then 10, and air other wise.
	 * If setting to air, it also checks to see if we've broken the surface and if so
	 * tries to make the floor the biome's top block
	 * 
	 * Modded by PersonTheCat to allow filling with water, or randomly from an array of filler
	 * blocks. Each filler block can have direction matchers and can either replace the
	 * current position or the location where the matching blockstate was found.
	 * 
	 * @param data          Block data array
	 * @param x             local X position
	 * @param y             local Y position
	 * @param z             local Z position
	 * @param chunkX        Chunk X position
	 * @param chunkZ        Chunk Y position
	 * @param foundTop      True if we've encountered the biome's top block. Ideally if we've broken the surface.
	 * 
	 * To-do: change airOnly to decorate / noAir. Decorate never places air.
	 */
	protected boolean replaceBlock(boolean airOnly, ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop)
	{
		Biome biome = world.getBiome(new BlockPos(x + (chunkX * 16), 0, z + (chunkZ * 16)));
		
		IBlockState state = data.getBlockState(x, y, z);
		IBlockState up = (IBlockState) MoreObjects.firstNonNull(data.getBlockState(x, y + 1, z), Values.BLK_AIR);
		IBlockState top = Blocks.GRASS.getDefaultState();
		IBlockState filler = Blocks.DIRT.getDefaultState();
				
		int yDown = y - 1;
		
		if (canReplaceBlock(state, up) || !airOnly || state.getBlock().equals(top.getBlock()) || state.getBlock().equals(filler.getBlock()))
		{
			if (yDown < lavaMaxHeight)
			{
				data.setBlockState(x, y, z, Values.BLK_LAVA);
				
				return true;
			}
			else if (!airOnly && fillBlocks != null)
			{
				for (BlockFiller replacement : fillBlocks)
				{					
					if (replacement.canGenerateAtHeight(y) && indRand.nextDouble() * 100 <= replacement.getChance())
					{
						boolean replaceAtMatch = replacement.getPreference().equals(Preference.REPLACE_MATCH),
								matchFound = false,
								breakFromLoop = false;

						if (replacement.hasMatchers())
						{
							BlockPos blockUp = new BlockPos(x, y + 1, z),
									 blockDown = new BlockPos(x, yDown, z),
									 blockNorth = new BlockPos(x, y, z - 1),
									 blockSouth = new BlockPos(x, y, z + 1),
									 blockEast = new BlockPos(x + 1, y, z),
									 blockWest = new BlockPos(x - 1, y, z);

							for (Direction direction : replacement.getDirections())
							{								
								switch (direction)
								{
									case UP:
										
										for (IBlockState matcher : replacement.getMatchers())
										{
											if (data.getBlockState(blockUp.getX(), blockUp.getY(), blockUp.getZ()).equals(matcher))
											{
												if (replaceAtMatch)
												{
													data.setBlockState(blockUp.getX(), blockUp.getY(), blockUp.getZ(), replacement.getFillBlock());
													
													break;
												}
												
												matchFound = true;
											}
										}

										break;
										
									case SIDE:
										
										for (IBlockState matcher : replacement.getMatchers())
										{
											if (breakFromLoop) break;
											
											for (BlockPos pos : new BlockPos[] {blockNorth, blockSouth, blockEast, blockWest})
											{
												if (replaceAtMatch)
												{
													if (placeOrStoreBlockState(data, chunkX, chunkZ, pos, replacement.getFillBlock(), matcher))
													{
														breakFromLoop = true;
													}
													
												}
												else if (areCoordsInChunk(pos.getX(), pos.getZ()))
												{
													if (data.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(matcher))
													{
														matchFound = true;
														breakFromLoop = true;
														break;
													}
												}
											}
										}
										
										break;
										
									case DOWN:
										
										for (IBlockState matcher : replacement.getMatchers())
										{
											if (data.getBlockState(blockDown.getX(), blockDown.getY(), blockDown.getZ()).equals(matcher))
											{
												if (replaceAtMatch)
												{
													data.setBlockState(blockDown.getX(), blockDown.getY(), blockDown.getZ(), replacement.getFillBlock());
													
													break;
												}
												
												matchFound = true;
											}
										}
										
										break;
										
									case ALL:
										
										for (IBlockState matcher : replacement.getMatchers())
										{
											if (breakFromLoop) break;
											
											for (BlockPos pos : new BlockPos[] {blockUp, blockDown, blockNorth, blockSouth, blockEast, blockWest})
											{	
												if (replaceAtMatch)
												{
													if (placeOrStoreBlockState(data, chunkX, chunkZ, pos, replacement.getFillBlock(), matcher))
													{
														breakFromLoop = true;
													}
												}
												else if (areCoordsInChunk(pos.getX(), pos.getZ()))
												{
													if (data.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(matcher))
													{
														matchFound = true;
														breakFromLoop = true;
														break;
													}
												}
											}
										}
										
										break;
										
									default:
										
										throw new AssertionError("Error: Invalid direction detected. Unable to phase through fourth dimension.");
								}
							}
						}
						
						else matchFound = true;
						
						if (matchFound)
						{
							data.setBlockState(x, y, z, replacement.getFillBlock());
							
							return true;
						}
					}
				}
			}
			
			data.setBlockState(x, y, z, Values.BLK_AIR);

			if (foundTop && data.getBlockState(x, yDown, z).getBlock().equals(filler.getBlock()))
			{
				data.setBlockState(x, yDown, z, top.getBlock().getDefaultState());
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Corrects the chunk coordinates to determine whether the chunk has been generated.
	 * 
	 * Prevents access violations. -Still needed?
	 */
	private boolean isChunkLoaded(int originalChunkX, int originalChunkZ, int relativeX, int relativeZ)
	{
		int chunkX = originalChunkX, chunkZ = originalChunkZ;
		
		chunkX = relativeX < 0 ? chunkX - 1 : relativeX > 15 ? chunkX + 1 : chunkX;
		chunkZ = relativeZ < 0 ? chunkZ - 1 : relativeZ > 15 ? chunkZ + 1 : chunkZ;
		
		return world.isChunkGeneratedAt(chunkX, chunkZ);
	}
	
	/**
	 * Unfinished. Returns true if a match is found in the current chunk.
	 */
	private boolean placeOrStoreBlockState(ChunkPrimer originalPrimer, int chunkX, int chunkZ, BlockPos pos, IBlockState state, IBlockState matcher)
	{
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();		
		
		//Coords in chunk? Test and place.
		if (areCoordsInChunk(x, z))
		{
			if (matcher.equals(originalPrimer.getBlockState(x, y, z)))
			{
				originalPrimer.setBlockState(x, y, z, state);
				
				return true;
			}
		}
		else
		{			
			ChunkCorrections corrections = null;
			
			//Adjust coordinates.		
			if (x < 0)
			{
				corrections = xMinusOne;
				chunkX -= 1;
				x = 15;
			}
			if (x > 15)
			{
				corrections = xPlusOne;
				chunkX += 1;
			x = 0;
			}
			if (z < 0)
			{
				corrections = zMinusOne;
				chunkZ -= 1;
				z = 15;
			}
			if (z > 15)
			{
				corrections = zPlusOne;
				chunkZ += 1;
				z = 0;
			}

			//Not in chunk but already generated? Test and place.
			if (world.isChunkGeneratedAt(chunkX, chunkZ))
			{
				Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
				BlockPos relativePos = new BlockPos(x, y, z);				
				
				if (chunk.getBlockState(relativePos).equals(matcher))
				{
					chunk.setBlockState(relativePos, state);
				}
			}
			else //Not in chunk, not already generated? Store for later.
			{
				corrections.addCorrection(x, y, z, state);
			}
		}
		
		return false;
	}
	
	private BlockPos getBlockPosFromRelativeCoords(int chunkX, int chunkZ, int x, int y, int z)
	{
		int posX = (chunkX * 16) + x,
		    posZ = (chunkZ * 16) + z;

		return new BlockPos(posX, y, posZ);
	}
	
	private BlockPos getRelativeCoordsFromBlockPos(int chunkX, int chunkZ, BlockPos pos)
	{
		int x = pos.getX() - (chunkX * 16);
		int z = pos.getZ() - (chunkZ * 16);
		
		return new BlockPos(x, pos.getY(), z);
	}
	
	private boolean areCoordsInChunk(int x, int z)
	{
		return x > -1 && x < 16 && z > -1 && z < 16;
	}
	
	private static boolean canReplaceBlock(IBlockState state, IBlockState blockAbove)
	{
		if (canReplaceLessSpecific(state)) return true;
		
		return (state.getBlock().equals(Blocks.SAND) || state.getBlock().equals(Blocks.GRAVEL)) &&
				!blockAbove.getMaterial().equals(Material.WATER);
	}
	
	/*
	 * Not very efficient.
	 */
    protected static boolean canReplaceLessSpecific(IBlockState state)
    {
    	if (state.equals(Values.BLK_STONE)) return true; //First most common block
    	
    	if (state.equals(Values.BLK_AIR)) return false; //Second most common (from overlapping generation)
    	
    	//To-do: test for collapsable material when water is above
    	
    	for (Block block : Values.replaceableBlocks) //Others
    	{
    		if (state.getBlock().equals(block)) return true;
    	}
    	
    	for (IBlockState block2 : BlockFiller.getAllFillBlocks())
    	{
    		if (state.equals(block2)) return true;
    	}
    	
    	for (IBlockState block3 : StoneLayer.getAllLayers())
    	{
    		if (state.equals(block3)) return true;
    	}
    	
    	return false;
    }
}