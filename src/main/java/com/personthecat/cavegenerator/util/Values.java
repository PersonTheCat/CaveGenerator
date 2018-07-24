package com.personthecat.cavegenerator.util;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class Values
{
	public static final IBlockState 
    
		BLK_STONE = Blocks.STONE.getDefaultState(),
		BLK_LAVA = Blocks.LAVA.getDefaultState(),
		BLK_WATER = Blocks.WATER.getDefaultState(),
		BLK_AIR = Blocks.AIR.getDefaultState();
	
	/**
	 * A list of replaceable blocks. Stone is handled elsewhere for performance efforts.
	 */
	public static final List<Block> replaceableBlocks = Arrays.asList(new Block[] 
	{
		Blocks.DIRT,
		Blocks.GRASS,
		Blocks.HARDENED_CLAY,
		Blocks.STAINED_HARDENED_CLAY,
		Blocks.SANDSTONE,
		Blocks.RED_SANDSTONE,
		Blocks.MYCELIUM,
		Blocks.SNOW_LAYER
	});
	
	public static final float 
	
		PI_OVER_2 = (float) (Math.PI / 2.0),
		PI_TIMES_2 = (float) (Math.PI * 2.0);
}
