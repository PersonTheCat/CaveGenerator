package com.personthecat.cavegenerator.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorSimplex;

/**
 * Not an actual stone replacer. Just stores the information.
 */
public class StoneReplacer
{
	public static void setWorld(World world)
	{
		Random master = new Random(world.getSeed());
		
		for (StoneLayer layer : StoneLayer.STONE_LAYER_REGISTRY)
		{
			layer.noise = new NoiseGeneratorSimplex(master);
		}
		
		for (StoneCluster cluster : StoneCluster.STONE_CLUSTER_REGISTRY)
		{
			cluster.ID = master.nextInt(0x2 << 14);
		}
	}
	
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

		private int ID, radius, radiusVariance, startingHeight, heightVariance;

		public StoneCluster(IBlockState state, int radiusVariance, int radius, int startingHeight, int heightVariance)
		{
			this.state = state;
			this.radiusVariance = radiusVariance;
			this.radius = radius;
			this.startingHeight = startingHeight;
			this.heightVariance = heightVariance;
			
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
    	
		public int getRadiusVariance()
		{
			return radiusVariance;
		}
    	
    	public int getRadius()
    	{
    		return radius;
    	}
    	
    	public int getStartingHeight()
    	{
    		return startingHeight;
    	}
    	
    	public int getHeightVariance()
    	{
    		return heightVariance;
    	}
	}
	
	public static class ClusterInfo
	{
		private BlockPos center;
		
		//Squared integers. Keep the original radiusY to avoid unnecessary calculations.
		private int radiusY, radiusX2, radiusY2, radiusZ2;
		
		private StoneCluster cluster;
		
		public ClusterInfo(StoneCluster cluster, BlockPos center, int radiusY, int radiusX2, int radiusY2, int radiusZ2)
		{
			this.cluster = cluster;
			this.center = center;
			this.radiusY = radiusY;
			this.radiusX2 = radiusX2;
			this.radiusY2 = radiusY2;
			this.radiusZ2 = radiusZ2;
		}
		
		public BlockPos getCenter()
		{
			return center;
		}
		
		public int getRadiusY()
		{
			return radiusY;
		}

		public int getRadiusX2()
		{
			return radiusX2;
		}

		public int getRadiusY2()
		{
			return radiusY2;
		}

		public int getRadiusZ2()
		{
			return radiusZ2;
		}

		public StoneCluster getCluster()
		{
			return cluster;
		}
	}
}
