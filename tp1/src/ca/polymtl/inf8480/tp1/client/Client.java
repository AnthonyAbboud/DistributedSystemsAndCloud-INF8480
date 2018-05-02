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
		
		// the instance of CloudUtil, in charge of keeping file state
		// and providing lower level functionnalities
		// see CloudUtil in shared folder
		util = new CloudUtil("./clientFiles/");
		
		// if "./client" alone is entered
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
						// we get an id and save it
						clientID = serverStub.CreateClientID();
						String stringID = Integer.toString(clientID);
						util.properties.setProperty("clientID", stringID);
						util.syncProperties();
						System.out.println("received id : " + stringID);
						return;
					}
					// on any other command we charge the id from properties
					default:{
						try{
							clientID =  Integer.parseInt( util.properties.getProperty("clientID"));
						}
						// an error means it wasn't set yet
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
					
					//all the work is done on server here
					String nom = args[1];
					boolean reussi = serverStub.create(nom);
					
					if(reussi) System.out.println("a file named \"" + nom + "\" has been added.");
					else  System.out.println("a file named \"" + nom + "\" already exists.");
					
					break;
				}
				
				case "list":
				{
					
					String[][] files = serverStub.list();
					
					// used format is {name [, owner]}
					for(String[] file : files){
						// so we first extract name 
						System.out.print("* " + file[0] + " \t");
						// then the owner if specified
						if(file.length > 1) System.out.println("locked by " + file[1]);
						else System.out.println("unlocked");
					}
					
					break;
				}
				
				case "sync":
				case "syncLocalDirectory":
				{
					String[][] files = serverStub.syncLocalDirectory();
					
					// we simply write content into a file
					// compute and save the checksum
					for(String[] file: files){
						util.writeFile(file[0], file[1]);
						util.setFileChecksum(file[0], util.checksum(file[1]));
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
					
					// we get the checksum, otherwise it means we don't have the file yet
					try{
						checksum = util.getFileChecksum(nom);
					}
					catch(Exception e){
						checksum = "0";
					}
					
					// with that we can request to receive the file
					String[] content = serverStub.get(nom, checksum);
					
					// then we either received it or not
					if(content.length != 0){
						util.writeFile(nom, content[0]);	
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
					
					// exactly the same as a get
					// if the directives were to have the "get" and "lock" functions exposed by the client and not the server,
					// i'd simply have made "lock" call "get" in client
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
						System.out.println(nom + " received");
					}
					
					
					// whether or not we reseived te file
					// we want to see if we have aquired the lock :
					int owner = Integer.parseInt(content[0]);
					if(owner == clientID) System.out.println("lock on " + nom + " aquired.");
					// a special case that i added
					else if(owner == 0) System.out.println(nom + " does not exist on server.");
					// sorry pal, toilet's occupied
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
					
					// we read the file content
					String content = util.readFile(nom);
					
					// send it to the server with hopes he'll like it
					boolean reussi = serverStub.push(nom, content, clientID);
					
					// did he ?
					if(reussi) System.out.println("your changes have been pushed.");
					else  System.out.println("push failed.\n you either do not own the file (try the lock command)\n or it does not exist yet (use the create command).");
					
					break;
				}	
				
				// default is where typos go
				default:{
					System.out.println("command \"" + args[0] + "\" does not exist.\n\n choose from :\n CreateClientID\n create <filename>\n list\n syncLocalDirectory\n get <filename>\n lock <filename>\n push <filename>");
				}
			}
		
		// did you forget to turn on the server ?
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
		// except getClientID, all calls finish by this saving properties call
		util.syncProperties();
		
	}		
		
	}
	
}
