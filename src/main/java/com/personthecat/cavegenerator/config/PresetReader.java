package com.personthecat.cavegenerator.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.personthecat.cavegenerator.BlockFiller;
import com.personthecat.cavegenerator.BlockFiller.Direction;
import com.personthecat.cavegenerator.BlockFiller.Preference;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.CaveGenerator.Extension;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class PresetReader
{
	CaveGenerator newGenerator;
	
	JsonObject json;
	
	public PresetReader(File preset)
	{
		newGenerator = new CaveGenerator();

		JsonParser parser = new JsonParser();
		
		try
		{
			json = parser.parse(new FileReader(preset)).getAsJsonObject();
		} 
		
		catch (FileNotFoundException e) 
		{
			throw new RuntimeException("Error: Could not find or load file: " + preset);
		}
		
		setupGenerator();
	}
	
	public CaveGenerator getGenerator()
	{
		return newGenerator;
	}
	
	private void setupGenerator()
	{
		addPrimaryValues();
		addBiomes();
		addBlockFillers();
		addExtensions();
	}
	
	private void addPrimaryValues()
	{
		addBooleans();
		addFloats();
		addInts();
	}

	private void addBooleans()
	{
		if (json.has("vanillaNoiseYReduction")) newGenerator.noiseYReduction = json.get("vanillaNoiseYReduction").getAsBoolean();
	}
	
	private void addFloats()
	{
		if (json.has("room"))
		{
			JsonObject room = json.get("room").getAsJsonObject();
			
			if (room.has("scale")) newGenerator.roomScale = room.get("scale").getAsFloat();
			
			if (room.has("scaleY")) newGenerator.roomScaleY = room.get("scaleY").getAsFloat();
		}
		
		if (json.has("twistXZ"))
		{
			JsonObject twistXZ = json.get("twistXZ").getAsJsonObject();
			
			if (twistXZ.has("exponent")) newGenerator.twistXZExponent = twistXZ.get("exponent").getAsFloat();
			
			if (twistXZ.has("factor")) newGenerator.twistXZFactor = twistXZ.get("factor").getAsFloat();
			
			if (twistXZ.has("randomnessFactor")) newGenerator.twistXZRandFactor = twistXZ.get("randomnessFactor").getAsFloat();
			
			if (twistXZ.has("startingValue")) newGenerator.startingTwistXZ = twistXZ.get("startingValue").getAsFloat();
		}
		
		if (json.has("twistY"))
		{
			JsonObject twistY = json.get("twistY").getAsJsonObject();
			
			if (twistY.has("exponent")) newGenerator.twistYExponent = twistY.get("exponent").getAsFloat();
			
			if (twistY.has("factor")) newGenerator.twistYFactor = twistY.get("factor").getAsFloat();
			
			if (twistY.has("randomnessFactor")) newGenerator.twistYRandFactor = twistY.get("randomnessFactor").getAsFloat();
			
			if (twistY.has("startingValue")) newGenerator.startingTwistY = twistY.get("startingValue").getAsFloat();
		}
		
		if (json.has("scale"))
		{
			JsonObject scale = json.get("scale").getAsJsonObject();
			
			if (scale.has("exponent")) newGenerator.scaleExponent = scale.get("exponent").getAsFloat();
			
			if (scale.has("factor")) newGenerator.scaleFactor = scale.get("factor").getAsFloat();
			
			if (scale.has("randomnessFactor")) newGenerator.scaleRandFactor = scale.get("randomnessFactor").getAsFloat();
			
			if (scale.has("startingValue")) newGenerator.startingScale = scale.get("startingValue").getAsFloat();
			
			if (scale.has("startingValueRandomnessFactor")) newGenerator.startingScaleRandFactor = scale.get("startingValueRandomnessFactor").getAsFloat();
		}
		
		if (json.has("scaleY"))
		{
			JsonObject scaleY = json.get("scaleY").getAsJsonObject();
			
			if (scaleY.has("exponent")) newGenerator.scaleYExponent = scaleY.get("exponent").getAsFloat();
			
			if (scaleY.has("factor")) newGenerator.scaleYFactor = scaleY.get("factor").getAsFloat();
			
			if (scaleY.has("randomnessFactor")) newGenerator.scaleYRandFactor = scaleY.get("randomnessFactor").getAsFloat();
			
			if (scaleY.has("startingValue")) newGenerator.startingScaleY = scaleY.get("startingValue").getAsFloat();
		}

		if (json.has("slopeXZ"))
		{
			JsonObject slopeXZ = json.get("slopeXZ").getAsJsonObject();
			
			if (slopeXZ.has("startingValue")) newGenerator.startingSlopeXZ = slopeXZ.get("startingValue").getAsFloat();
			
			if (slopeXZ.has("startingValueRandomnessFactor")) newGenerator.startingSlopeXZRandFactor = slopeXZ.get("startingValueRandomnessFactor").getAsFloat();
		}
		
		if (json.has("slopeY"))
		{
			JsonObject slopeY = json.get("slopeY").getAsJsonObject();
			
			if (slopeY.has("startingValue")) newGenerator.startingSlopeY = slopeY.get("startingValue").getAsFloat();
			
			if (slopeY.has("startingValueRandomnessFactor")) newGenerator.startingSlopeYRandFactor = slopeY.get("startingValueRandomnessFactor").getAsFloat();
		}
		
		if (json.has("chance")) newGenerator.generatorSelectionChance = json.get("chance").getAsFloat();
	}
	
	private void addInts()
	{		
		if (json.has("lavaRules"))
		{
			JsonObject lavaRules = json.get("lavaRules").getAsJsonObject();
			
			if (lavaRules.has("maxHeight")) newGenerator.lavaMaxHeight = lavaRules.get("maxHeight").getAsInt();
		}
		
		if (json.has("distance")) newGenerator.startingDistance = json.get("distance").getAsInt();
		
		if (json.has("spawnInSystemInverseChance")) newGenerator.spawnInSystemInverseChance = json.get("spawnInSystemInverseChance").getAsInt();
	}
	
	private void addBiomes()
	{
		List<Biome> biomes = new ArrayList<>();
		
		if (json.has("biomes"))
		{
			JsonObject biomeContainer = json.get("biomes").getAsJsonObject();

			for (JsonElement name : biomeContainer.get("names").getAsJsonArray())
			{
				biomes.add(CommonMethods.getBiome(name.getAsString()));
			}
			
			for (JsonElement ID : biomeContainer.get("IDs").getAsJsonArray())
			{
				biomes.add(CommonMethods.getBiome(ID.getAsInt()));
			}
			
			for (JsonElement type : biomeContainer.get("types").getAsJsonArray())
			{
				for (Biome biome : BiomeDictionary.getBiomes(Type.getType(type.getAsString())))
				{
					biomes.add(biome);
				}
			}
			
			newGenerator.biomes = biomes.toArray(new Biome[0]);
		}
	}

	private void addBlockFillers()
	{
		List<BlockFiller> fillers = new ArrayList<>();
		
		if (json.has("blockFillers"))
		{
			JsonObject fillerContainer = json.get("blockFillers").getAsJsonObject();

			for (JsonElement key : fillerContainer.getAsJsonArray("keys"))
			{
				JsonObject fillerObject = null;
				
				try
				{
					fillerObject = fillerContainer.get(key.getAsString()).getAsJsonObject();
				}
				
				catch (NullPointerException e)
				{
					throw new RuntimeException(
						"Error: Key \"" + key.getAsString() + "\" does not have an equivalent filler object. "
					  + "Make sure it is typed correctly.");
				}
				
				if (!fillerObject.has("state") || !fillerObject.has("chance") || !fillerObject.has("minHeight") || !fillerObject.has("maxHeight"))
				{
					System.err.println("Error: You must specify a state, chance, minHeight, and maxHeight for each block filler.");
				}
				
				IBlockState state = CommonMethods.getBlockState(fillerObject.get("state").getAsString());
				
				double chance = fillerObject.get("chance").getAsDouble();
				
				int minHeight = fillerObject.get("minHeight").getAsInt();
				
				int maxHeight = fillerObject.get("maxHeight").getAsInt();
				
				IBlockState[] matchers = new IBlockState[0];
				
				if (fillerObject.has("matchers"))
				{
					JsonArray matcherArray = fillerObject.getAsJsonArray("matchers");
					
					matchers = new IBlockState[matcherArray.size()];
					
					for (int i = 0; i < matcherArray.size(); i++)
					{
						matchers[i] = CommonMethods.getBlockState(matcherArray.get(i).getAsString());
					}
				}
				
				Direction[] directions = new Direction[0];
				
				if (fillerObject.has("directions"))
				{
					JsonArray directionArray = fillerObject.getAsJsonArray("directions");
					
					directions = new Direction[directionArray.size()];
					
					for (int i = 0; i < directionArray.size(); i++)
					{
						directions[i] = Direction.fromString(directionArray.get(i).getAsString());
					}
				}
				
				Preference preference = Preference.REPLACE_ORIGINAL;
				
				if (fillerObject.has("preference")) preference = Preference.fromString(fillerObject.get("preference").getAsString());
				
				fillers.add(new BlockFiller(state, chance, minHeight, maxHeight, matchers, directions, preference));
			}
			
			newGenerator.fillBlocks = fillers.toArray(new BlockFiller[0]);
		}
	}
	
	private void addExtensions()
	{
		if (json.has("extensions"))
		{
			JsonObject extensionContainer = json.get("extensions").getAsJsonObject();
			
			if (extensionContainer.has("beginning"))
			{
				String asString = extensionContainer.get("beginning").getAsString();
				
				String[] split = asString.split(":");
				
				if (split.length > 1)
				{
					if (split[0].equals("preset"))
					{
						newGenerator.extBeginningPreset = split[1];
					}
				}
				
				newGenerator.extBeginning = Extension.fromString(asString);
			}
			
			if (extensionContainer.has("random"))
			{
				String asString = extensionContainer.get("random").getAsString();
				
				String[] split = asString.split(":");
				
				if (split.length > 1)
				{
					if (split[0].equals("preset"))
					{
						newGenerator.extRandPreset = split[1];
					}
				}
				
				newGenerator.extRand = Extension.fromString(asString);
			}
			
			if (extensionContainer.has("end"))
			{
				String asString = extensionContainer.get("end").getAsString();
				
				String[] split = asString.split(":");
				
				if (split.length > 1)
				{
					if (split[0].equals("preset"))
					{
						newGenerator.extEndPreset = split[1];
					}
				}
				
				newGenerator.extEnd = Extension.fromString(asString);
			}
		}
	}
}
