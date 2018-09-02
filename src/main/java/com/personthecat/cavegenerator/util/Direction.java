package com.personthecat.cavegenerator.util;

import java.util.Arrays;

public enum Direction
{
	UP,
	DOWN,
	SIDE,
	ALL;
	
	public static Direction fromString(String s)
	{
		for (Direction d : values())
		{
			if (d.toString().equalsIgnoreCase(s))
			{
				return d;
			}
		}
		
		throw new RuntimeException(
			"Error: Direction \"" + s + "\" does not exist."
		  + "The following are valid options:\n\n"
		  +  Arrays.toString(values()));
	}
}