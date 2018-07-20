package com.personthecat.cavegenerator.world;

import java.util.Random;

import com.google.common.base.MoreObjects;
import com.personthecat.cavegenerator.BlockFiller;
import com.personthecat.cavegenerator.CaveInit;
import com.personthecat.cavegenerator.BlockFiller.Direction;
import com.personthecat.cavegenerator.BlockFiller.Preference;
import com.personthecat.cavegenerator.util.Values;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenCaves;
import scala.actors.threadpool.Arrays;

public class CaveGenerator
{
	public Biome[] biomes = new Biome[0];
	
	public boolean 
	
		noiseYReduction = true; //Vanilla function
	
	public Extension
	
		extBeginning = Extension.NONE,
		extRand = Extension.VANILLA_BRANCHES,
		extEnd = Extension.NONE;

	public float 
	
		//Most unchanging values
		roomScale = 6.0F,
		roomScaleY = 0.5F,
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
	
		//Starting values
		startingTwistXZ = 0.0F,
		startingTwistY = 0.0F,
		startingScale = 0.0F,
		startingScaleRandFactor = 1.0F,
		startingScaleY = 1.0F,
		startingSlopeXZ = 0.0F,
		startingSlopeXZRandFactor = 1.0F,
		startingSlopeY = 0.0F,
		startingSlopeYRandFactor = 0.25F,
		
		//Probabilities
		generatorSelectionChance = 100.0F;

	public BlockFiller[] fillBlocks = new BlockFiller[0];
	
	public int
		
		//Most unchanging values
		minHeight = 8,
		maxHeight = 128,
		lavaMaxHeight = 10,
		
		//Starting values
		startingDistance = 0,
		
		//Probabilities (1 / #)
		spawnInSystemInverseChance = 4;
	
	public String
	
		extBeginningPreset,
		extRandPreset,
		extEndPreset;
	
	/**
	 * So that objects can be initialized during init. May remove.
	 */
	private Random indRand()
	{
		return CaveManager.instance.indRand();
	}
	
	private int range()
	{
		return CaveManager.instance.range();
	}
	
	private Random rand()
	{
		return CaveManager.instance.rand();
	}
	
	private World world()
	{
		return CaveManager.instance.world();
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
		
		private void place(CaveGenerator generator, String altPreset, Random rand, long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ, float scale, float slopeXZ, float slopeY, int position, int distance, double scaleY)
		{
			switch(this)
			{
				case VANILLA_BRANCHES:
					
					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, rand.nextFloat() * 0.5F + 0.5F, slopeXZ - Values.PI_OVER_2, slopeY / 3.0F, position, distance, 1.0D);
					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, rand.nextFloat() * 0.5F + 0.5F, slopeXZ + Values.PI_OVER_2, slopeY / 3.0F, position, distance, 1.0D);
					
					break;
					
				case MATCHING_BRANCHES:

					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ - Values.PI_OVER_2, slopeY / 3.0F, position, distance, scaleY);
					generator.addTunnel(rand.nextLong(), chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ + Values.PI_OVER_2, slopeY / 3.0F, position, distance, scaleY);
					
					break;
					
				case PRESET:
					
					CaveGenerator altGenerator = CaveInit.GENERATORS.get(altPreset);
					
					float direction = rand.nextBoolean() ? -Values.PI_OVER_2 : Values.PI_OVER_2;
					
					altGenerator.addTunnel(seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ + direction, slopeY, position, distance, scaleY);
					
					break;

				case ROOM:

					generator.addRoom(seed, chunkX, chunkZ, primer, posX, posY, posZ);
					
					break;

				default: return;
			}
		}
	}
	
	protected void addRoom(long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ)
	{
		addTunnel(seed, chunkX, chunkZ, primer, posX, posY, posZ, 1.0F + rand().nextFloat() * roomScale, 0.0F, 0.0F, -1, -1, roomScaleY);
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
	 * @param startingPoint Not entirely sure. Seems to indicate whether a room should be spawned instead of a tunnel.
	 * @param distance      The length of the tunnel. 0 = get distance from MapGenBase.
	 * @param scaleY        A vertical cave size multiple. 1.0 = same as scale.
	 */
	protected void addTunnel(long seed, int chunkX, int chunkZ, ChunkPrimer primer, double posX, double posY, double posZ, float scale, float slopeXZ, float slopeY, int startingPoint, int distance, double scaleY)
	{
		double centerX = chunkX * 16 + 8,
			   centerZ = chunkZ * 16 + 8;

		float twistXZ = startingTwistXZ, twistY = startingTwistY;
		
		Random localRandom = new Random(seed);
		
		if (distance <= 0)
		{
			int chunkDistance = range() * 16 - 16;
			
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
			
			//In vanilla, slopes are recalculated on subsequent iterations.
			slopeXZ += twistXZ * 0.1F;
			slopeY += twistY * 0.1F;

			//Rotates the beginning of the line around the end.
			twistY = (float) Math.pow(twistY, twistYExponent);
			twistY *= twistYFactor;
			twistY += twistYRandFactor * (localRandom.nextFloat() - localRandom.nextFloat()) * localRandom.nextFloat();

			//Positive is counterclockwise, negative is clockwise.
			twistXZ = (float) Math.pow(twistXZ, twistXZExponent);
			twistXZ *= twistXZFactor;
			twistXZ += twistXZRandFactor * (localRandom.nextFloat() - localRandom.nextFloat()) * localRandom.nextFloat();

			scale = (float) Math.pow(scale, scaleExponent);
			scale *= scaleFactor;
			scale += scaleRandFactor * (indRand().nextFloat() - 0.5F);
			if (scale < 0) scale = 0;
			
			scaleY = (float) Math.pow(scaleY, scaleYExponent);
			scaleY *= scaleYFactor;
			scaleY += scaleYRandFactor * (indRand().nextFloat() - 0.5F);
			if (scaleY < 0) scaleY = 0;

			if (!isRoom && scale > 1.0F && distance > 0)
			{
				if (position == randomSegmentIndex )
				{
					extRand.place(this, extBeginningPreset, localRandom, seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ, slopeY, position, distance, scaleY);
					
					if (!extRand.equals(Extension.NONE)) return;
				}
				
				if (position == startingPoint)
				{
					extBeginning.place(this, extRandPreset, localRandom, seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ, slopeY, position, distance, scaleY);
					
					if (!extBeginning.equals(Extension.NONE)) return;
				}
				
				if (position == distance)
				{
					extEnd.place(this, extEndPreset, localRandom, seed, chunkX, chunkZ, primer, posX, posY, posZ, scale, slopeXZ, slopeY, position, distance, scaleY);
					
					if (!extEnd.equals(Extension.NONE)) return;
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
					int startX = MathHelper.floor(posX - stretchXZ) - chunkX * 16 - 1,
					    endX = MathHelper.floor(posX + stretchXZ) - chunkX * 16 + 1,
					    startY = MathHelper.floor(posY - stretchY) - 1,
					    endY = MathHelper.floor(posY + stretchY) + 1,
					    startZ = MathHelper.floor(posZ - stretchXZ) - chunkZ * 16 - 1,
					    endZ = MathHelper.floor(posZ + stretchXZ) - chunkZ * 16 + 1;

					if (startX < 0)	startX = 0;
					if (endX > 16) endX = 16;
					if (startY < 1) startY = 1;
					if (endY > 248) endY = 248;
					if (startZ < 0) startZ = 0;
					if (endZ > 16) endZ = 16;
					
					boolean waterBlockFound = testForWater(primer, stretchXZ, stretchY, chunkX, chunkZ, startX, endX, posX, startY, endY, posY, startZ, endZ, posZ);
					
					if (!waterBlockFound)
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
	
	/**
	 * Original version of the algorithm. Does not match replaceSection()
	 * 
	 * Delete me.
	 */
	private boolean testForWater2(ChunkPrimer primer, int chunkX, int chunkZ, int x1, int x2, int y1, int y2, int z1, int z2)
	{
		for (int x = x1; x < x2; x++)
		{
			for (int z = z1; z < z2; z++)
			{
				for (int y = y1 + 1; y >= y2 - 1; y--)
				{
					if (y >= 0 && y < 256)
					{
						if (isOceanBlock(primer, x, y, z, chunkX, chunkZ))
						{
							return true;
						}

						if (y != y1 - 1 && x != x1 && x != x2 - 1 && z != z1 && z != z2 - 1)
						{
							y = y1; //Reset y
						}
					}
				}
			}
		}
		
		return false;
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
	
	private void replaceSection(boolean airOnly, ChunkPrimer primer, double stretchXZ, double stretchY, int chunkX, int chunkZ, int x1, int x2, double posX, int y1, int y2, double posY, int z1, int z2, double posZ)
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
		Biome biome = world().getBiome(new BlockPos(x + chunkX * 16, 0, z + chunkZ * 16));
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
	 */
	protected void replaceBlock(boolean airOnly, ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop)
	{
		Biome biome = world().getBiome(new BlockPos(x + (chunkX * 16), 0, z + (chunkZ * 16)));
		IBlockState state = data.getBlockState(x, y, z);
		IBlockState up = (IBlockState) MoreObjects.firstNonNull(data.getBlockState(x, y + 1, z), Values.BLK_AIR);
		IBlockState top = biome.topBlock;
		IBlockState filler = biome.fillerBlock;
				
		int yDown = y - 1;
		
		if (canReplaceBlock(state, up) || fillBlocks != null || state.getBlock().equals(top.getBlock()) || state.getBlock().equals(filler.getBlock()))
		{
			if (yDown < lavaMaxHeight)
			{
				data.setBlockState(x, y, z, Values.BLK_LAVA);
				
				return;
			}
			
			else if (!airOnly && fillBlocks != null)
			{
				for (BlockFiller replacement : fillBlocks)
				{					
					if (replacement.canGenerateAtHeight(y) && indRand().nextDouble() * 100 <= replacement.getChance())
					{
						boolean replaceAtMatch = replacement.getPreference().equals(Preference.REPLACE_MATCH),
								matchFound = false,
								breakFromLoop = false;

						if (replacement.hasMatchers())
						{
							BlockPos blockUp = new BlockPos(x, y + 1, z),
									 blockDown = new BlockPos(x, yDown, z),
									 blockNorth = new BlockPos(x + 1, y, z),
									 blockSouth = new BlockPos(x - 1, y, z),
									 blockEast = new BlockPos(x, y, z + 1),
									 blockWest = new BlockPos(x, y, z - 1);

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
												//if (isChunkLoaded(chunkX, chunkZ, pos.getX(), pos.getZ()))
												{
													if (data.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(matcher))
													{
														if (replaceAtMatch)
														{
															data.setBlockState(pos.getX(), pos.getY(), pos.getZ(), replacement.getFillBlock());
															
															break;
														}
														
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
												//if (isChunkLoaded(chunkX, chunkZ, pos.getX(), pos.getZ()))
												{
													if (data.getBlockState(pos.getX(), pos.getY(), pos.getZ()).equals(matcher))
													{
														if (replaceAtMatch)
														{
															data.setBlockState(pos.getX(), pos.getY(), pos.getZ(), replacement.getFillBlock());

															break; //Just break. Make sure the block at position can also be replaced correctly.
														}
														
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
							
							return;
						}
					}
				}
			}
			
			data.setBlockState(x, y, z, Values.BLK_AIR);

			if (foundTop && data.getBlockState(x, yDown, z).getBlock().equals(filler.getBlock()))
			{
				data.setBlockState(x, yDown, z, top.getBlock().getDefaultState());
			}
		}
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
		
		return world().isChunkGeneratedAt(chunkX, chunkZ);
	}
	
	protected boolean canReplaceBlock(IBlockState state, IBlockState biomeFiller)
	{
		if (state.getBlock().equals(Blocks.STONE)) return true;  //First most common block
		
		if ((state.getBlock().equals(Blocks.AIR))) return false; //Second most common (from overlapping generation)
		
		for (Block replaceableBlock : Values.replaceableBlocks) //Others
		{
			if (state.getBlock().equals(replaceableBlock)) return true;
		}
		
		return (state.getBlock().equals(Blocks.SAND) || state.getBlock().equals(Blocks.GRAVEL)) && 
				!biomeFiller.getMaterial().equals(Material.WATER);
	}
}