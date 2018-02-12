package ca.polymtl.inf8480.tp1.shared;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;




public class CloudUtil{
	
	
	// retains data between sessions, mainly the association <string filename, int as string checksum>
	public java.util.Properties properties;
	// directory used to store data
	public String folder;
	// directory that stores the managed files
	public String propertiesPath;
	// directory that stores the managed files
	public String storage;
	
	public CloudUtil(String folder_){
		
		//set our paths
		folder = folder_;
		propertiesPath = folder + "properties.txt";
		storage = folder + "cloud_storage/";
		
		// load properties like clientID/number of slients, file checksums, ..
		try{
			properties = new Properties();
			properties.load(new FileInputStream(propertiesPath));
		} catch ( IOException e) {
			System.err.println("Error loading properties : " + e.getMessage());
		}
	}
		
	// writes the properties in a file
	public void syncProperties(){
		try{
			properties.store(new FileOutputStream(propertiesPath), "");
		} catch ( IOException e) {
			System.err.println("Error exporting properties : " + e.getMessage());
		}
	}
	
	
	
	// file ownership functions
	public void setFileOwner(String name, int clientID){
		properties.setProperty(name + "_owner", Integer.toString(clientID));
	}
	public int getFileOwner(String name){
		return Integer.parseInt(properties.getProperty(name + "_owner"));
	}
	
	public void setFileChecksum(String name, byte[] checksum){
		try{			
			String MD5String = new String(checksum, "UTF-8");
			setFileChecksum(name, MD5String);
		}
		catch(Exception e){
			System.err.println("Error storing " + name + " checksum : " + e.getMessage());
		}		
	}
	
	public void setFileChecksum(String name, String checksum){	
		properties.setProperty(name + "_MD5", checksum);
	}
	
	// checksum
	public static byte[] checksum(String content){
		return checksum(content.getBytes());
	}
	public static byte[] checksum(byte[] content){
		byte[] checksum = new byte[0];
		try{
			checksum = MessageDigest.getInstance("MD5").digest(content);
		}
		catch(Exception e){
			System.err.println("Error calculating checksum : " + e.getMessage());
		}
		return checksum;
	}
	
	
	// file checksum functions
	public String getFileChecksum(String name){
		return properties.getProperty(name + "_MD5");	
	}
	
	public String readFile(String name){
		
		File file = new File(storage + name);
		byte[] content = new byte[0];
		String contentString = "";
		
		try{
			
			content = Files.readAllBytes(file.toPath());
			byte[] checksum = MessageDigest.getInstance("MD5").digest(content);
			contentString = new String(content, "UTF-8");
			
			setFileChecksum(name, checksum);
			
		}
		catch(Exception e){
			System.err.println("Error reading " + name + " : " + e.getMessage());
		}
		
		return contentString;
	}
	
	
	// write String in file
	public void writeFile(String name, String content){
		try {
			File f = new File(storage + name);
			FileWriter fw = new FileWriter(f);
			fw.write(content);
			fw.flush();
			fw.close();
			
			byte[] checksum = checksum(content);
			setFileChecksum(name, checksum);
			
		} catch (IOException e) {
			System.err.println("Error writing file content in syncLocalDirectory : " + e.getMessage());
		}
	}
	
}