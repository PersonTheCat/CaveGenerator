package com.personthecat.cavegenerator.world.anticascade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.personthecat.cavegenerator.Main;
import com.personthecat.cavegenerator.world.anticascade.CaveCompletion.ChunkCoordinates;
import com.personthecat.cavegenerator.world.anticascade.CaveCompletion.ChunkCorrections;

import net.minecraft.world.World;

public class CorrectionStorage implements Serializable
{
	private static final String SAVE_LOCATION = "/cavegenerator/new_chunk_info.dat";
	
	public static Map<Integer, List<ChunkCorrections>> worldCorrections = new HashMap<>();

	/**
	 * Searches through all currently-marked corrections to find one with
	 * matching coordinates. Returns a new object if one cannot be found. 
	 */
	public static ChunkCorrections getCorrectionsForChunk(int dimension, int chunkX, int chunkZ)
	{
		ChunkCoordinates coordinates = new ChunkCoordinates(chunkX, chunkZ);		
		
		List<ChunkCorrections> corrections = worldCorrections.get(dimension);
		
		if (corrections == null) corrections = new ArrayList<>();
		
		for (ChunkCorrections correction : corrections)
		{
			ChunkCoordinates atLocation = correction.getCoordinates();
			
			if (coordinates.equals(atLocation))
			{
				return correction;
			}
		}
		
		ChunkCorrections newCorrectionTable = new ChunkCorrections(coordinates);
		
		corrections.add(newCorrectionTable);
		
		worldCorrections.put(dimension, corrections);
		
		return newCorrectionTable;
	}
	
	private static String loadedWorldDir;
	
	public static void recordCorrections()
	{
		if (!loadedWorldDir.equals(""))
		{
			File data = new File(loadedWorldDir + SAVE_LOCATION);
			
			try
			{
				if (!data.exists())
				{
					data.getParentFile().mkdirs();
					data.createNewFile();
				}
				
				FileOutputStream file = new FileOutputStream(data);
				ObjectOutputStream objectOut = new ObjectOutputStream(file);
				
				objectOut.writeObject(worldCorrections);
				
				objectOut.close();
			}
			
			catch (IOException e) { e.printStackTrace(); }
		}
		
		else System.err.println("Error: Could not record additional decorations. Please report this issue.");
	}
	
	public static void removeCorrectionsFromWorld(int dimension, ChunkCorrections corrections)
	{
		List<ChunkCorrections> list = worldCorrections.get(dimension);
		
		if (list != null) list.remove(corrections);
	}
	
	/**
	 * Loads previous corrections from world save. No need to do anything
	 * if no corrections exist.
	 */
	public static void setCorrectionsFromSave(String saveFolder)
	{
		try
		{
			loadedWorldDir = Main.proxy.getSavesDirectory() + "/" + saveFolder;
			
			new File(loadedWorldDir).mkdir();

			try
			{
				FileInputStream file = new FileInputStream(loadedWorldDir + SAVE_LOCATION);
				ObjectInputStream objectIn = new ObjectInputStream(file);
				
				worldCorrections = (HashMap<Integer, List<ChunkCorrections>>) objectIn.readObject();
				
				objectIn.close();
				
				System.out.println("Successfully set corrections!");
			}
			
			catch (FileNotFoundException ignored) {}
			
			catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
		}

		catch (NullPointerException ignored) {/*World was just created. Ignore.*/}
	}
}
