package com.personthecat.cavegenerator.world.feature;

import java.util.Random;

import javax.annotation.Nullable;

import com.personthecat.cavegenerator.util.Values;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import static net.minecraft.block.BlockStairs.EnumHalf;
import static net.minecraft.block.BlockStairs.EnumShape;

public class GiantPillar extends WorldGenerator
{	
	private final IBlockState pillarBlock;
	private final int frequency, minHeight, maxHeight, minLength, maxLength;
	
	private BlockStairs stairBlock;
	
	public GiantPillar(int frequency, int minHeight, int maxHeight, int minLength, int maxLength, IBlockState pillarBlock, @Nullable Block stairBlock)
	{
		this.frequency = frequency;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.pillarBlock = pillarBlock;
		
		if (minHeight < 0) minHeight = 0;
		
		if (stairBlock != null && stairBlock instanceof BlockStairs)
		{
			this.stairBlock = (BlockStairs) stairBlock;
		}
	}
	
	public int getFrequency()
	{
		return frequency;
	}
	
	public int getMinHeight()
	{
		return minHeight;
	}
	
	public int getMaxHeight()
	{
		return maxHeight;
	}
	
	@Override //@param pos is the top block in the pillar.
	public boolean generate(World world, Random rand, BlockPos pos)
	{
		int actualMax = pos.getY();		
		int actualMin = getLowestBlock(world, pos);
		
		if (actualMin < 0) return false;
		
		int length = actualMax - actualMin;

		if (length < minLength || length > maxLength) return false;
		
		for (int y = actualMax; y >= actualMin; y--)
		{
			BlockPos current = new BlockPos(pos.getX(), y, pos.getZ());
			
			world.setBlockState(current, pillarBlock, 2);
			
			if (stairBlock != null && !stairBlock.equals(Values.BLK_AIR.getBlock()))
			{
				if (y == actualMax)
				{
					testPlaceStairs(world, rand, pos, EnumHalf.TOP);
				}
				else if (y == actualMin)
				{
					testPlaceStairs(world, rand, new BlockPos(pos.getX(), y, pos.getZ()), EnumHalf.BOTTOM);
				}
			}
		}
		
		return true;
	}
	
	private int getLowestBlock(World world, BlockPos pos)
	{
		boolean previouslyAir = true;
		
		for (pos = pos.down(); pos.getY() > minHeight; pos = pos.down())
		{
			boolean currentlyAir = !world.getBlockState(pos).isOpaqueCube();
			
			if (previouslyAir && !currentlyAir)
			{
				return pos.getY();
			}
			
			previouslyAir = currentlyAir;
		}
		
		return -1;
	}
	
	private void testPlaceStairs(World world, Random rand, BlockPos pos, EnumHalf topOrBottom)
	{		
		if (topOrBottom.equals(EnumHalf.TOP))
		{
			for (BlockPos north : getPositionsToScan(pos.north()))
			{
				BlockPos up = north.up();
				
				if (rand.nextInt(2) == 0)
				{
					if (world.getBlockState(up).isOpaqueCube() && world.getBlockState(north).equals(Values.BLK_AIR))
					{
						world.setBlockState(north, getStairRotation(EnumFacing.SOUTH, topOrBottom), 16);
						
						break;
					}
				}
			}
			for (BlockPos south : getPositionsToScan(pos.south()))
			{
				BlockPos up = south.up();
				
				if (rand.nextInt(2) == 0)
				{
					if (world.getBlockState(up).isOpaqueCube() && world.getBlockState(south).equals(Values.BLK_AIR))
					{
						world.setBlockState(south, getStairRotation(EnumFacing.NORTH, topOrBottom), 16);
						
						break;
					}
				}
			}
			for (BlockPos east : getPositionsToScan(pos.east()))
			{
				BlockPos up = east.up();
				
				if (rand.nextInt(2) == 0)
				{
					if (world.getBlockState(up).isOpaqueCube() && world.getBlockState(east).equals(Values.BLK_AIR))
					{
						world.setBlockState(east, getStairRotation(EnumFacing.WEST, topOrBottom), 16);
						
						break;
					}
				}
			}
			for (BlockPos west : getPositionsToScan(pos.west()))
			{
				BlockPos up = west.up();
				
				if (rand.nextInt(2) == 0)
				{
					if (world.getBlockState(up).isOpaqueCube() && world.getBlockState(west).equals(Values.BLK_AIR))
					{
						world.setBlockState(west, getStairRotation(EnumFacing.EAST, topOrBottom), 16);
						
						break;
					}
				}
			}
		}
		else
		{
			for (BlockPos north : getPositionsToScan(pos.north()))
			{
				BlockPos down = north.down();
				
				if (down.getY() >= minHeight && rand.nextInt(2) == 0)
				{
					if (world.getBlockState(down).isOpaqueCube() && world.getBlockState(north).equals(Values.BLK_AIR))
					{
						world.setBlockState(north, getStairRotation(EnumFacing.SOUTH, topOrBottom), 16);
						
						break;
					}
				}
			}
			for (BlockPos south : getPositionsToScan(pos.south()))
			{
				BlockPos down = south.down();
				
				if (down.getY() >= minHeight && rand.nextInt(2) == 0)
				{
					if (world.getBlockState(down).isOpaqueCube() && world.getBlockState(south).equals(Values.BLK_AIR))
					{
						world.setBlockState(south, getStairRotation(EnumFacing.NORTH, topOrBottom), 16);
						
						break;
					}
				}
			}
			for (BlockPos east : getPositionsToScan(pos.east()))
			{
				BlockPos down = east.down();
				
				if (down.getY() >= minHeight && rand.nextInt(2) == 0)
				{
					if (world.getBlockState(down).isOpaqueCube() && world.getBlockState(east).equals(Values.BLK_AIR))
					{
						world.setBlockState(east, getStairRotation(EnumFacing.WEST, topOrBottom), 16);
						
						break;
					}
				}
			}
			for (BlockPos west : getPositionsToScan(pos.west()))
			{
				BlockPos down = west.down();
				
				if (down.getY() >= minHeight && rand.nextInt(2) == 0)
				{
					if (world.getBlockState(down).isOpaqueCube() && world.getBlockState(west).equals(Values.BLK_AIR))
					{
						world.setBlockState(west, getStairRotation(EnumFacing.EAST, topOrBottom), 16);
						
						break;
					}
				}
			}
		}
	}
	
	private BlockPos[] getPositionsToScan(BlockPos pos)
	{
		return new BlockPos[]
		{
		 	pos,
		 	pos.up(),
		 	pos.up(2),
		 	pos.up(3),
		 	pos.down(),
		 	pos.down(2),
		 	pos.down(3)
		};
	}
	
	private IBlockState getStairRotation(EnumFacing facing, EnumHalf topOrBottom)
	{
		return stairBlock.getDefaultState().withProperty(BlockStairs.FACING, facing).withProperty(BlockStairs.HALF, topOrBottom).withProperty(BlockStairs.SHAPE, EnumShape.STRAIGHT);
	}
}