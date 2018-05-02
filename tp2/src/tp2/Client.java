package tp2;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.*;
import java.io.*;

import tp2.ServerInterface;


public class Client {

	/*
	*	Function: main()
	*
	*	In params:
	*			- String[] args: Array containing all arguments passed in parameters by user.
	*
	*	Description: Main function of class Client
	*/
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
      
		Client client = new Client();
		client.run(args);
		
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println(elapsedTime);
	}
	
	/* Declaration of 4 serverStubs, 4 being the maximum number of servers used in this experiment */
	private ServerInterface[] serverStubs = new ServerInterface[4];
	
	/* Declaration of number of data (they're all the same in all the files (100)) */
	private int length = 100;

	/* Array of type Data containing all the data from the text file */
	public List<AbstractMap.SimpleEntry<String, Integer>> data;

	/* Port used to connect the Client (5000 by default to work in the labs) */
	int port = 5000;

	/* Declaration of threads for multithreading */
	Thread[] myThreads;
	
	/* number of servers connected (max 4) */
	int nbServers = 0;


	/*
	*	Function: super()
	*
	*	Description: Calls the parent constructor with no arguments.
	*/
	public Client() {
		super();
	}

	/*
	*	Function: loadServerStub()
	*
	*	In params:
	*			- String hostname: IP adress of the server
	*
	*	Returns: Server stub of class ServerInterface
	*
	*	Description: Connects to a server and loads a server stub.
	*/
	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;
		try {
			
			Registry registry = LocateRegistry.getRegistry(hostname, port);
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
	
	/*
	*	Function: loadData()
	*
	*	In params:
	*			- String fileName: Name of the file containing the operations.
	*
	*	Description: Opens the text file and loads its data into the array data of type Data.
	*/
	private void loadData(String fileName){

		try{
			String line = null;
			String[] tokens;
			//int index = 0;			
			data = new ArrayList(length);

			/* Relative path of file */
			String filePath = "operations/" + fileName;

			/* Open text file */
			FileReader fileReader = new FileReader(filePath);
			/* Wrap FileReader in BufferedReader */
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while((line = bufferedReader.readLine()) != null){
				/* Split the name of operation and value */
				tokens = line.split(" ");
				if(tokens.length != 2){throw new IllegalArgumentException();}
				/* Store them in the object attributes as String and Int */
				data.add( new AbstractMap.SimpleEntry<String, Integer>(tokens[0], Integer.parseInt(tokens[1]) ) );
			}

			/* Close text file */
			bufferedReader.close();

		}
		catch(FileNotFoundException e){
			System.err.println("Error opening file: " + e.getMessage());
		} 
		catch(IOException e){
			System.err.println("Error reading operations from file: " + e.getMessage());
		}
	}

	/*
	*	Function: distribution()
	*
	*	In params:
	*			- Data[][] subLists: 2D Array containing the lists of data that will be sent to each server.
	*			- int nbServers: Number of servers.
	*
	*	Description: Computes the sub lists to distribution according to number of computing servers active.
	*/
	private List<List<AbstractMap.SimpleEntry<String, Integer>>> distribution(){
		
		List<List<AbstractMap.SimpleEntry<String, Integer>>> subLists = new ArrayList<List<AbstractMap.SimpleEntry<String, Integer>>>(nbServers);
		
		int totalData = data.size();
		/* Number of data */
		int remainingSize = totalData;
		/* Number of data to send to current server */
		int currentSize = 0;
		/* Maximum capacities of each server */
		int[] capacities = new int[nbServers];
		/* Total capacities of all servers */
		int totalCapacities = 0;

		try{
			/* Retrieve capacity data from servers */
			for(int i = 0; i < nbServers; i++){
				capacities[i] = serverStubs[i].getWorkCapacity();
				totalCapacities += capacities[i];
				System.out.println(capacities[i]);
			}
		} catch(RemoteException e){
			System.err.println("Error getting work capacities or servers: " + e.getMessage());
		}

		/* Distribution function: Total data * (capacity(i) / (sum(capacities))) */
		for(int i = 0; i < nbServers-1; i++){
			currentSize = (int)(capacities[i] / totalCapacities);
			//subLists[i] = new ArrayList<AbstractMap.SimpleEntry<String, Integer>>(currentSize);
			subLists.add(data.subList(totalData-remainingSize, totalData-remainingSize+currentSize));
			remainingSize -= currentSize;
		}
		//subLists[nbServers-1] = new ArrayList<AbstractMap.SimpleEntry<String, Integer>>(remainingSize);
		subLists.add(data.subList(totalData-remainingSize, totalData));
		
		/* Function call to send the lists to each server */
		return subLists;
	}

	/*
	*	Function: sendToServers()
	*
	*	In params:
	*			- Data[][] subLists: 2D Array containing the lists of data that will be sent to each server.
	*			- int nbServers: Number of servers.
	*
	*	Description: Sends each sub list to its appropriate to server.
	*/
	private List<AbstractMap.SimpleEntry<Boolean, Integer>> sendToServers(List<List<AbstractMap.SimpleEntry<String, Integer>>> subLists){

		List<AbstractMap.SimpleEntry<Boolean, Integer>> results = new ArrayList<AbstractMap.SimpleEntry<Boolean, Integer>>();

		/* For each server, send data in a different thread */
		for(int i = 0; i < nbServers; i++){
			myThreads[i] = new Thread(){
				public void run(){
					
						/* Sends data and receives the computing result in results[i] */
						for(int i = 0; i < nbServers; i++){
							
							ArrayList<AbstractMap.SimpleEntry<String, Integer>> message = new ArrayList<AbstractMap.SimpleEntry<String, Integer>>(subLists.get(i));
							
							AbstractMap.SimpleEntry<Boolean, Integer> entry = new AbstractMap.SimpleEntry<Boolean, Integer>(false, 0);
							
							try{
								entry = serverStubs[i].sendWork( message );
							} catch(RemoteException e){
								System.err.println("Error sending data to servers: " + e.getMessage());
							}
							
							//System.out.println("entry received : " + Boolean.toString(entry.getKey()) + " " + Integer.toString(entry.getValue()));
							results.add(entry);
						}
						
					
				}
			};
			/* Activates thread */
			myThreads[i].start();
		}
		
		/* Wait for thread to finish */
		for(int i = 0; i < nbServers; i++){
			try{
				myThreads[i].join();
			} catch(InterruptedException e){
				System.err.println("Error receiving server results: " + e.getMessage());
			}
		}
		
		return results;
	}

	/*
	*	Function: verifyResults()
	*
	*	In params:
	*			- Data[][] subLists: 2D Array containing the lists of data that will be sent to each server.
	*			- int nbServers: Number of servers.
	*
	*	Description: Verifies if the sub results are all valid/accepted. If not, returns to distribution function.
	*/
	private boolean verifyResults(List<AbstractMap.SimpleEntry<Boolean, Integer>> results){
		for(int i = 0; i < nbServers; i++){
			if(results.get(i).getKey().equals(false)){
				return false;
			}
		}
		return true;
	}
	
	// function used in secure mode
	private List<AbstractMap.SimpleEntry<Boolean, Integer>> secured(List<List<AbstractMap.SimpleEntry<String, Integer>>> subLists) {
		List<AbstractMap.SimpleEntry<Boolean, Integer>> results;
		
		do{
			results = sendToServers(subLists);
		}
		while( !verifyResults(results));
		
		return results;
	}
	
	// function used in unsecure mode
	private List<AbstractMap.SimpleEntry<Boolean, Integer>> unsecured(List<List<AbstractMap.SimpleEntry<String, Integer>>> subLists) {
		List<AbstractMap.SimpleEntry<Boolean, Integer>> result1 = secured(subLists);
		List<AbstractMap.SimpleEntry<Boolean, Integer>> result2 = secured(subLists);
		List<AbstractMap.SimpleEntry<Boolean, Integer>> results = new ArrayList<AbstractMap.SimpleEntry<Boolean, Integer>>();
		
		// for all received results
		for(int i=0; i< result1.size(); i++){
			
			AbstractMap.SimpleEntry<Boolean, Integer> entry1 =  result1.get(i);
			AbstractMap.SimpleEntry<Boolean, Integer> entry2 = result2.get(i);
			
			// while the servers do not agree, we recalculate the answer
			while( !entry1.equals(entry2) ){
			
				List<List<AbstractMap.SimpleEntry<String, Integer>>> list = new ArrayList<List<AbstractMap.SimpleEntry<String, Integer>>>();
				list.add( subLists.get(i) );
				list.add( subLists.get(i) );
				List<AbstractMap.SimpleEntry<Boolean, Integer>> calculatedResults = sendToServers(list);
				entry1 = calculatedResults.get(0);
				entry2 = calculatedResults.get(1);
			}
			results.add(entry1);
		}
		
		return results;
	}
	

	/*
	*	Function: finalAnswer()
	*
	*	In params:
	*			- int nbServers: Number of servers.
	*
	*	Description: Computes the final results, applies the final modulo 4000 to avoid overflow and displays it to the user.
	*/
	private void finalAnswer(List<AbstractMap.SimpleEntry<Boolean, Integer>> results){

		long finalResult = 0;

		for(int i = 0; i < nbServers; i++){
			//System.out.println("Result size " + Integer.toString(results.size()));
			if (i < results.size()){
				System.out.println("Resultat du serveur " + Integer.toString(i) + " : " + Integer.toString(results.get(i).getValue()));
				finalResult += results.get(i).getValue();
			}
		}

		finalResult = finalResult%4000;

		System.out.println("Resultat du calcul: " + finalResult);
	}

	/*
	*	Function: run()
	*
	*	In params:
	*			- Strings[] args: Array of strings, being the parameters passed by the user on terminal
	*
	*	Description: Runs the dispatcher and its different methods for this experiment.
	*/
	public void run(String[] args){

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		/* If the file name is specified, load its data */
		if(args.length >= 1){	
			loadData(args[0]);
		}

		/* Define the mode */
		boolean modeSecured = true;
		if(args.length >= 2){
			if(args[1] == "s"){modeSecured = true;}
			else if(args[1] == "n"){modeSecured = false;}
		}

		/* Number of computing servers */
		nbServers = args.length - 2;
		myThreads = new Thread[nbServers];
		
		/* Server stubs loading */
		for(int i = 0; i < nbServers; i++){
			serverStubs[i] = loadServerStub(args[i+2]);
		}
		
		/* Sublists to split task between the servers */
		List<List<AbstractMap.SimpleEntry<String, Integer>>> subLists = distribution();

		/* Determine work redistribution (need to keep doing that whenever something fails?)*/
		List<AbstractMap.SimpleEntry<Boolean, Integer>> results;
		if( modeSecured ){
			results = secured(subLists);
		}
		else{
			results = unsecured(subLists);
		}
		


		/* Display final answer */
		finalAnswer(results);
	}
	
}
