package com.personthecat.cavegenerator.util;

import java.awt.Point;

public class RandomChunkSelector
{
	private HashGenerator noise;

	private static final double SELECTION_THRESHOLD = 78.0; //Can't go higher.

	public RandomChunkSelector(Long worldSeed)
	{
		this.noise = new HashGenerator(worldSeed);		
	}
	
	public boolean getBooleanForCoordinates(int ID, int x, int y)
	{
		return noise.getHash(x, ID, y) > SELECTION_THRESHOLD;
	}
	
//	public double getProbabilityForCoordinates(int ID, int x, int y)
//	{
//		Point center = new Point(x, y);
//		
//		if (getBooleanForCoordinates(ID, x, y)) return 100.0;
//
//		if (getBooleanForDistance(ID, center, 1)) return 66.6;
//		
//		if (getBooleanForDistance(ID, center, 2)) return 33.3;
//		
//		return DEFAULT_PROBABILITY;
//	}
//	
//	public boolean getBooleanForDistance(int ID, Point center, int radius)
//	{
//		int diameter = (radius * 2) + 1;
//		int innerLength = diameter - 2;
//		int shift = (radius - 1) * -1;
//		int nRadius = radius * -1;
//		
//		int x = center.x, y = center.y;
//
//		//Start with the corners.
//		if (getBooleanForCoordinates(ID, x + radius, y + radius)) return true;
//		if (getBooleanForCoordinates(ID, x + nRadius, y + nRadius)) return true;
//		if (getBooleanForCoordinates(ID, x + radius, y + nRadius)) return true;
//		if (getBooleanForCoordinates(ID, x + nRadius, y + radius)) return true;
//		
//		//Get the points between the corners.
//		for (int i = 0 + shift; i < innerLength + shift; i++)
//		{
//			if (getBooleanForCoordinates(ID, x + radius, y + i)) return true;
//			if (getBooleanForCoordinates(ID, x + i, y + radius)) return true;
//			if (getBooleanForCoordinates(ID, x + nRadius, y + i)) return true;
//			if (getBooleanForCoordinates(ID, x + i, y + nRadius)) return true;
//		}
//		
//		return false;
//	}
}