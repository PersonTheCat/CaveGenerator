package com.personthecat.cavegenerator.util;

public class RandomChunkSelector
{
	private HashGenerator noise;
	
	public RandomChunkSelector(Long worldSeed)
	{
		this.noise = new HashGenerator(worldSeed);		
	}
	
	public boolean getBooleanForCoordinates(int ID, int x, int y, double threshold)
	{
		return noise.getHash(ID, x, y) > threshold;
	}
}