package com.personthecat.cavegenerator.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.personthecat.cavegenerator.util.CommonMethods;
import com.personthecat.cavegenerator.util.Direction;
import com.personthecat.cavegenerator.world.BlockFiller;
import com.personthecat.cavegenerator.world.BlockFiller.Preference;
import com.personthecat.cavegenerator.world.CaveGenerator;
import com.personthecat.cavegenerator.world.CaveGenerator.Extension;
import com.personthecat.cavegenerator.world.StoneReplacer.StoneCluster;
import com.personthecat.cavegenerator.world.StoneReplacer.StoneLayer;
import com.personthecat.cavegenerator.world.feature.GiantPillar;
import com.personthecat.cavegenerator.world.feature.LargeStalactite;
import com.personthecat.cavegenerator.world.feature.StructureSpawnInfo;

import static com.personthecat.cavegenerator.Main.logger;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
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
		addGiantPillars();
		addStructures();
		addLargeStalagmitesAndStalactites();
		addFinalHeights();
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
		List<StoneCluster> clusters = new ArrayList<>();
		
		if (json.has("stoneClusters"))
		{
			JsonObject clusterContainer = json.get("stoneClusters").getAsJsonObject();
			
			if (clusterContainer.has("keys"))
			{
				for (JsonElement key : clusterContainer.getAsJsonArray("keys"))
				{
					JsonObject cluster = null;
					
					try
					{
						cluster = clusterContainer.get(key.getAsString()).getAsJsonObject();
					}
					
					catch (NullPointerException e)
					{
						throw new RuntimeException(
							"Error: Key \"" + key.getAsString() + "\" does not have an equivalent cluster object. "
						  + "Make sure it is typed correctly.");
					}
					
					if (!cluster.has("state") || !cluster.has("radius") ||
						!cluster.has("radiusVariance") || !cluster.has("startingHeight") ||
						!cluster.has("heightVariance") || !cluster.has("frequency"))
					{
						throw new RuntimeException("Error: You must specify a state, radius, radiusVariance, startingHeight, heightVariance, and frequency for each stone cluster.");
					}
					
					IBlockState state = CommonMethods.getBlockState(cluster.get("state").getAsString());
					int radius = cluster.get("radius").getAsInt();
					int radiusVariance = cluster.get("radiusVariance").getAsInt();
					int startingHeight = cluster.get("startingHeight").getAsInt();
					int heightVariance = cluster.get("heightVariance").getAsInt();
					double selectionThreshold = (1.0 - (cluster.get("frequency").getAsDouble())) * 92.0;
					
					clusters.add(new StoneCluster(state, radiusVariance, radius, startingHeight, heightVariance, selectionThreshold));
				}
			}
		}
		
		newGenerator.stoneClusters = clusters.toArray(new StoneCluster[0]);
	}

	private void addBlockFillers()
	{
		List<BlockFiller> fillersNormal = new ArrayList<>();
		List<BlockFiller> fillersUp = new ArrayList<>();
		List<BlockFiller> fillersDown = new ArrayList<>();
		List<BlockFiller> fillersSide = new ArrayList<>();
		
		if (json.has("blockFillers"))
		{
			JsonObject fillerContainer = json.get("blockFillers").getAsJsonObject();

			boolean fillersAreReplaceable = true;
			
			if (fillerContainer.has("fillersAreReplaceable"))
			{
				if (!fillerContainer.get("fillersAreReplaceable").getAsBoolean())
				{
					fillersAreReplaceable = false;
				}
			}
			
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
				
				if (fillerObject.has("matchers") ^ fillerObject.has("directions"))
				{
					throw new RuntimeException("Error: You must specify both a direction to test for and blocks to match when using either directions or matchers. See " + key.getAsString() + ".");
				}
				
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
				
				BlockFiller filler = new BlockFiller(state, chance, minHeight, maxHeight, matchers, directions, preference);
				
				if (fillerObject.has("spawnInPatches") && fillerObject.get("spawnInPatches").getAsBoolean())
				{
					filler.setSpawnInPatches();
				}
				if (fillerObject.has("patchSpacing")) filler.setPatchSpacing(fillerObject.get("patchSpacing").getAsInt());
				
				if (fillerObject.has("patchScale")) filler.setPatchThreshold((fillerObject.get("patchScale").getAsDouble() * 2.0) - 1.0);
				
//				if (fillerObject.has("caveSpecific") && fillerObject.get("caveSpecific").getAsBoolean())
//				{
//					boolean canSet = true;
//					
//					for (Direction dir : directions)
//					{
//						if (dir.equals(Direction.SIDE) || dir.equals(Direction.ALL))
//						{
//							logger.warn("Block fillers cannot be cave specific when using side matchers. Spawning " + key.getAsString() + " normally, instead.");
//							
//							canSet = false;
//						}
//					}
//					
//					if (canSet) filler.setCaveSpecific();
//				}
				
				if (fillersAreReplaceable) filler.registerAsReplaceable();
				
				if (directions.length == 0)
				{
					fillersNormal.add(filler);
				}
				else for (Direction dir : directions)
				{
					switch (dir)
					{
						case ALL:
							fillersUp.add(filler);
							fillersDown.add(filler);
							fillersSide.add(filler);
							break;
						case SIDE:
							fillersSide.add(filler);
							break;
						case UP:
							fillersUp.add(filler);
							break;
						case DOWN:
							fillersDown.add(filler);
							break;
						
						default: throw new IllegalStateException("Error: I'm really not sure how you got here... This direction... does not exist...");
					}
				}
			}
			
			newGenerator.fillBlocksNormal = fillersNormal.toArray(new BlockFiller[0]);
			newGenerator.fillBlocksUp = fillersUp.toArray(new BlockFiller[0]);
			newGenerator.fillBlocksDown = fillersDown.toArray(new BlockFiller[0]);
			newGenerator.fillBlocksSide = fillersSide.toArray(new BlockFiller[0]);
		}
	}
	
	/**
	 * To-do: Decide how necessary this function really is.
	 */
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
	
	private void addFinalHeights()
	{
		newGenerator.globalMinHeight = getMinNumber(newGenerator.minHeight, newGenerator.cavernMinHeight, newGenerator.rMinHeight);
		newGenerator.globalMaxHeight = getMaxNumber(newGenerator.maxHeight, newGenerator.cavernMaxHeight, newGenerator.rMaxHeight);
		
		List<Integer> noiseMins = new ArrayList<>();
		List<Integer> noiseMaxs = new ArrayList<>();
		
		if (newGenerator.cavernsEnabled)
		{
			noiseMins.add(newGenerator.cavernMinHeight);
			noiseMaxs.add(newGenerator.cavernMaxHeight);
		}
		
		if (newGenerator.stoneLayers.length > 0)
		{
			noiseMins.add(0);
			noiseMaxs.add(newGenerator.stoneLayers[newGenerator.stoneLayers.length - 1].getMaxHeight());
		}

		if (newGenerator.stoneClusters.length > 0)
		{
			int maxHeight = 0;
			
			for (StoneCluster cluster : newGenerator.stoneClusters)
			{
				int height = cluster.getStartingHeight() + cluster.getHeightVariance() + cluster.getRadius() + cluster.getRadiusVariance();
				
				if (height > maxHeight) maxHeight = height;
			}
			
			int minHeight = 0;
			
			for (StoneCluster cluster : newGenerator.stoneClusters)
			{
				int height = cluster.getStartingHeight() - cluster.getHeightVariance() - cluster.getRadius() - cluster.getRadiusVariance();
				
				if (height < minHeight) minHeight = height;
			}
			
			if (minHeight < 0) minHeight = 0;
			
			noiseMins.add(minHeight);
			noiseMaxs.add(maxHeight);
		}
		
		if (noiseMins.size() > 0)
		{
			newGenerator.noiseMinHeight = getMinNumber(noiseMins.stream().mapToInt(i -> i).toArray());
		}
		
		if (noiseMaxs.size() > 0)
		{
			newGenerator.noiseMaxHeight = getMaxNumber(noiseMaxs.stream().mapToInt(i -> i).toArray());
		}
	}
	
	private int getMinNumber(int... nums)
	{
		int minNumber = nums[0];
		
		for (int i = 0; i < nums.length; i++)
		{
			int number = nums[i];
			
			if (number < minNumber)
			{
				minNumber = number;
			}
		}
		
		return minNumber;
	}
	
	private int getMaxNumber(int... nums)
	{
		int maxNumber = 0;
		
		for (int i = 0; i < nums.length; i++)
		{
			int number = nums[i];
			
			if (number > maxNumber)
			{
				maxNumber = number;
			}
		}
		
		return maxNumber;
	}
	
	private void addGiantPillars()
	{
		List<GiantPillar> finalPillars = new ArrayList<>();
		
		if (json.has("giantPillars"))
		{
			JsonObject pillars = json.get("giantPillars").getAsJsonObject();
			
			if (pillars.has("keys"))
			{
				for (JsonElement key : pillars.get("keys").getAsJsonArray())
				{
					JsonObject pillar = null;
					
					try
					{
						pillar = pillars.get(key.getAsString()).getAsJsonObject();
					}
					catch (NullPointerException e)
					{
						throw new RuntimeException(
								"Error: Key \"" + key.getAsString() + "\" does not have an equivalent pillar object. "
							  + "Make sure it is typed correctly.");
					}
					
					if (!pillar.has("pillarState"))
					{
						throw new RuntimeException("Error: You must specify a pillarState for each pillar. Only the other parameters have default values.");
					}
					
					IBlockState pillarState = CommonMethods.getBlockState(pillar.get("pillarState").getAsString());
					
					Block stairBlock = null;
					
					if (pillar.has("stairBlock")) stairBlock = CommonMethods.getBlockState(pillar.get("stairBlock").getAsString()).getBlock();
					
					int frequency = 15;
					
					if (pillar.has("frequency")) frequency = pillar.get("frequency").getAsInt();
					
					int minHeight = 10;
					
					if (pillar.has("minHeight")) minHeight = pillar.get("minHeight").getAsInt();
					
					int maxHeight = 50;
					
					if (pillar.has("maxHeight")) maxHeight = pillar.get("maxHeight").getAsInt();
					
					int minLength = 5;
					
					if (pillar.has("minLength")) minLength = pillar.get("minLength").getAsInt();
					
					int maxLength = 12;
					
					if (pillar.has("maxLength")) maxLength = pillar.get("maxLength").getAsInt();
					
					finalPillars.add(new GiantPillar(frequency, minHeight, maxHeight, minLength, maxLength, pillarState, stairBlock));
				}
			}
		}
		
		newGenerator.pillars = finalPillars.toArray(new GiantPillar[0]);
	}
	
	/**
	 * Super not finished. Sorry!
	 */
	private void addStructures()
	{
		List<StructureSpawnInfo> finalStructures = new ArrayList<>();
		
		if (json.has("structures"))
		{
			JsonObject structures = json.get("structures").getAsJsonObject();
			
			if (structures.has("keys"))
			{
				for (JsonElement key : structures.get("keys").getAsJsonArray())
				{
					JsonObject structure = null;
					
					try
					{
						structure = structures.get(key.getAsString()).getAsJsonObject();
					}
					catch (NullPointerException e)
					{
						throw new RuntimeException(
								"Error: Key \"" + key.getAsString() + "\" does not have an equivalent structure object. "
							  + "Make sure it is typed correctly.");
					}
					
					if (!structure.has("name"))
					{
						throw new RuntimeException(
								"Error: You must specify a name for each structure. "
							  + "This can be a registry name or the name of a file under /cavegenerator/structures/.");
					}
					
					String name = structure.get("name").getAsString();
					
					PlacementSettings settings = new PlacementSettings();
					settings.setReplacedBlock(Blocks.STONE);
					
					if (structure.has("integrity")) settings.setIntegrity(structure.get("integrity").getAsFloat());
					
					BlockPos offset = new BlockPos(0, 0, 0);
					
					if (structure.has("offset"))
					{
						JsonArray offsets = structure.get("offset").getAsJsonArray();
						
						if (offsets.size() != 3)
						{
							throw new RuntimeException("Error: \"offset\" must contain exactly three elements (x, y, z).");
						}
						
						offset = offset.add(offsets.get(0).getAsInt(), offsets.get(1).getAsInt(), offsets.get(2).getAsInt());
					}
					else
					{
						if (structure.has("offsetX"))
						{
							offset = offset.add(structure.get("offsetX").getAsInt(), 0, 0);
						}
						if (structure.has("offsetY"))
						{
							offset = offset.add(0, structure.get("offsetY").getAsInt(), 0);
						}
						if (structure.has("offsetZ"))
						{
							offset = offset.add(0, 0, structure.get("offsetZ").getAsInt());
						}
					}

					IBlockState[] matchers = new IBlockState[] {Blocks.STONE.getDefaultState()};
					
					if (structure.has("matchers"))
					{
						JsonArray matcherArray = structure.getAsJsonArray("matchers");
						
						matchers = new IBlockState[matcherArray.size()];
						
						for (int i = 0; i < matcherArray.size(); i++)
						{
							matchers[i] = CommonMethods.getBlockState(matcherArray.get(i).getAsString());
						}
					}
					
					Direction[] directions = new Direction[0];
					
					if (structure.has("directions"))
					{
						JsonArray directionArray = structure.getAsJsonArray("directions");
						
						directions = new Direction[directionArray.size()];
						
						for (int i = 0; i < directionArray.size(); i++)
						{
							directions[i] = Direction.fromString(directionArray.get(i).getAsString());
						}
					}
					
					double minBurialPercentage = 0.0;
					
					if (structure.has("minBurialPercentage")) minBurialPercentage = structure.get("minBurialPercentage").getAsDouble();

					int frequency = 1;
					
					if (structure.has("frequency")) frequency = structure.get("frequency").getAsInt();
					
					double chance = 0.05;
					
					if (structure.has("chance")) chance = structure.get("chance").getAsDouble();
					
					int minHeight = 10;
					
					if (structure.has("minHeight")) minHeight = structure.get("minHeight").getAsInt();
					
					int maxHeight = 50;
					
					if (structure.has("maxHeight")) maxHeight = structure.get("maxHeight").getAsInt();
					
					StructureSpawnInfo newInfo = new StructureSpawnInfo(name, settings, offset, matchers, directions, minBurialPercentage, frequency, chance, minHeight, maxHeight);
					
					if (structure.has("airMatchers"))
					{
						JsonArray matcherArray = structure.get("airMatchers").getAsJsonArray();
						
						BlockPos[] positions = new BlockPos[matcherArray.size()];
						
						for (int i = 0; i < matcherArray.size(); i++)
						{
							JsonArray asArray = matcherArray.get(i).getAsJsonArray();
							
							if (asArray.size() != 3)
							{
								throw new RuntimeException(
										"Error: Each airMatcher must contain exactly three elements (x, y, z)."
									  + "These are relative coordinates to be tested for air in addition to the initially matched location.");
							}
							
							positions[i] = new BlockPos(asArray.get(0).getAsInt(), asArray.get(1).getAsInt(), asArray.get(2).getAsInt());
						}
						
						newInfo.setAdditionalAirMatchers(positions);
					}
					
					if (structure.has("solidMatchers"))
					{
						JsonArray matcherArray = structure.get("solidMatchers").getAsJsonArray();
						
						BlockPos[] positions = new BlockPos[matcherArray.size()];
						
						for (int i = 0; i < matcherArray.size(); i++)
						{
							JsonArray asArray = matcherArray.get(i).getAsJsonArray();
							
							if (asArray.size() != 3)
							{
								throw new RuntimeException(
										"Error: Each solidMatcher must contain exactly three elements (x, y, z)."
									  + "These are relative coordinates to be tested for solid blocks in addition to the initially matched location.");
							}
							
							positions[i] = new BlockPos(asArray.get(0).getAsInt(), asArray.get(1).getAsInt(), asArray.get(2).getAsInt());
						}
						
						newInfo.setAdditionalSolidMatchers(positions);
					}
					
					if (structure.has("debugSpawns") && structure.get("debugSpawns").getAsBoolean())
					{
						newInfo.setDebugSpawns();
					}
					
					if (structure.has("rotateRandomly") && structure.get("rotateRandomly").getAsBoolean())
					{
						newInfo.setRotateRandomly();
					}
					
					finalStructures.add(newInfo);
				}
			}
		}
		
		newGenerator.structures = finalStructures.toArray(new StructureSpawnInfo[0]);
	}
	
	/*
	 * To-do: Avoid repetition.
	 */
	private void addLargeStalagmitesAndStalactites()
	{
		List<LargeStalactite> finalStalactites = new ArrayList<>();
		
		if (json.has("largeStalagmites"))
		{
			JsonObject stalagmites = json.get("largeStalagmites").getAsJsonObject();
			
			if (stalagmites.has("keys"))
			{
				for (JsonElement key : stalagmites.get("keys").getAsJsonArray())
				{
					JsonObject stalagmite = null;
					
					try
					{
						stalagmite = stalagmites.get(key.getAsString()).getAsJsonObject();
					}
					catch (NullPointerException e)
					{
						throw new RuntimeException(
							"Error: Key \"" + key.getAsString() + "\" does not have an equivalent stalagmite object. "
						  + "Make sure it is typed correctly.");
					}
					
					if (!stalagmite.has("state"))
					{
						throw new RuntimeException("Error: You must specify a state for each large stalagmite. Only the other parameters have default values.");
					}
					
					IBlockState state = CommonMethods.getBlockState(stalagmite.get("state").getAsString());
					
					int maxLength = 3;
					
					if (stalagmite.has("maxLength")) maxLength = stalagmite.get("maxLength").getAsInt();
					
					double chance = 16.7;
					
					if (stalagmite.has("chance")) chance = stalagmite.get("chance").getAsDouble();
					
					else if (stalagmite.has("probability")) chance = stalagmite.get("probability").getAsDouble();
					
					int minHeight = 11;
					
					if (stalagmite.has("minHeight")) minHeight = stalagmite.get("minHeight").getAsInt();
					
					int maxHeight = 55;
					
					if (stalagmite.has("maxHeight")) maxHeight = stalagmite.get("maxHeight").getAsInt();
					
					LargeStalactite newStalagmite = new LargeStalactite(maxLength, chance, state, minHeight, maxHeight, LargeStalactite.Type.STALAGMITE);
					
					if (stalagmite.has("spawnInPatches") && stalagmite.get("spawnInPatches").getAsBoolean()) newStalagmite.setSpawnInPatches();
					
					if (stalagmite.has("patchSpacing")) newStalagmite.setPatchSpacing(stalagmite.get("patchSpacing").getAsInt());
					
					if (stalagmite.has("patchScale")) newStalagmite.setPatchThreshold((stalagmite.get("patchScale").getAsDouble() * -2.0) + 1.0);
					
					if (stalagmite.has("matchers"))
					{
						JsonArray matcherArray = stalagmite.getAsJsonArray("matchers");
						
						IBlockState[] matchers = new IBlockState[matcherArray.size()];
						
						for (int i = 0; i < matcherArray.size(); i++)
						{
							matchers[i] = CommonMethods.getBlockState(matcherArray.get(i).getAsString());
						}
						
						newStalagmite.setMatchers(matchers);
					}
					
					finalStalactites.add(newStalagmite);
				}
			}
		}
		
		if (json.has("largeStalactites"))
		{
			JsonObject stalactites = json.get("largeStalactites").getAsJsonObject();
			
			if (stalactites.has("keys"))
			{
				for (JsonElement key : stalactites.get("keys").getAsJsonArray())
				{
					JsonObject stalactite = null;
					
					try
					{
						stalactite = stalactites.get(key.getAsString()).getAsJsonObject();
					}
					catch (NullPointerException e)
					{
						throw new RuntimeException(
							"Error: Key \"" + key.getAsString() + "\" does not have an equivalent stalagmite object. "
						  + "Make sure it is typed correctly.");
					}
					
					if (!stalactite.has("state"))
					{
						throw new RuntimeException("Error: You must specify a state for each large stalactite. Only the other parameters have default values.");
					}
					
					IBlockState state = CommonMethods.getBlockState(stalactite.get("state").getAsString());
					
					int maxLength = 3;
					
					if (stalactite.has("maxLength")) maxLength = stalactite.get("maxLength").getAsInt();
					
					double chance = 16.7;
					
					if (stalactite.has("chance")) chance = stalactite.get("chance").getAsDouble();
					
					else if (stalactite.has("probability")) chance = stalactite.get("probability").getAsDouble();
					
					int minHeight = 11;
					
					if (stalactite.has("minHeight")) minHeight = stalactite.get("minHeight").getAsInt();
					
					int maxHeight = 55;
					
					if (stalactite.has("maxHeight")) maxHeight = stalactite.get("maxHeight").getAsInt();
					
					LargeStalactite newStalactite = new LargeStalactite(maxLength, chance, state, minHeight, maxHeight, LargeStalactite.Type.STALACTITE);
					
					if (stalactite.has("spawnInPatches") && stalactite.get("spawnInPatches").getAsBoolean()) newStalactite.setSpawnInPatches();
					
					if (stalactite.has("patchSpacing")) newStalactite.setPatchSpacing(stalactite.get("patchSpacing").getAsInt());
					
					if (stalactite.has("patchScale")) newStalactite.setPatchThreshold((stalactite.get("patchScale").getAsDouble() * -2.0) + 1.0);
					
					if (stalactite.has("matchers"))
					{
						JsonArray matcherArray = stalactite.getAsJsonArray("matchers");
						
						IBlockState[] matchers = new IBlockState[matcherArray.size()];
						
						for (int i = 0; i < matcherArray.size(); i++)
						{
							matchers[i] = CommonMethods.getBlockState(matcherArray.get(i).getAsString());
						}
						
						newStalactite.setMatchers(matchers);
					}
					
					finalStalactites.add(newStalactite);
				}
			}
		}
		
		if (finalStalactites.size() > 0)
		{
			newGenerator.stalactites = finalStalactites.toArray(new LargeStalactite[0]);
		}
	}
}