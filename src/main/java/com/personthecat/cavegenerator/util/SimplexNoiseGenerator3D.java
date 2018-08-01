package com.personthecat.cavegenerator.util;

import java.util.Random;

import net.minecraft.util.math.MathHelper;

/**
 * Based on the work of Stefan Gustavson and Peter Eastman.
 */
public class SimplexNoiseGenerator3D
{
	private static final double 
		
		skew = 1.0 / 3.0,
		unskew = 1.0 / 6.0;
	
	private static Grad[] grads = 
	{
	 	grad(1, 1, 0), grad(-1, 1, 0), grad(1, -1, 0), grad(-1, -1, 0),
	 	grad(1, 0, 1), grad(-1, 0, 1), grad(1, 0, -1), grad(-1, 0, -1),
	 	grad(0, 1, 1), grad(0, -1, 1), grad(0, 1, -1), grad(0, -1, -1)
	};

	private static Random rand;
	
	private short[] shorts, permutations, permsModulus;
	
	public SimplexNoiseGenerator3D(long seed)
	{
		rand = new Random(seed);
		
		shorts = getShortsRandomized(256);
		permutations = getPermutations(512);
		permsModulus = getPermutationsModulus();
	}
	
	public Random getRandom()
	{
		return rand;
	}
	
	public double getNoise(double x, double y, double z)
	{
		double corner0 = 0.0, 
			   corner1 = 0.0, 
			   corner2 = 0.0, 
			   corner3 = 0.0;
	    
		double skewed = (x + y + z) * skew;
	    
	    int xFloored = floor(x + skewed),
	    	yFloored = floor(y + skewed),
	    	zFloored = floor(z + skewed);
	    
	    double unskewed = (xFloored + yFloored + zFloored) * unskew;
	    
	    double xOrigin = xFloored - unskewed,
	    	   yOrigin = yFloored - unskewed,
	    	   zOrigin = zFloored - unskewed;
	    
	    double xOffset0 = x - xOrigin,
	    	   yOffset0 = y - yOrigin,
	    	   zOffset0 = z - zOrigin;

		int xOffset1, yOffset1, zOffset1,
			xOffset2, yOffset2, zOffset2;
		
		if (xOffset0 >= yOffset0)
		{
			if (yOffset0 >= zOffset0)
			{
				xOffset1 = 1; yOffset1 = 0;	zOffset1 = 0;
				xOffset2 = 1; yOffset2 = 1; zOffset2 = 0;
			}
			else if (xOffset0 >= zOffset0)
			{
				xOffset1 = 1; yOffset1 = 0; zOffset1 = 0;
				xOffset2 = 1; yOffset2 = 0; zOffset2 = 1;
			}
			else
			{
				xOffset1 = 0; yOffset1 = 0; zOffset1 = 1;
				xOffset2 = 1; yOffset2 = 0; zOffset2 = 1;
			}
		}
		else
		{
			if (yOffset0 < zOffset0)
			{
				xOffset1 = 0; yOffset1 = 0; zOffset1 = 1;
				xOffset2 = 0; yOffset2 = 1; zOffset2 = 1;
			}
			else if (xOffset0 < zOffset0)
			{
				xOffset1 = 0; yOffset1 = 1; zOffset1 = 0;
				xOffset2 = 0; yOffset2 = 1; zOffset2 = 1;
			}
			else
			{
				xOffset1 = 0; yOffset1 = 1; zOffset1 = 0;
				xOffset2 = 1; yOffset2 = 1; zOffset2 = 0;
			}
		}

		double x1 = xOffset0 - xOffset1 + unskew,
			   y1 = yOffset0 - yOffset1 + unskew,
			   z1 = zOffset0 - zOffset1 + unskew,
			   x2 = xOffset0 - xOffset2 + (2.0 * unskew),
			   y2 = yOffset0 - yOffset2 + (2.0 * unskew),
			   z2 = zOffset0 - zOffset2 + (2.0 * unskew),
			   x3 = xOffset0 - 1.0 + (3.0 * unskew),
			   y3 = yOffset0 - 1.0 + (3.0 * unskew),
			   z3 = zOffset0 - 1.0 + (3.0 * unskew);
		
		int xAnd = xFloored & 255,
			yAnd = yFloored & 255,
			zAnd = zFloored & 255;

		double contribution0 = 0.6 - (xOffset0 * xOffset0) - (yOffset0 * yOffset0) - (zOffset0 * zOffset0),
			   contribution1 = 0.6 - (x1 * x1) - (y1 * y1) - (z1 * z1),
			   contribution2 = 0.6 - (x2 * x2) - (y2 * y2) - (z2 * z2),
			   contribution3 = 0.6 - (x3 * x3) - (y3 * y3) - (z3 * z3);
		
		int	mut0 = permsModulus[xAnd + permutations[yAnd + permutations[zAnd]]],
			mut1 = permsModulus[xAnd + xOffset1 + permutations[yAnd + yOffset1 + permutations[zAnd + zOffset1]]],
			mut2 = permsModulus[xAnd + xOffset2 + permutations[yAnd + yOffset2 + permutations[zAnd + zOffset2]]],
			mut3 = permsModulus[xAnd + 1 + permutations[yAnd + 1 + permutations[zAnd + 1]]];
		
		if (contribution0 > 0)
		{
			contribution0 *= contribution0;
			corner0 = contribution0 * contribution0 * dot(grads[mut0], xOffset0, yOffset0, zOffset0);
		}
		if (contribution1 > 0)
		{
			contribution1 *= contribution1;
			corner1 = contribution1 * contribution1 * dot(grads[mut1], x1, y1, z1);
		}
		if (contribution2 > 0)
		{
			contribution2 *= contribution2;
			corner2 = contribution2 * contribution2 * dot(grads[mut2], x2, y2, z2);
		}		
		if (contribution3 > 0)
		{
			contribution3 *= contribution3;
			corner3 = contribution3 * contribution3 * dot(grads[mut3], x3, y3, z3);
		}
		//Scaled total
		return (corner0 + corner1 + corner2 + corner3) * 32.0;
	}
	
	public double getFractalNoise(double x, double y, double z, int octaves, double frq, double amp)
	{
		double gain = 1.0, sum = 0.0f;

		for (int i = 0; i < octaves; i++)
		{
			sum += getNoise(x * gain / frq, y * gain / frq, z * gain / frq) * amp / gain;
			gain *= 2.0;
		}
		
		return sum;
	}
	
	public double getSphericalNoise(int radius, double longitude, double latitude, int octaves, double frq, double amp)
	{
		double planarRadius = radius * MathHelper.cos((float) latitude);
		
		double x = planarRadius * MathHelper.sin((float) longitude),
		       y = radius * MathHelper.sin((float) latitude),
		       z = planarRadius * MathHelper.cos((float) longitude);
		
		return getFractalNoise(x, y, z, octaves, frq, amp);
	}
	
	private static int floor(double value)
	{
		int asInt = (int) value;
		
		return value < asInt ? asInt - 1 : asInt;
	}
	
	private static double dot(Grad g, double x, double y, double z)
	{
		return (g.x * x) + (g.y * y) + (g.z * z);
	}
	
	private static short[] getShortsRandomized(int range)
	{
		short[] shorts = new short[range];

		for (short i = 0; i < range; i++)
		{
			shorts[i] = i;
		}
		
		return shuffle(shorts);
	}
	
	private static short[] shuffle(short[] shorts)
	{
		for (int i = 0; i < shorts.length; i++)
		{
			int index = rand.nextInt(i + 1);
			
			short a = shorts[index];
			shorts[index]  = shorts[i];
			shorts[i] = a;
		}
		
		return shorts;
	}
	
	private short[] getPermutations(int range)
	{
		short[] perms = new short[range];
		
		for (short i = 0; i < range; i++)
		{
			perms[i] = shorts[i & 255];
		}
		
		return perms;
	}
	
	private short[] getPermutationsModulus()
	{
		short[] perms = new short[permutations.length];
		
		for (short i = 0; i < permutations.length; i++)
		{			
			perms[i] = (short) (permutations[i] % 12);
		}
		
		return perms;
	}
	
	/**
	 * Shorthand
	 */
	private static Grad grad(double x, double y, double z)
	{
		return new Grad(x, y, z);
	}
	
	private static class Grad
	{
		double x, y, z;
		
		Grad(double x, double y, double z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}