package com.personthecat.cavegenerator.world.feature;

import java.util.Random;

import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.util.Values;
import com.personthecat.cavegenerator.world.BlockFiller;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.BlockFiller.Preference;
import com.personthecat.cavegenerator.world.feature.LargeStalactite.Type;

import static com.personthecat.cavegenerator.Main.logger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fml.common.IWorldGenerator;
import scala.actors.threadpool.Arrays;

public class CaveFeatureGenerator implements IWorldGenerator
{
	//To-do: Pre-calculate this.
	private static NoiseGeneratorSimplex noise;
	private int[] waterlessHeightMap = new int[256];
	
	@Override
	public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider)
	{
		int dimension = world.provider.getDimension();
		
		if (CaveInit.isAnyGeneratorEnabledForDimension(dimension))
		{
			generateWaterlessHeightMap(chunkX, chunkZ, world);
			
			for (CaveGenerator generator : CaveInit.GENERATORS.values())
			{
				if (generator.enabledGlobally && generator.canGenerateInDimension(dimension))
				{
					if (generator.cavernsEnabled) //I also hate this.
					{
						preventCaveins(chunkX, chunkZ, world, generator.cavernMaxHeight);
					}
					
					//decorateSurfaces(generator, rand, chunkX, chunkZ, world);
					generatePillars(generator, rand, chunkX, chunkZ, world);
					generateStalactitesAndStalagmites(generator, rand, chunkX, chunkZ, world);
					generateStructures(generator, rand, chunkX, chunkZ, world);
					//fillAirBlocks(generator, rand, chunkX, chunkZ, world);
				}
			}
			//printHeightMap(chunkX, chunkZ, world);
		}
	}
	
	private void printHeightMap(int chunkX, int chunkZ, World world)
	{		
		int[][] heights = new int[16][16];
		
		logger.info("Heights for (" + chunkX + "," + chunkZ + "):");
		
		for (int x = 0; x < 15; x++)
		{
			for (int z = 0; z < 15; z++)
			{
				heights[x][z] = getWaterlessMaxHeight(x, z);
			}
			
			logger.info(Arrays.toString(heights[x]));
		}
	}
	
	private void generateWaterlessHeightMap(int chunkX, int chunkZ, World world)
	{
		Chunk currentChunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
		int approximateMaxHeight = currentChunk.getTopFilledSegment();
		
		for (int x = 0; x < 16; x++)
		{
			for (int z = 0; z < 16; z++)
			{				
				boolean previouslyAir = !currentChunk.getBlockState(x, approximateMaxHeight, z).isOpaqueCube();
				
				if (!previouslyAir)
				{
					setWaterlessMaxHeight(x, z, approximateMaxHeight);
					
					continue;
				}
				
				for (int y = approximateMaxHeight - 1; y >= 0; y--)
				{
					boolean currentlyAir = !currentChunk.getBlockState(x, y, z).isOpaqueCube();
					
					if (previouslyAir && !currentlyAir)
					{
						setWaterlessMaxHeight(x, z, y);
						
						break;
					}
				}
			}
		}
	}
	
	private void setWaterlessMaxHeight(int x, int z, int height)
	{
		this.waterlessHeightMap[z << 4 | x] = height;
	}
	
	private int getWaterlessMaxHeight(int x, int z)
	{
		return waterlessHeightMap[Math.abs((z % 16) << 4 | (x % 16))];
	}
	
	private int getActualMaxHeight(int x, int z, int previousMaxHeight)
	{
		int waterless = getWaterlessMaxHeight(x, z) - 5;
		
		return waterless < previousMaxHeight ? waterless : previousMaxHeight;
	}
	
	private void preventCaveins(int chunkX, int chunkZ, World world, int cavernMaxHeight)
	{
		for (int x = 0; x < 16; x++)
		{
			for (int z = 0; z < 16; z++)
			{
				int waterlessHeight = getWaterlessMaxHeight(x, z);
				
				if (waterlessHeight <= cavernMaxHeight + 1)
				{
					int actualX = chunkX * 16 + x, actualZ = chunkZ * 16 + z;
					
					BlockPos posUp = new BlockPos(actualX, waterlessHeight + 1, actualZ);
					BlockPos currentPos = new BlockPos(actualX, waterlessHeight, actualZ);
					BlockPos posDown = new BlockPos(actualX, waterlessHeight - 1, actualZ);
					
					IBlockState blockUp = world.getBlockState(posUp);
					IBlockState currentBlock = world.getBlockState(currentPos);
					
					if (blockUp.equals(Values.BLK_WATER))
					{
						while(world.getBlockState(posDown).getBlock() instanceof BlockFalling)
						{
							posDown = posDown.down();
						}
						if (currentBlock.getBlock() instanceof BlockFalling)
						{
							world.setBlockState(posDown, Values.BLK_STONE, 16);
						}
						else if (currentBlock.getMaterial().equals(Material.AIR))
						{
							world.setBlockState(posDown, Values.BLK_STONE, 16);
							world.setBlockState(currentPos, Blocks.GRAVEL.getDefaultState(), 16);
						}
					}
				}
			}
		}
	}
	
	@Deprecated
	private void decorateSurfaces(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		if (cave.fillBlocksUp.length > 0 || cave.fillBlocksDown.length > 0)
		{
			decorateSurfacesY(cave, rand, chunkX, chunkZ, world);
		}
		if (cave.fillBlocksSide.length > 0)
		{
			decorateSurfacesX(cave, rand, chunkX, chunkZ, world);
			decorateSurfacesZ(cave, rand, chunkX, chunkZ, world);
		}
	}
	
	@Deprecated
	private void fillAirBlocks(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		if (cave.fillBlocksNormal.length > 0)
		{
			int startX = chunkX * 16 + 8, startZ = chunkZ * 16 + 8;
			
			for (int x = startX; x < startX + 16; x++)
			{
				for (int z = startZ; z < startZ + 16; z++)
				{
					for (int y = cave.globalMinHeight; y <= cave.globalMaxHeight; y++)
					{
						BlockPos currentPos = new BlockPos(x, y, z);
						
						if (world.getBlockState(currentPos).equals(Values.BLK_AIR))
						{
							for (BlockFiller replacement : cave.fillBlocksNormal)
							{
								if (!replacement.isCaveSpecific() && replacement.canGenerateAtHeight(y))
								{
									if (rand.nextDouble() * 100 <= replacement.getChance())
									{
										if (replacement.testNoise(x, y, z))
										{
											world.setBlockState(currentPos, replacement.getFillBlock());
											
											break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * This is the disappointment of a lifetime. Turns out, I was right when I
	 * first wrote this mod; it's actually faster to generate caves twice and
	 * decorate surfaces the second time than it is to run this generator
	 * once per chunk after all caves have been placed. Much, much faster.
	 * Even just replacing all air blocks in a given space causes enormous
	 * tick lag for several seconds after the world has been generated.
	 * 
	 * Not sure how to proceed.
	 */
	@Deprecated
	private void decorateSurfacesY(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		int startX = chunkX * 16 + 8, startZ = chunkZ * 16 + 8;
		
		for (int x = startX; x < startX + 16; x++)
		{
			for (int z = startZ; z < startZ + 16; z++)
			{
				int startingY = cave.globalMinHeight;
				
				BlockPos previousPos = new BlockPos(x, startingY, z);
				
				boolean previouslyAir = !world.getBlockState(previousPos).isOpaqueCube();
				
				for (int y = startingY + 1; y <= cave.globalMaxHeight; y++)
				{
					BlockPos currentPos = new BlockPos(x, y, z);
					
					boolean currentlyAir = !world.getBlockState(currentPos).isOpaqueCube();
					
					if (!previouslyAir && currentlyAir) //Bottom surface.
					{
						for (BlockFiller replacement : cave.fillBlocksDown)
						{
							if (!replacement.isCaveSpecific() && replacement.canGenerateAtHeight(y))
							{								
								if (rand.nextDouble() * 100 <= replacement.getChance())
								{	
									if (replacement.testNoise(x, y, z))
									{
										if (replacement.getPreference().equals(Preference.REPLACE_ORIGINAL))
										{
											world.setBlockState(currentPos, replacement.getFillBlock(), 2);
										}
										else world.setBlockState(previousPos, replacement.getFillBlock(), 2);
										
										break;
									}
								}
							}
						}
					}
					else if (previouslyAir && !currentlyAir) //Upper surface.
					{
						for (BlockFiller replacement : cave.fillBlocksUp)
						{
							if (!replacement.isCaveSpecific() && replacement.canGenerateAtHeight(y))
							{
								if (rand.nextDouble() * 100 <= replacement.getChance())
								{
									if (replacement.testNoise(x, y, z))
									{
										if (replacement.getPreference().equals(Preference.REPLACE_ORIGINAL))
										{
											world.setBlockState(previousPos, replacement.getFillBlock(), 2);
										}
										else world.setBlockState(currentPos, replacement.getFillBlock(), 2);
										
										break;
									}
								}
							}
						}
					}
					
					previousPos = currentPos;
					previouslyAir = currentlyAir;
				}
			}
		}
	}

	private void decorateSurfacesX(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		int startX = chunkX * 16 + 8, startZ = chunkZ * 16 + 8;
	}
	
	private void decorateSurfacesZ(CaveGenerator cave, Random rand, int chunkX, int chunkZ, World world)
	{
		int startX = chunkX * 16 + 8, startZ = chunkZ * 16 + 8;
	}
	
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
						if (structureFromDirection(direction, structureInfo, rand, chunkX, chunkZ, world))
						{
							break;
						}
					}
				}
			}
		}
	}
	
	private boolean structureFromDirection(Direction direction, StructureSpawnInfo structureInfo, Random rand, int chunkX, int chunkZ, World world)
	{
		Template structure = structureInfo.getStructure(world);
		
		BlockPos startingPos = getStartingStructurePos(rand, structure.getSize(), chunkX, chunkZ, structureInfo.getMinHeight(), structureInfo.getMaxHeight());
		
		BlockPos match = getMatchOnYAxis(structureInfo, rand, direction, world, startingPos);
		
		if (match != null && matchSources(structureInfo.getMatchers(), world, match))
		{
			BlockPos offset = structureInfo.getOffset();
			
			match = match.add(offset.getX(), offset.getY(), offset.getZ());
			
			if (matchAirBlocks(structureInfo, world, match))
			{
				if (structureInfo.shouldRotateRandomly())
				{
					Rotation randomRotation = Rotation.values()[rand.nextInt(3)];
					
					structureInfo.getSettings().setRotation(randomRotation);
				}
				
				if (structureInfo.shouldDebugSpawns())
				{
					logger.info("Spawning " + structureInfo.getStructureName() + " at " + match);
				}
				
				StructureSpawner.SpawnStructure(structure, structureInfo.getSettings(), world, centerBySize(match, structure.getSize()));
				
				return true;
			}
		}
		
		return false;
	}
	
	private boolean matchSources(IBlockState[] matchers, World world, BlockPos match)
	{
		if (matchers.length > 0)
		{
			IBlockState previousBlock = world.getBlockState(match);
			
			for (IBlockState matcher : matchers)
			{
				if (matcher.equals(previousBlock))
				{
					return true;
				}
			}
			
			return false;
		}
		
		return true;
	}
	
	private boolean matchAirBlocks(StructureSpawnInfo structureInfo, World world, BlockPos match)
	{
		for (BlockPos airMatcher : structureInfo.getAdditionalAirMatchers())
		{
			if (!world.getBlockState(match.add(airMatcher.getX(), airMatcher.getY(), airMatcher.getZ())).isOpaqueCube())
			{
				return false;
			}
		}
		for (BlockPos solidMatcher : structureInfo.getAdditionalSolidMatchers())
		{
			if (world.getBlockState(match.add(solidMatcher.getX(), solidMatcher.getY(), solidMatcher.getZ())).isOpaqueCube())
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Determines whether structures can spawn anywhere in a chunk,
	 * Or just in the center. Needs to be adjusted for structures
	 * larger than ~14-16 blocks.
	 * 
	 * Does not include a y coordinate. Avoid repeated calculations.
	 */
	private BlockPos getStartingStructurePos(Random rand, BlockPos structureSize, int chunkX, int chunkZ, int minHeight, int maxHeight)
	{
		int x = Math.abs(structureSize.getX());
		int z = Math.abs(structureSize.getZ());
		
		//Leaves enough room for half of the structure at either end, per dimension.
		if (x < 16)
		{
			if (x % 2 == 0) x += 1; //Fix even numbers breaking bounds.
			
			x = rand.nextInt(16 - x) + (x / 2) + (chunkX * 16) + 8;
		}
		else x = 8 + (chunkX * 16) + 8;
		
		if (z < 16)
		{
			if (z % 2 == 0) z += 1; //Maybe just always do this?
			
			z = rand.nextInt(16 - z) + (z / 2) + (chunkZ * 16) + 8;
		}
		else z = 8 + (chunkZ * 16) + 8;
		
		//if (# > 15) { This feature isn't ready yet. }
		
		return new BlockPos(x, 0, z);
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

						int actualMaxHeight = getActualMaxHeight(x, z, gen.getMaxHeight());
						
						for (int y = gen.getMinHeight(); y < actualMaxHeight; y++)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								BlockPos match = new BlockPos(x, y - 1, z);
								
								if (matchSources(gen.getMatchers(), world, match))
								{
									gen.generate(world, rand, match);
								}
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

						int actualMaxHeight = getActualMaxHeight(x, z, gen.getMaxHeight());
						
						for (int y = gen.getMinHeight(); y < actualMaxHeight; y++)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								BlockPos match = new BlockPos(x, y - 1, z);
								
								if (matchSources(gen.getMatchers(), world, match))
								{
									gen.generate(world, rand, match);	
								}
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

						int actualMaxHeight = getActualMaxHeight(x, z, gen.getMaxHeight());
						
						for (int y = actualMaxHeight; y > gen.getMinHeight(); y--)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								BlockPos match = new BlockPos(x, y + 1, z);
								
								if (matchSources(gen.getMatchers(), world, match))
								{
									gen.generate(world, rand, match);
								}
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

						int actualMaxHeight = getActualMaxHeight(x, z, gen.getMaxHeight());
						
						for (int y = actualMaxHeight; y > gen.getMinHeight(); y--)
						{
							boolean currentlyAir = !world.getBlockState(new BlockPos(x, y, z)).isFullCube();

							if ((previouslyAir && !currentlyAir) && (rand.nextDouble() * 100) <= probability)
							{
								BlockPos match = new BlockPos(x, y + 1, z);
								
								if (matchSources(gen.getMatchers(), world, match))
								{
									gen.generate(world, rand, match);
								}
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
	
	/*
	 * @param startingPos should start at y = 0;
	 */
	private BlockPos getMatchOnYAxis(StructureSpawnInfo info, Random rand, Direction direction, World world, BlockPos startingPos)
	{
		int actualMaxHeight = getActualMaxHeight(startingPos.getX(), startingPos.getZ(), info.getMaxHeight());
		
		startingPos = startingPos.up(rand.nextInt(actualMaxHeight));
		
		BlockPos match = getMatchFromAboveHalf(direction, world, startingPos, info.getMinHeight(), info.getMatchers());
		
		if (match != null) return match;

		BlockPos topPos = new BlockPos(startingPos.getX(), actualMaxHeight, startingPos.getZ());
		
		return getMatchFromAboveHalf(direction, world, topPos, startingPos.getY(), info.getMatchers());
	}
	
	private BlockPos getMatchFromAboveHalf(Direction direction, World world, BlockPos startingPos, int minHeight, IBlockState[] matchers)
	{
		BlockPos pos = startingPos;
		IBlockState previousBlock = world.getBlockState(pos);
		boolean previouslyAir = !previousBlock.isOpaqueCube();		
		
		for (pos = pos.down(); pos.getY() >= minHeight; pos = pos.down())
		{
			IBlockState currentBlock = world.getBlockState(pos);
			boolean currentlyAir = !currentBlock.isOpaqueCube();
			
			if (direction.equals(Direction.UP))
			{
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
			else if (previouslyAir && !currentlyAir)
			{
				for (IBlockState matcher : matchers)
				{
					if (currentBlock.equals(matcher))
					{
						return pos;
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