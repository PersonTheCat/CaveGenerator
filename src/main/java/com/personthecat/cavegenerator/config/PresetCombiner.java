package com.personthecat.cavegenerator.config;

import static com.personthecat.cavegenerator.Main.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraftforge.fml.common.Loader;

public class PresetCombiner
{
	private static File configFile = new File(Loader.instance().getConfigDir() + "/cavegenerator.cfg");
	
	private static String presetPath;
		
	public static void init()
	{
		presetPath = Loader.instance().getConfigDir().getPath() + "/cavegenerator/presets";
		
		new File(presetPath).mkdirs();
		
		for (String combiner : ConfigFile.presetCombiners)
		{			
			combine(combiner);
			removeEntry(combiner);
		}
	}
	
	private static JsonObject loadPreset(String file)
	{
		JsonObject obj = null;
		
		File preset = new File(file);
		
		JsonParser parser = new JsonParser();
		
		try
		{
			obj = parser.parse(new FileReader(preset)).getAsJsonObject();
		}
		catch (FileNotFoundException e) 
		{
			throw new RuntimeException("Error: Could not find or load file: " + preset);
		}
		
		return obj;
	}
	
	/*
	 * To-do: Break this up for readability.
	 */
	private static void combine(String configEntry)
	{
		String adjusted = configEntry.replaceAll(".json", "");
		
		String[] adjustedSplit = adjusted.split(",");
		
		if (StringUtils.isEmpty(configEntry))
		{
			logger.info("Entry: " + configEntry + " is invalid. Skipping...");
			
			return;
		}
		
		String from = adjustedSplit[0].trim();
		String to = adjustedSplit[1].trim();
		
		String[] fromSplit = from.split("\\.");
		
		JsonObject[] fromObjs = new JsonObject[fromSplit.length];
		JsonObject[] toObjs = new JsonObject[fromSplit.length];
		
		String fromPath = presetPath + "/" + fromSplit[0] + ".json";
		String toPath = presetPath + "/" + to + ".json";
		
		backupFile(toPath);
		
		fromObjs[0] = loadPreset(fromPath);
		toObjs[0] = loadPreset(toPath);
		
		for (int i = 1; i < fromSplit.length; i++)
		{
			JsonObject fromParent = fromObjs[i - 1];
			JsonObject toParent = toObjs[i - 1];
			
			String name = fromSplit[i];
			
			if (!fromParent.has(name))
			{
				throw new RuntimeException("Error: The original json does not contain \"" + name + ".\" Please enter a valid entry to copy.");
			}
			
			if (toParent.has("keys"))
			{
				JsonArray keys = toParent.getAsJsonArray("keys");

				if (!arrayHasKey(keys, name))
				{
					keys.add(name);
					
					toParent.remove("keys");
					toParent.add("keys", keys);
				}
			}
			
			JsonElement fromEntry = fromParent.get(name);
			
			if (!fromEntry.isJsonObject() || i == fromSplit.length - 1)
			{
				if (toParent.has(name))
				{
					toParent.remove(name);
				}
				
				toParent.add(name, fromEntry);
			}
			else
			{
				fromObjs[i] = fromEntry.getAsJsonObject();
				
				if (toParent.has(name))
				{
					JsonElement toEntry = toParent.get(name);
					
					if (toEntry.isJsonObject())
					{
						toObjs[i] = toEntry.getAsJsonObject();
					}
					
					else throw new RuntimeException("Error: Json type mismatch. Please make sure both jsons are formatted accordingly.");	
				}
				else
				{
					toParent.add(name, fromEntry);
					
					break;
				}
			}
		}
		
		writeToFile(new File(toPath), formatJson(toObjs[0].toString()));
	}
	
	private static boolean arrayHasKey(JsonArray array, String key)
	{
		for (JsonElement inArray : array)
		{
			if (inArray.getAsString().equals(key))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static String formatJson(String json)
	{
		JsonParser parser = new JsonParser();
		JsonObject obj = parser.parse(json).getAsJsonObject();
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		return applyChangesToPrettyPrinting(gson.toJson(obj));
	}
	
	private static String applyChangesToPrettyPrinting(String json)
	{
		String properSeparator = System.getProperty("line.separator");

		json = json.replace("\n", properSeparator);
		json = json.replaceAll("  ", "\t");
		
		String[] lines = json.split(properSeparator);
		
		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i];
			
			if (line.endsWith("{"))
			{
				lines[i] = shiftToNextLine(line, "{");
			}
			else if (line.endsWith("[") && ! line.contains("]"))
			{
				lines[i] = shiftToNextLine(line, "[");
			}
		}
		
		StringBuilder finalJson = new StringBuilder();
		
		for (String line : lines)
		{
			//Deliberately remove these lines to avoid confusion.
			if (!line.trim().startsWith("\"//\""))
			{
				finalJson.append(line);
				finalJson.append(properSeparator);
			}
		}
		
		return finalJson.toString();
	}
	
	private static String shiftToNextLine(String line, String character)
	{
		String properSeparator = System.getProperty("line.separator");
		
		StringBuilder replacement = new StringBuilder(properSeparator);

		for (int j = 0; j < getNumTabs(line); j++)
		{
			replacement.append("\t");
		}
		
		replacement.append(character);
		
		return line.replace(character, replacement);
	}
	
	private static int getNumTabs(String line)
	{
		int num = 0;
		
		for (char ch : line.toCharArray())
		{
			if (Character.toString(ch).equals("\t"))
			{
				num++;
			}
			
			else break;
		}
		
		return num;
	}
	
	private static void backupFile(String path)
	{
		File original = new File(path);
		File backup = new File(path + ".bak");
		
		if (!backup.exists()) //Only store one backup.
		{
			try
			{
				Files.copy(original, backup);
			}
			catch (IOException e) { logger.warn("Error: Could not backup preset file."); }
		}
	}
	
	/*
	 * To-do: Stop writing the entire file at once?
	 */
	private static void writeToFile(File file, String line)
	{
		try
		{
			FileWriter writer = new FileWriter(file);
			
			writer.write(line);				
			
			writer.close();
		}
		
		catch (IOException e) { logger.warn("Could not write new file"); }
	}
	
	private static String getPreviousContents(File file)
	{
		try
		{
			return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			logger.warn("Error: Could not load previous config file.");
			
			return null;
		}
	}

	/**
	 * Working around a Forge bug where comments don't get retrieved correctly.
	 */
	private static void removeEntry(String entry)
	{
		String originalContents = getPreviousContents(configFile);
		
		if (originalContents != null)
		{
			String properSeparator = System.getProperty("line.separator");
			
			String[] lines = originalContents.split(properSeparator);
			
			StringBuilder newContents = new StringBuilder();
			
			for (int i = 0; i < lines.length; i++)
			{
				String line = lines[i];
				String trim = line.trim();
				
				if (!trim.equals(entry))
				{
					newContents.append(line);
					newContents.append(properSeparator);
				}
				else if (trim.startsWith("S:") && trim.contains("<") && trim.endsWith(entry))
				{
					newContents.append(line.substring(0, line.length() - entry.length()));
					newContents.append(properSeparator);
				}
				else if (trim.contains(entry) && trim.endsWith(">"))
				{
					newContents.append(line.substring(entry.length() - 1));
					newContents.append(properSeparator);
				}
			}
			
			writeToFile(configFile, newContents.toString());
		}
	}
}