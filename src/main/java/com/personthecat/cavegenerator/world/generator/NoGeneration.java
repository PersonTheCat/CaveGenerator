package com.personthecat.cavegenerator.world.generator;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.MapGenMineshaft;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class NoGeneration extends MapGenMineshaft {

    private static final NoGeneration INSTANCE = new NoGeneration();

    /** Prevent instantiation. */
    private NoGeneration() {}

    public static NoGeneration getInstance() {
        return INSTANCE;
    }

    @Override
    public void generate(World worldIn, int x, int z, ChunkPrimer primer) {}
}