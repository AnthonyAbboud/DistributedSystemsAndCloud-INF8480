package ca.polymtl.inf8480.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;
import ca.polymtl.inf8480.tp1.shared.CloudUtil;

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
	private CloudUtil util;
	
	
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
	
	
	public void run(String[] args){
		
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		serverStub = loadServerStub("127.0.0.1");
		
		// see CloudUtil in shared folder
		util = new CloudUtil("./clientFiles/");
		
		
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
						util.properties.setProperty("clientID", Integer.toString(clientID));
						util.syncProperties();
						return;
					}
					default:{
						try{
							clientID =  Integer.parseInt( util.properties.getProperty("clientID"));
						}
						catch(Exception e){
							System.out.println("you need to call createClientId() first.");
							//System.out.println(e.getMessage());
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
						util.writeFile(file[0], file[1]);
						util.setFileChecksum(file[0], file[2]);
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
						checksum = util.getFileChecksum(nom);
					}
					catch(Exception e){
						checksum = "0";
					}
					
					String[] content = serverStub.get(nom, checksum);
					
					if(content.length != 0){
						
						util.writeFile(nom, content[0]);
						util.setFileChecksum(nom, util.checksum(content[0]));		
						
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
					
					String checksum;
					try{
						checksum = util.getFileChecksum(nom);
					}
					catch(Exception e){
						checksum = "0";
					}
					
					String[] content = serverStub.lock(nom, clientID, checksum);
					if(content.length != 1){

						util.writeFile(nom, content[1]);
						util.setFileChecksum(nom, util.checksum(content[1]));		
						
						System.out.println(nom + " received");
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
					
					String content = util.readFile(nom);
					
					boolean reussi = serverStub.push(nom, content, clientID);
					
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
		
		util.syncProperties();
		
	}		
		
	}
	
}
