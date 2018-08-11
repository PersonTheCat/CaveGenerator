package com.personthecat.cavegenerator.util;

import java.awt.Point;

public class RandomChunkSelector
{
	private HashGenerator noise;

	private static final double SELECTION_THRESHOLD = 80.0;

	public RandomChunkSelector(Long worldSeed)
	{
		this.noise = new HashGenerator(worldSeed);		
	}
	
	public boolean getBooleanForCoordinates(int ID, int x, int y)
	{
		return noise.getHash(x, ID, y) > SELECTION_THRESHOLD;
	}
}