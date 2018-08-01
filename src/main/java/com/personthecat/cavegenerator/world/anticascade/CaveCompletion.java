package com.personthecat.cavegenerator.world.anticascade;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.personthecat.cavegenerator.util.CommonMethods;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class CaveCompletion
{
	/**
	 * Similar to ChunkPrimer
	 */
	public static class ChunkCorrections implements Serializable
	{
		//Using an object so values can be null.
		private final Character[] corrections = new Character[65536];
		
		private ChunkCoordinates location;
		
		public ChunkCorrections(ChunkCoordinates location)
		{
			this.location = location;
		}
		
		public ChunkCoordinates getCoordinates()
		{
			return location;
		}
		
		public void addCorrection(int x, int y, int z, IBlockState state)
		{
			corrections[getBlockIndex(x, y, z)] = (char) Block.getStateId(state);
		}
		
		/**
		 * Should not return air instead of null
		 */
		public IBlockState getCorrection(int x, int y, int z)
		{
			Character ch = null;
					
			try
			{
				ch = corrections[getBlockIndex(x, y, z)];
			}
			
			catch (NullPointerException e) {return null;}
			
			if (ch == null) return null;
			
			return Block.getStateById(ch);
		}
		
	    private static int getBlockIndex(int x, int y, int z)
	    {
	        return x << 12 | z << 8 | y;
	    }

	    private void writeObject(ObjectOutputStream out) throws IOException
	    {
	    	out.writeInt(location.x);
	    	out.writeInt(location.z);
	    	
	    	try
	    	{
		    	for (int i = 0; i < corrections.length; i++)
		    	{
		    		try
		    		{
			    		if (corrections[i] != '\u0000') //Probably not necessary.
			    		{
			    			out.writeInt(i);
			    			out.writeChar(corrections[i]);
			    		}
		    		}
		
		    		catch (NullPointerException ignored) {/*Nothing to write*/} 
		    	}
	    	}
	    	
	    	catch (NullPointerException ignored) {/*No corrections to read*/}
	    }
	    
	    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException
	    {
	    	if (in.available() > 0) //Usable information exists.
	    	{
		    	this.location = new ChunkCoordinates(in.readInt(), in.readInt());
		    	
		    	for (int i = 0; i < in.available(); i = i + 2)
		    	{
		    		this.corrections[in.readInt()] = in.readChar();
		    	}
	    	}
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