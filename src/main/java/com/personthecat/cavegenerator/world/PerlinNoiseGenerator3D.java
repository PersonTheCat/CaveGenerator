package com.personthecat.cavegenerator.world;

import java.util.Random;

public class PerlinNoiseGenerator3D
{
	int B = 256;
	int[] perms = new int[B + B];

	public PerlinNoiseGenerator3D(long seed)
	{
		Random rand = new Random(seed);

		int i = 0, j, k;
		
		for (; i < B; i++)
		{
			perms[i] = i;
		}

		while (i != 0)
		{
			k = perms[i];
			j = rand.nextInt(B);
			perms[i] = perms[j];
			perms[j] = k;
			i--;
		}

		for (i = 0; i < B; i++)
		{
			perms[B + i] = perms[i];
		}
	}

	public float getNoise(float x, float y, float z)
	{
		int ix0, iy0, ix1, iy1, iz0, iz1;
		float fx0, fy0, fz0, fx1, fy1, fz1;
		float s, t, r;
		float nxy0, nxy1, nx0, nx1, n0, n1;

		ix0 = floor(x); // Integer part of x
		iy0 = floor(y); // Integer part of y
		iz0 = floor(z); // Integer part of z
		fx0 = x - ix0; // Fractional part of x
		fy0 = y - iy0; // Fractional part of y
		fz0 = z - iz0; // Fractional part of z
		fx1 = fx0 - 1.0f;
		fy1 = fy0 - 1.0f;
		fz1 = fz0 - 1.0f;
		ix1 = (ix0 + 1) & 0xff; // Wrap to 0..255
		iy1 = (iy0 + 1) & 0xff;
		iz1 = (iz0 + 1) & 0xff;
		ix0 = ix0 & 0xff;
		iy0 = iy0 & 0xff;
		iz0 = iz0 & 0xff;

		r = fade(fz0);
		t = fade(fy0);
		s = fade(fx0);

		nxy0 = grad3(perms[ix0 + perms[iy0 + perms[iz0]]], fx0, fy0, fz0);
		nxy1 = grad3(perms[ix0 + perms[iy0 + perms[iz1]]], fx0, fy0, fz1);
		nx0 = lerp(r, nxy0, nxy1);

		nxy0 = grad3(perms[ix0 + perms[iy1 + perms[iz0]]], fx0, fy1, fz0);
		nxy1 = grad3(perms[ix0 + perms[iy1 + perms[iz1]]], fx0, fy1, fz1);
		nx1 = lerp(r, nxy0, nxy1);

		n0 = lerp(t, nx0, nx1);

		nxy0 = grad3(perms[ix1 + perms[iy0 + perms[iz0]]], fx1, fy0, fz0);
		nxy1 = grad3(perms[ix1 + perms[iy0 + perms[iz1]]], fx1, fy0, fz1);
		nx0 = lerp(r, nxy0, nxy1);

		nxy0 = grad3(perms[ix1 + perms[iy1 + perms[iz0]]], fx1, fy1, fz0);
		nxy1 = grad3(perms[ix1 + perms[iy1 + perms[iz1]]], fx1, fy1, fz1);
		nx1 = lerp(r, nxy0, nxy1);

		n1 = lerp(t, nx0, nx1);

		return 0.936f * lerp(s, n0, n1);
	}

	public float getFractalNoise(float x, float y, float z, int octaves, float frq, float amp)
	{
		float gain = 1.0f, sum = 0.0f;

		for (int i = 0; i < octaves; i++)
		{
			sum += getNoise(x * gain / frq, y * gain / frq, z * gain / frq) * amp / gain;
			gain *= 2.0f;
		}
		
		return sum;
	}

	private static int floor(double value)
	{
		int asInt = (int) value;

		return value < asInt ? asInt - 1 : asInt;
	}
	
	private float fade(float t)
	{
		return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
	}

	private float lerp(float t, float a, float b)
	{
		return a + t * (b - a);
	}

	private float grad3(int hash, float x, float y, float z)
	{
		int h = hash & 15;
		float u = h < 8 ? x : y;
		float v = (h < 4) ? y : (h == 12 || h == 14) ? x : z;
		
		return (((h & 1) != 0) ? -u : u) + (((h & 2) != 0) ? -v : v);
	}
}