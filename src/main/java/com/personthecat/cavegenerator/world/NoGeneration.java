package com.personthecat.cavegenerator.world;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

public class NoGeneration extends MapGenBase
{
	@Override
	public void generate(World worldIn, int x, int z, ChunkPrimer primer) {}
}
