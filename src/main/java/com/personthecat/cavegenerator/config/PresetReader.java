package com.personthecat.cavegenerator.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.world.BlockFiller;
import com.personthecat.cavegenerator.world.BlockFiller.Direction;
import com.personthecat.cavegenerator.world.BlockFiller.Preference;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.CaveGenerator.Extension;
import com.personthecat.cavegenerator.world.StoneReplacer.StoneLayer;

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
		addGlobalValues();
		addRooms();
		addTunnels();
		addCaverns();
		addRavines();
		addLavaRules();
		addBiomes();
		addStoneLayers();
		addStoneClusters();
		addBlockFillers();
	}
	
	private void addGlobalValues()
	{
		if (json.has("enabled")) newGenerator.enabledGlobally = json.get("enabled").getAsBoolean();
		
		if (json.has("dimensions"))
		{
			JsonArray dimArray = json.get("dimensions").getAsJsonArray();
			
			List<Integer> dimensions = new ArrayList<>();
			
			for (JsonElement element : dimArray)
			{
				dimensions.add(element.getAsInt());
			}
			
			newGenerator.dimensions = dimensions.stream().mapToInt(i -> i).toArray();
		}
		
		if (json.has("useDimensionBlacklist")) newGenerator.useDimensionBlacklist = json.get("useDimensionBlacklist").getAsBoolean();
	}
	
	private void addRooms()
	{
		if (json.has("rooms"))
		{
			JsonObject room = json.get("rooms").getAsJsonObject();
			
			if (room.has("scale")) newGenerator.roomScale = room.get("scale").getAsFloat();
			
			if (room.has("scaleY")) newGenerator.roomScaleY = room.get("scaleY").getAsFloat();
		}
	}
	
	private void addTunnels()
	{
		if (json.has("tunnels"))
		{
			JsonObject tunnels = json.get("tunnels").getAsJsonObject();
			
			if (tunnels.has("twistXZ"))
			{
				JsonObject twistXZ = tunnels.get("twistXZ").getAsJsonObject();
				
				if (twistXZ.has("exponent")) newGenerator.twistXZExponent = twistXZ.get("exponent").getAsFloat();
				
				if (twistXZ.has("factor")) newGenerator.twistXZFactor = twistXZ.get("factor").getAsFloat();
				
				if (twistXZ.has("randomnessFactor")) newGenerator.twistXZRandFactor = twistXZ.get("randomnessFactor").getAsFloat();
				
				if (twistXZ.has("startingValue")) newGenerator.startingTwistXZ = twistXZ.get("startingValue").getAsFloat();
			}
			
			if (tunnels.has("twistY"))
			{
				JsonObject twistY = tunnels.get("twistY").getAsJsonObject();
				
				if (twistY.has("exponent")) newGenerator.twistYExponent = twistY.get("exponent").getAsFloat();
				
				if (twistY.has("factor")) newGenerator.twistYFactor = twistY.get("factor").getAsFloat();
				
				if (twistY.has("randomnessFactor")) newGenerator.twistYRandFactor = twistY.get("randomnessFactor").getAsFloat();
				
				if (twistY.has("startingValue")) newGenerator.startingTwistY = twistY.get("startingValue").getAsFloat();
			}
			
			if (tunnels.has("scale"))
			{
				JsonObject scale = tunnels.get("scale").getAsJsonObject();
				
				if (scale.has("exponent")) newGenerator.scaleExponent = scale.get("exponent").getAsFloat();
				
				if (scale.has("factor")) newGenerator.scaleFactor = scale.get("factor").getAsFloat();
				
				if (scale.has("randomnessFactor")) newGenerator.scaleRandFactor = scale.get("randomnessFactor").getAsFloat();
				
				if (scale.has("startingValue")) newGenerator.startingScale = scale.get("startingValue").getAsFloat();
				
				if (scale.has("startingValueRandomnessFactor")) newGenerator.startingScaleRandFactor = scale.get("startingValueRandomnessFactor").getAsFloat();
			}
			
			if (tunnels.has("scaleY"))
			{
				JsonObject scaleY = tunnels.get("scaleY").getAsJsonObject();
				
				if (scaleY.has("exponent")) newGenerator.scaleYExponent = scaleY.get("exponent").getAsFloat();
				
				if (scaleY.has("factor")) newGenerator.scaleYFactor = scaleY.get("factor").getAsFloat();
				
				if (scaleY.has("randomnessFactor")) newGenerator.scaleYRandFactor = scaleY.get("randomnessFactor").getAsFloat();
				
				if (scaleY.has("startingValue")) newGenerator.startingScaleY = scaleY.get("startingValue").getAsFloat();
			}

			if (tunnels.has("slopeXZ"))
			{
				JsonObject slopeXZ = tunnels.get("slopeXZ").getAsJsonObject();
				
				if (slopeXZ.has("startingValue")) newGenerator.startingSlopeXZ = slopeXZ.get("startingValue").getAsFloat();
				
				if (slopeXZ.has("startingValueRandomnessFactor")) newGenerator.startingSlopeXZRandFactor = slopeXZ.get("startingValueRandomnessFactor").getAsFloat();
			}
			
			if (tunnels.has("slopeY"))
			{
				JsonObject slopeY = tunnels.get("slopeY").getAsJsonObject();
				
				if (slopeY.has("startingValue")) newGenerator.startingSlopeY = slopeY.get("startingValue").getAsFloat();
				
				if (slopeY.has("startingValueRandomnessFactor")) newGenerator.startingSlopeYRandFactor = slopeY.get("startingValueRandomnessFactor").getAsFloat();
			}
			
			if (tunnels.has("frequency")) newGenerator.tunnelFrequency = tunnels.get("frequency").getAsInt();
			
			if (tunnels.has("distance")) newGenerator.startingDistance = tunnels.get("distance").getAsInt();
			
			if (tunnels.has("minHeight")) newGenerator.minHeight = tunnels.get("minHeight").getAsInt();
			
			if (tunnels.has("maxHeight")) newGenerator.maxHeight = tunnels.get("maxHeight").getAsInt();
			
			if (tunnels.has("spawnInSystemInverseChance")) newGenerator.spawnInSystemInverseChance = tunnels.get("spawnInSystemInverseChance").getAsInt();
			
			if (tunnels.has("spawnIsolatedInverseChance")) newGenerator.spawnIsolatedInverseChance = tunnels.get("spawnIsolatedInverseChance").getAsInt();
			
			if (tunnels.has("vanillaNoiseYReduction")) newGenerator.noiseYReduction = tunnels.get("vanillaNoiseYReduction").getAsBoolean();
			
			addExtensions(tunnels);
		}
	}
	
	private void addRavines()
	{
		if (json.has("ravines"))
		{
			JsonObject ravines = json.get("ravines").getAsJsonObject();
			
			if (ravines.has("twistXZ"))
			{
				JsonObject twistXZ = ravines.get("twistXZ").getAsJsonObject();
				
				if (twistXZ.has("exponent")) newGenerator.rTwistXZExponent = twistXZ.get("exponent").getAsFloat();
				
				if (twistXZ.has("factor")) newGenerator.rTwistXZFactor = twistXZ.get("factor").getAsFloat();
				
				if (twistXZ.has("randomnessFactor")) newGenerator.rTwistXZRandFactor = twistXZ.get("randomnessFactor").getAsFloat();
				
				if (twistXZ.has("startingValue")) newGenerator.rStartingTwistXZ = twistXZ.get("startingValue").getAsFloat();
			}
			
			if (ravines.has("twistY"))
			{
				JsonObject twistY = ravines.get("twistY").getAsJsonObject();
				
				if (twistY.has("exponent")) newGenerator.rTwistYExponent = twistY.get("exponent").getAsFloat();
				
				if (twistY.has("factor")) newGenerator.rTwistYFactor = twistY.get("factor").getAsFloat();
				
				if (twistY.has("randomnessFactor")) newGenerator.rTwistYRandFactor = twistY.get("randomnessFactor").getAsFloat();
				
				if (twistY.has("startingValue")) newGenerator.rStartingTwistY = twistY.get("startingValue").getAsFloat();
			}
			
			if (ravines.has("scale"))
			{
				JsonObject scale = ravines.get("scale").getAsJsonObject();
				
				if (scale.has("exponent")) newGenerator.rScaleExponent = scale.get("exponent").getAsFloat();
				
				if (scale.has("factor")) newGenerator.rScaleFactor = scale.get("factor").getAsFloat();
				
				if (scale.has("randomnessFactor")) newGenerator.rScaleRandFactor = scale.get("randomnessFactor").getAsFloat();
				
				if (scale.has("startingValue")) newGenerator.rStartingScale = scale.get("startingValue").getAsFloat();
				
				if (scale.has("startingValueRandomnessFactor")) newGenerator.rStartingScaleRandFactor = scale.get("startingValueRandomnessFactor").getAsFloat();
			}
			
			if (ravines.has("scaleY"))
			{
				JsonObject scaleY = ravines.get("scaleY").getAsJsonObject();
				
				if (scaleY.has("exponent")) newGenerator.rScaleYExponent = scaleY.get("exponent").getAsFloat();
				
				if (scaleY.has("factor")) newGenerator.rScaleYFactor = scaleY.get("factor").getAsFloat();
				
				if (scaleY.has("randomnessFactor")) newGenerator.rScaleYRandFactor = scaleY.get("randomnessFactor").getAsFloat();
				
				if (scaleY.has("startingValue")) newGenerator.rStartingScaleY = scaleY.get("startingValue").getAsFloat();
			}

			if (ravines.has("slopeXZ"))
			{
				JsonObject slopeXZ = ravines.get("slopeXZ").getAsJsonObject();
				
				if (slopeXZ.has("startingValue")) newGenerator.rStartingSlopeXZ = slopeXZ.get("startingValue").getAsFloat();
				
				if (slopeXZ.has("startingValueRandomnessFactor")) newGenerator.rStartingSlopeXZRandFactor = slopeXZ.get("startingValueRandomnessFactor").getAsFloat();
			}
			
			if (ravines.has("slopeY"))
			{
				JsonObject slopeY = ravines.get("slopeY").getAsJsonObject();
				
				if (slopeY.has("startingValue")) newGenerator.rStartingSlopeY = slopeY.get("startingValue").getAsFloat();
				
				if (slopeY.has("startingValueRandomnessFactor")) newGenerator.rStartingSlopeYRandFactor = slopeY.get("startingValueRandomnessFactor").getAsFloat();
			}
			
			if (ravines.has("inverseChance")) newGenerator.rInverseChance = ravines.get("inverseChance").getAsInt();
			
			if (ravines.has("distance")) newGenerator.rStartingDistance = ravines.get("distance").getAsInt();
			
			if (ravines.has("minHeight")) newGenerator.rMinHeight = ravines.get("minHeight").getAsInt();
			
			if (ravines.has("maxHeight")) newGenerator.rMaxHeight = ravines.get("maxHeight").getAsInt();
			
			if (ravines.has("noiseYFactor")) newGenerator.rNoiseYFactor = ravines.get("noiseYFactor").getAsFloat();
		}
	}
	
	private void addCaverns()
	{
		if (json.has("caverns"))
		{
			JsonObject caverns = json.get("caverns").getAsJsonObject();
			
			if (caverns.has("enabled")) newGenerator.cavernsEnabled = caverns.get("enabled").getAsBoolean();
			
			if (caverns.has("scale"))
			{
				float selectionThreshold = (caverns.get("scale").getAsFloat() * -2.0F) + 1.0F;
				
				newGenerator.cavernSelectionThreshold = selectionThreshold;
			}
			
			if (caverns.has("spacing")) newGenerator.cavernFrequency = caverns.get("spacing").getAsFloat();
			
			if (caverns.has("scaleY")) newGenerator.cavernScaleY = caverns.get("scaleY").getAsFloat();
			
			if (caverns.has("amplitude")) newGenerator.cavernAmplitude = caverns.get("amplitude").getAsFloat();
			
			if (caverns.has("minHeight")) newGenerator.cavernMinHeight = caverns.get("minHeight").getAsInt();
			
			if (caverns.has("maxHeight")) newGenerator.cavernMaxHeight = caverns.get("maxHeight").getAsInt();
			
			if (caverns.has("fastYSmoothing")) newGenerator.fastCavernYSmoothing = caverns.get("fastYSmoothing").getAsBoolean();
		}
	}
	
	private void addLavaRules()
	{		
		if (json.has("lavaRules"))
		{
			JsonObject lavaRules = json.get("lavaRules").getAsJsonObject();
			
			if (lavaRules.has("maxHeight")) newGenerator.lavaMaxHeight = lavaRules.get("maxHeight").getAsInt();
		}
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
		
		if (json.has("useBiomeBlacklist")) newGenerator.useBiomeBlacklist = json.get("useBiomeBlacklist").getAsBoolean();
	}
	
	private void addStoneLayers()
	{
		List<StoneLayer> layers = new ArrayList<>();
		
		if (json.has("stoneLayers"))
		{
			JsonObject layerContainer = json.get("stoneLayers").getAsJsonObject();
			
			if (layerContainer.has("keys"))
			{
				for (JsonElement key : layerContainer.getAsJsonArray("keys"))
				{
					JsonObject layer = null;
					
					try
					{
						layer = layerContainer.get(key.getAsString()).getAsJsonObject();
					}
					
					catch (NullPointerException e)
					{
						throw new RuntimeException(
							"Error: Key \"" + key.getAsString() + "\" does not have an equivalent layer object. "
						  + "Make sure it is typed correctly.");
					}
					
					if (!layer.has("maxHeight") && !layer.has("state"))
					{
						throw new RuntimeException("Error: You must specify a state and maxHeight for each stone layer.");
					}
					
					int maxHeight = layer.get("maxHeight").getAsInt();
					
					IBlockState state = CommonMethods.getBlockState(layer.get("state").getAsString());
					
					layers.add(new StoneLayer(maxHeight, state));
				}
			}
		}
		
		newGenerator.stoneLayers = layers.toArray(new StoneLayer[0]);
	}
	
	private void addStoneClusters()
	{
//		List<StoneCluster> clusters = new ArrayList<>();
//		
//		if (json.has("stoneClusters"))
//		{
//			JsonObject clusterContainer = json.get("stoneClusters").getAsJsonObject();
//			
//			if (clusterContainer.has("keys"))
//			{
//				for (JsonElement key : clusterContainer.getAsJsonArray("keys"))
//				{
//					JsonObject cluster = null;
//					
//					try
//					{
//						cluster = clusterContainer.get(key.getAsString()).getAsJsonObject();
//					}
//					
//					catch (NullPointerException e)
//					{
//						throw new RuntimeException(
//							"Error: Key \"" + key.getAsString() + "\" does not have an equivalent cluster object. "
//						  + "Make sure it is typed correctly.");
//					}
//					
//					if (!cluster.has("state"))
//					{
//						throw new RuntimeException("Error: You must specify a state for each stone layer.");
//					}
//					
//					IBlockState state = CommonMethods.getBlockState(cluster.get("state").getAsString());
//					
//					double spacing = cluster.has("spacing") ? cluster.get("spacing").getAsDouble() : 20.0;
//					
//					double selectionThreshold = cluster.has("scale") ? ((cluster.get("scale").getAsDouble()  * -2.0) + 1.0) : 0.4;
//					
//					clusters.add(new StoneCluster(state, spacing, selectionThreshold));					
//				}
//			}
//		}
//		
//		newGenerator.stoneClusters = clusters.toArray(new StoneCluster[0]);
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
					throw new RuntimeException("Error: You must specify a state, chance, minHeight, and maxHeight for each block filler.");
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
	
	private void addExtensions(JsonObject tunnelObject)
	{
		if (tunnelObject.has("extensions"))
		{
			JsonObject extensionContainer = tunnelObject.get("extensions").getAsJsonObject();
			
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
