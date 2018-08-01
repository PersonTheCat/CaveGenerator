package com.personthecat.cavegenerator.util;

/**
 * Generates noise quickly. Useful when shape isn't so important.
 */
public class HashGenerator
{
	private static final int
	
		X_MULTIPLE = 0x653,  //1619
		Y_MULTIPLE = 0x7A69, //31337
		Z_MULTIPLE = 0x1B3B; //6971
	
	private static final long 
	
		GENERAL_MULTIPLE = 0x5DEECE66DL, //25214903917
		ADDEND = 0xBL,                   //11
		MASK = 0xFFFFFFFFFFFFL,          //281474976710656
		SCALE = 0x16345785D8A0000L;	     //E18
	
	private long seed;
	
	public HashGenerator(long seed)
	{
		this.seed = scramble(seed);
	}
	
	public double getHash(int x, int y, int z)
	{
		long hash = seed;
		
		hash ^= x * X_MULTIPLE;
		hash ^= y * Y_MULTIPLE;
		hash ^= z * Z_MULTIPLE;
		
		hash *= hash;
		
		return ((hash >> 13) ^ hash) / SCALE;
	}
	
	/*
	 * Similar to Random's scramble method.
	 */
	private static long scramble(long seed)
	{
		long newseed = (seed ^ GENERAL_MULTIPLE) & MASK;
		
		return (newseed * GENERAL_MULTIPLE + ADDEND) & MASK;
	}	
}