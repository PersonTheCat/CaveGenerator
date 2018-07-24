package com.personthecat.cavegenerator.world;

import java.io.Serializable;

import com.personthecat.cavegenerator.util.CommonMethods;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class CaveCompletion
{
	public static class ChunkCorrections implements Serializable
	{
		private static final int PLS_BALANCE_CAREFULLY = 8192;
		
		private ChunkCoordinates location;
		private BlockReplacement[] replacements = new BlockReplacement[PLS_BALANCE_CAREFULLY];
		
		private int lastIndex = 0;
		
		public ChunkCorrections(ChunkCoordinates location)
		{
			this.location = location;
		}
		
		public ChunkCoordinates getCoordinates()
		{
			return location;
		}
		
		public void addReplacement(BlockReplacement replacement)
		{
			this.replacements[lastIndex] = replacement;
			
			lastIndex++;
		}
		
		public BlockReplacement[] getReplacements()
		{
			return replacements;
		}
		
		public int getLastIndex()
		{
			return lastIndex;
		}
	}
	
	public static class BlockReplacement implements Serializable
	{
		private String state;
		private int x, y, z;
		
		public BlockReplacement(IBlockState state, BlockPos pos)
		{
			Block block = state.getBlock();
			
			this.state = block.getRegistryName().toString() + ":" + block.getMetaFromState(state);
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
		}
		
		public IBlockState getState()
		{
			return CommonMethods.getBlockState(state);
		}
		
		public BlockPos getPos()
		{
			return new BlockPos(x, y, z);
		}
	}
	
	public static class ChunkCoordinates implements Serializable
	{
		private int x, z;
		
		public ChunkCoordinates(int x, int z)
		{
			this.x = x;
			this.z = z;
		}
		
		public int getX()
		{
			return x;
		}
		
		public int getZ()
		{
			return z;
		}
		
		public boolean equals(ChunkCoordinates coordinates)
		{
			return x == coordinates.x && z == coordinates.z;
		}
	}
}