package ca.polymtl.inf8480.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Client {
	
	
	public static void main(String[] args) {
		Client client = new Client();
		client.run(args);
	}
	
	// the ID used for gonnection
	private int clientID;
	// stub we're using for remote calls
	private ServerInterface serverStub = null;
	// retains data between sessions, mainly the association <string filename, int as string checksum>
	private java.util.Properties properties;
	// directory used to store data
	private String folder = "./clientFiles/";
	// directory that stores the managed files
	private String propertiesPath = folder + "properties.xml";
	// directory that stores the managed files
	private String storage = folder + "cloud_storage/";
	
	
	public Client() {
		super();
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
			
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage() 	+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.err.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
		}

		return stub;
	}
	
	private void syncProperties(){
		try{
			properties.store(new FileOutputStream(propertiesPath), "");
		} catch ( IOException e) {
			System.err.println("Error exporting properties : " + e.getMessage());
		}
	}
	
	
	public void run(String[] args){
		
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		serverStub = loadServerStub("127.0.0.1");
		
		// load server properties like number of slients, file checksums, ..
		try{
			properties = new Properties();
			properties.load(new FileInputStream(propertiesPath));
		} catch ( IOException e) {
			System.err.println("Erreur loading properties : " + e.getMessage());
			
			File file = new File(propertiesPath);
			String content= "";
			try{
				 content = new String(Files.readAllBytes(file.toPath()), "UTF-8");
			}
			catch(Exception e2 ){}
			
			System.err.println(content);
			
			return;
		}
		
		
		if (args.length == 0) {
			System.out.println("usage : \" ./client <command> <arguments> \"");
		}
		else {
			
			
			try {
				
				// check client has a valid ID
				switch(args[0]){
					case "ID":
					case "createID":
					case "CreateID":
					case "createClientID":
					case "CreateClientID":{
						clientID = serverStub.CreateClientID();
						properties.setProperty("clientID", Integer.toString(clientID));
						syncProperties();
						return;
					}
					default:{
						try{
							clientID =  Integer.parseInt( properties.getProperty("clientID"));
						}
						catch(Exception e){
							System.out.println("you need to call createClientId() first.");
							System.out.println(e.getMessage());
							return;
						}
					}
				}
				

			// if so we can do what he wants
			switch(args[0]){
				
				case "create":{
					
					if(args.length < 2){
						System.out.println("please specify a file name.");
						return;
					}
					String nom = args[1];
					boolean reussi = serverStub.create(nom);
					
					if(reussi) System.out.println("a file named \"" + nom + "\" has been added.");
					else  System.out.println("a file named \"" + nom + "\" already exists.");
					
					break;
				}
				
				case "list":
				{
					
					String[][] files = serverStub.list();
					
					for(String[] file : files){
						System.out.print("* " + file[0] + " \t");
						if(file.length > 1) System.out.println("locked by " + file[1]);
						else System.out.println("unlocked");
					}
					
					break;
				}
				
				case "sync":
				case "syncLocalDirectory":
				{
					String[][] files = serverStub.syncLocalDirectory();
					for(String[] file: files){
						try {
							File f = new File(storage + file[0]);
							FileWriter fw = new FileWriter(f);
							fw.write(file[1]);
							fw.flush();
							fw.close();
						} catch (IOException e) {
							System.err.println("Error writing file content in syncLocalDirectory : " + e.getMessage());
						}
						properties.setProperty(file[0] + "_MD5", file[2]);
					}
					break;
				}
				
				case "get":
				{
					if(args.length < 2){
						System.out.println("please specify a file name.");
						return;
					}
					String nom = args[1];
					String checksum;
					try{
						checksum = properties.getProperty(nom+"_MD5");
					}
					catch(Exception e){
						checksum = "0";
					}
					
					String[] content = serverStub.get(nom, checksum);
					if(content.length != 0){
						try {
							File f = new File(storage + nom);
							FileWriter fw = new FileWriter(f);
							fw.write(content[0]);
							fw.flush();
							fw.close();
						} catch (IOException e) {
							System.err.println("Error writing file content in get : " + e.getMessage());
						}
						
						String md5 = "";
						try{
							md5 = new String(MessageDigest.getInstance("MD5").digest(content[0].getBytes()), "UTF-8");
						}
						catch(Exception e){
							System.err.println("Error in get : " + e.getMessage());
						}
						
						properties.setProperty(nom + "_MD5", md5);
						
						System.out.println(nom + " received");
					}
					else System.out.println("no file received");
					
					break;
				}
				
				case "lock":
				{
					if(args.length < 2){
						System.out.println("please specify a file name.");
						return;
					}
					String nom = args[1];
					
					String checksum = "";
					try{
						checksum = properties.getProperty(nom+"_MD5");
					}
					catch(Exception e){
						checksum = "0";
					}
					
					String[] content = serverStub.lock(nom, clientID, checksum);
					if(content.length != 1){
						try {
							File f = new File(storage + nom);
							FileWriter fw = new FileWriter(f);
							fw.write(content[1]);
							fw.flush();
							fw.close();
						} catch (IOException e) {
							System.err.println("Error writing file content in lock : " + e.getMessage());
						}
						
						String md5 = "";
						try{
							md5 = new String(MessageDigest.getInstance("MD5").digest(content[1].getBytes()), "UTF-8");
						}
						catch(Exception e){
							System.err.println("Error in lock : " + e.getMessage());
						}
						
						properties.setProperty(nom + "_MD5", md5);
					}
					
					int owner = Integer.parseInt(content[0]);
					
					if(owner == clientID) System.out.println("lock on " + nom + " aquired.");
					else System.out.println(nom + " is locked by " + content[0]);
					
					break;
				}
				
				case "push":
				{
					if(args.length < 2){
						System.out.println("please specify a file name.");
						return;
					}
					String nom = args[1];
					
					File file = new File(storage + nom);
					byte[] content = new byte[0];
					String contentString = "";
					String MD5String = "";
					
					try{
						
						content = Files.readAllBytes(file.toPath());
						byte[] checksum = MessageDigest.getInstance("MD5").digest(content);
						
						contentString = new String(content, "UTF-8");
						MD5String = new String(checksum, "UTF-8");
						
					}
					catch(Exception e){
						System.err.println("Error in push : " + e.getMessage());
					}
					
					properties.setProperty(nom + "_MD5", MD5String);
					
					boolean reussi = serverStub.push(nom, contentString, clientID);
					if(reussi) System.out.println("your changes have been pushed.");
					else  System.out.println("push failed.\n you either do not own the file (use the lock command)\n or it does not exist yet (use the create command).");
					
					break;
				}	
				
				default:{
					System.out.println("command \"" + args[0] + "\" does not exist.\n\n choose from :\n CreateClientID\n create <filename>\n list\n syncLocalDirectory\n get <filename>\n lock <filename>\n push <filename>");
				}
			}
		
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
		syncProperties();
		
	}		
		
	}
	
}
