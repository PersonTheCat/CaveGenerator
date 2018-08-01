package com.personthecat.cavegenerator.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.personthecat.cavegenerator.util.SimplexNoiseGenerator3D;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

/**
 * Not an actual stone replacer. Just stores the information.
 */
public class StoneReplacer
{
	public static class StoneLayer
	{
    	private static final List<StoneLayer> STONE_LAYER_REGISTRY = new ArrayList<>();
		
		private IBlockState state;
    	
    	private int maxHeight;
    	
    	public NoiseGeneratorSimplex noise;
    	
    	public StoneLayer(int maxHeight, IBlockState state)
    	{
    		this.state = state;
    		this.maxHeight = maxHeight;
    		
    		STONE_LAYER_REGISTRY.add(this);
    	}
    	
    	public IBlockState getState()
    	{
    		return state;
    	}
    	
    	public int getMaxHeight()
    	{
    		return maxHeight;
    	}
    	
    	public static List<IBlockState> getAllLayers()
    	{
    		List<IBlockState> layers = new ArrayList<>();
    		
    		for (StoneLayer layer : STONE_LAYER_REGISTRY)
    		{
    			layers.add(layer.state);
    		}
    		
    		return layers;
    	}
	}
	
	public static class StoneCluster
	{
		private static final List<StoneCluster> STONE_CLUSTER_REGISTRY = new ArrayList<>();
		
		private IBlockState state;

		private int ID, radius, noiseLevel;

		public StoneCluster(IBlockState state, int noise, int radius)
		{
			this.state = state;
			this.noiseLevel = noise;
			this.radius = radius;
			
			STONE_CLUSTER_REGISTRY.add(this);
		}
		
    	public IBlockState getState()
    	{
    		return state;
    	}
    	
    	public int getID()
    	{
    		return ID;
    	}
    	
		public int getNoiseLevel()
		{
			return noiseLevel;
		}
    	
    	public int getRadius()
    	{
    		return radius;
    	}
	}
	
	public static void setWorld(World world)
	{
		Random master = new Random(world.getSeed());
		
		for (StoneLayer layer : StoneLayer.STONE_LAYER_REGISTRY)
		{
			layer.noise = new NoiseGeneratorSimplex(master);
		}
		
		for (StoneCluster cluster : StoneCluster.STONE_CLUSTER_REGISTRY)
		{
			cluster.ID = master.nextInt(0x2 >> 14);
		}
	}
}
