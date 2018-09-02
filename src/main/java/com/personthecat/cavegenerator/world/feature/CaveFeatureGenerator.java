package com.personthecat.cavegenerator.world.feature;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.feature.LargeStalactite.Type;

import static com.personthecat.cavegenerator.Main.logger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraft.world.gen.structure.template.Template;
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
				generateStructures(generator, rand, chunkX, chunkZ, world);
				generatePillars(generator, rand, chunkX, chunkZ, world);
				generateStalactitesAndStalagmites(generator, rand, chunkX, chunkZ, world);
			}
		}
	}
	
	/**
	 * Extremely WIP. Barely works. Should ideally be moved into a MapGenBase.
	 */
	private void generateStructures(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		for (StructureSpawnInfo structureInfo : cave.structures)
		{
			for (int i = 0; i < structureInfo.getFrequency(); i++)
			{
				if (rand.nextDouble() * 100 <= structureInfo.getChance())
				{
					for (Direction direction : structureInfo.getDirections())
					{
						Template structure = structureInfo.getStructure(world);
						
						BlockPos startingPos = getStartingStructurePos(rand, structure.getSize(), chunkX, chunkZ, structureInfo.getMinHeight(), structureInfo.getMaxHeight());
						
						BlockPos match = null;
						
						if (direction.equals(Direction.UP))
						{
							match = getMatchFromAbove(structureInfo, world, startingPos);
						}
						else if (direction.equals(Direction.DOWN))
						{
							match = getMatchFromBelow(structureInfo, world, startingPos);
						}
						
						if (match != null)
						{
							BlockPos offset = structureInfo.getOffset();
							
							match = match.add(offset.getX(), offset.getY(), offset.getZ());
							
							boolean shouldContinue = false;
							
							for (BlockPos airMatcher : structureInfo.getAdditionalAirMatchers())
							{								
								if (!world.getBlockState(match.add(airMatcher.getX(), airMatcher.getY(), airMatcher.getZ())).isOpaqueCube())
								{
									shouldContinue = true;
									
									break;
								}
							}
							
							if (shouldContinue) continue;
							
							if (structureInfo.shouldDebugSpawns())
							{
								logger.info("Spawning " + structureInfo.getStructureName() + " at " + match);
							}
							
							StructureSpawner.SpawnStructure(structure, structureInfo.getSettings(), world, match);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Determines whether structures can spawn anywhere in a chunk,
	 * Or just in the center. Needs to be adjusted for structures
	 * larger than ~14-16 blocks.
	 */
	private BlockPos getStartingStructurePos(Random rand, BlockPos structureSize, int chunkX, int chunkZ, int minHeight, int maxHeight)
	{
		int increment = maxHeight - minHeight;
		if (increment < 1) increment = 1;
		
		int y = rand.nextInt(increment) + minHeight;
		
		int x = Math.abs(structureSize.getX());
		int z = Math.abs(structureSize.getZ());
		
		//Leaves enough room for half of the structure at either end, per dimension.
		if (x < 16)
		{
			if (x % 2 == 0) x += 1; //Fix even numbers breaking bounds.
			
			x = rand.nextInt(16 - x) + (x / 2) + (chunkX * 16);
		}
		else x = 8 + (chunkX * 16);
		
		if (z < 16)
		{
			if (z % 2 == 0) z += 1; //Maybe just always do this?
			
			z = rand.nextInt(16 - z) + (z / 2) + (chunkZ * 16);
		}
		else z = 8 + (chunkZ * 16);
		
		//if (# > 15) { This feature isn't ready yet. }
		
		return centerBySize(new BlockPos(x, y, z), structureSize);
	}
	
	//Moves each dimension by half of @param size in the opposite direction. 
	private BlockPos centerBySize(BlockPos toCenter, BlockPos size)
	{
		int xOffset = (size.getX() / 2) * -1;
		int zOffset = (size.getZ() / 2) * -1;
		
		return toCenter.add(xOffset, 0, zOffset);
	}
	
	private void generateStalactitesAndStalagmites(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		for (LargeStalactite stalactite : cave.stalactites)
		{
			if (stalactite.shouldSpawnInPatches()) noise = new NoiseGeneratorSimplex(new Random(rand.nextLong()));
			
			double probability = stalactite.getChance();
			
			if (stalactite.getType().equals(Type.STALACTITE))
			{
				if (probability >= 65) generateStalactites(cave, stalactite, rand, chunkX, chunkZ, world);
				
				else generateStalactitesByQuadrant(cave, stalactite, rand, chunkX, chunkZ, world);
			}
			else
			{
				if (probability >= 65) generateStalagmites(cave, stalactite, rand, chunkX, chunkZ, world);
				
				else generateStalagmitesByQuadrant(cave, stalactite, rand, chunkX, chunkZ, world);          
			}
		}
	}
	
	private void generatePillars(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{		
		for (GiantPillar pillar : cave.pillars)
		{
			for (int i = 0; i < rand.nextInt(pillar.getFrequency() + 1); i++)
			{
				int minHeight = pillar.getMinHeight(), maxHeight = pillar.getMaxHeight();
				
				//Avoid pillars spawning directly next to each other.
				int x = ((rand.nextInt(6) * 2) + 2) + (chunkX * 16); //Between 2 and 14.
				int z = ((rand.nextInt(6) * 2) + 1) + (chunkZ * 16); //Between 1 and 13.
				
				int y = rand.nextInt(maxHeight - minHeight) + minHeight;
				
				BlockPos previous = new BlockPos(x, y, z);
				boolean previouslyAir = !world.getBlockState(previous).isOpaqueCube();
				
				for (int h = y; h > minHeight; h--)
				{
					BlockPos current = new BlockPos(x, h, z);
					boolean currentlyAir = !world.getBlockState(current).isOpaqueCube();
					
					if (!previouslyAir && currentlyAir) //Found a cave from the top.
					{					
						pillar.generate(world, rand, previous);
						
						break;
					}
					
					previous = current;
					previouslyAir = currentlyAir;
				}
			}
		}
	}
	
	private void generateStalactites(CaveGenerator cave, LargeStalactite gen, Random rand, int chunkX, int chunkZ, World world)
	{
		if (cave.canGenerateInBiome(world.getBiome(new BlockPos(8, 0, 8))))
		{
			double probability = gen.getChance();
			
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					if (!gen.shouldSpawnInPatches() || get2DNoiseFractal(noise, x, z, 1, gen.getPatchSpacing(), 1.0) > gen.getPatchThreshold())
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
			if (!gen.shouldSpawnInPatches() || get2DNoiseFractal(noise, posX + 4, posZ + 4, 1, gen.getPatchSpacing(), 1.0) > gen.getPatchThreshold())
			{
				double probability = gen.getChance();
				
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
			double probability = gen.getChance();
			
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					if (!gen.shouldSpawnInPatches() || get2DNoiseFractal(noise, x, z, 1, gen.getPatchSpacing(), 1.0) > gen.getPatchThreshold())
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
			if (!gen.shouldSpawnInPatches() || get2DNoiseFractal(noise, posX + 4, posZ + 4, 1, gen.getPatchSpacing(), 1.0) > gen.getPatchThreshold())
			{
				double probability = gen.getChance();
				
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
	
	/**
	 * Each of these methods should start from the middle, 
	 * work to one side, and then restart from the other.
	 */
	
	private BlockPos getMatchFromBelow(StructureSpawnInfo info, World world, BlockPos startingPos)
	{
		BlockPos match = getMatchFromBelowHalf(world, startingPos, info.getMaxHeight(), info.getMatchers());
		
		if (match != null) return match;
		
		BlockPos bottomPos = new BlockPos(startingPos.getX(), info.getMinHeight(), startingPos.getZ());
		
		return getMatchFromBelowHalf(world, bottomPos, startingPos.getY(), info.getMatchers());
	}
	
	private BlockPos getMatchFromBelowHalf(World world, BlockPos startingPos, int maxHeight, IBlockState[] matchers)
	{
		BlockPos pos = startingPos;
		IBlockState previousBlock = world.getBlockState(pos);
		boolean previouslyAir = !previousBlock.isOpaqueCube();
		
		for (pos = pos.up(); pos.getY() <= maxHeight; pos = pos.up())
		{
			IBlockState currentBlock = world.getBlockState(pos);
			boolean currentlyAir = !currentBlock.isOpaqueCube();
			
			if (!previouslyAir && currentlyAir)
			{
				for (IBlockState matcher : matchers)
				{
					if (previousBlock.equals(matcher))
					{
						return pos.down();
					}
				}
			}
			
			previousBlock = currentBlock;
			previouslyAir = currentlyAir;
		}
		
		return null;
	}
	
	private BlockPos getMatchFromAbove(StructureSpawnInfo info, World world, BlockPos startingPos)
	{
		BlockPos match = getMatchFromAboveHalf(world, startingPos, info.getMinHeight(), info.getMatchers());
		
		if (match != null) return match;
		
		BlockPos topPos = new BlockPos(startingPos.getX(), info.getMaxHeight(), startingPos.getZ());
		
		return getMatchFromAboveHalf(world, topPos, startingPos.getY(), info.getMatchers());
	}
	
	private BlockPos getMatchFromAboveHalf(World world, BlockPos startingPos, int minHeight, IBlockState[] matchers)
	{
		BlockPos pos = startingPos;
		IBlockState previousBlock = world.getBlockState(pos);
		boolean previouslyAir = !previousBlock.isOpaqueCube();		
		
		for (pos = pos.down(); pos.getY() >= minHeight; pos = pos.down())
		{
			IBlockState currentBlock = world.getBlockState(pos);
			boolean currentlyAir = !currentBlock.isOpaqueCube();
			
			if (!previouslyAir && currentlyAir)
			{
				for (IBlockState matcher : matchers)
				{
					if (previousBlock.equals(matcher))
					{
						return pos.up();
					}
				}
			}
		}
		
		return null;
	}
	
	private BlockPos getMatchFromNorth(StructureSpawnInfo info, World world, BlockPos startingPos)
	{
		return null;
	}
	
	private BlockPos getMatchFromSouth(StructureSpawnInfo info, World world, BlockPos startingPos)
	{
		return null;
	}
	
	private BlockPos getMatchFromEast(StructureSpawnInfo info, World world, BlockPos startingPos)
	{
		return null;
	}
	
	private BlockPos getMatchFromWest(StructureSpawnInfo info, World world, BlockPos startingPos)
	{
		return null;
	}
}