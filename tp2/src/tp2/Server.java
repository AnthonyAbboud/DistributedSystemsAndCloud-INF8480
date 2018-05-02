package tp2;

import java.rmi.ConnectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


import java.util.*;
import java.io.*;


import tp2.ServerInterface;
import tp2.Operations;

public class Server implements ServerInterface {
	
	//main
	public static void main(String[] args) {
		if(args.length < 4){
			//System.out.println(args.length);
			System.out.println("expected arguments : serverName, port, workCapacity, maliciousness");
			return;
		}
		Server server = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Float.parseFloat(args[3]) );
		server.run();
	}
	
	// the name assigned to the server object by the rmiregistry
	String name;
	// the port used by the rmiregistry
	int port;
	// the number of operations for a task to be guaranted to be accepted
	int Q;
	// the tendency to return false results
	float m;
	// a random number generator object
	java.util.Random random = new Random();
	
	
	public Server(String serverName, int serverPort, int workCapacity, float maliciousness)  {
		super();
		name = serverName;
		port = serverPort;
		Q = workCapacity;
		m = maliciousness;
	}
	
	
	// launches the server service
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			
			// chooses the port the object will be exported on
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, port);
			
			// finds the registry based on port (!)
			Registry registry = LocateRegistry.getRegistry(5000);
			// binds, in the registry, its instance to its name
			registry.rebind(name, stub);
			System.out.println("Server ready.");
		
		
		} catch (ConnectException e) {
			
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
		
	}
	
	@Override
	public int getWorkCapacity() throws RemoteException{
		return Q;
	}
	
	
	// receives and and does the work depending on server parameters
	@Override
	public AbstractMap.SimpleEntry<Boolean, Integer> sendWork(ArrayList<AbstractMap.SimpleEntry<String, Integer>> data) throws RemoteException {
		
		// a result object that will be sent back
		boolean accepted = false;
		int result = 0;
		
		// workload size (number of operations)
		int u = data.size();
		
		// we compute the chance to defect based on that and workCapacity
		float chanceToDefect= ((float) (u-Q)) /Q;
		
		// if the server defects, it stops here
		if (random.nextFloat() < chanceToDefect){
			accepted = false; 
		}
		else{
			
			// otherwise the work is accepted
			accepted = true;
			
			// there's still the chance that it sends back the wrong answer
			if (random.nextFloat() < m){
				// a random wrong answer
				result = (random.nextInt()%4000);
			}
			else {
				
				// otherwise we compute and send the right answer
				result = execute(data);
			}
		}
		
		return new AbstractMap.SimpleEntry<Boolean, Integer>(accepted, result);
	}
	
	// the computation function
	private int execute(ArrayList<AbstractMap.SimpleEntry<String, Integer>> operations){
		
		// the result that will be sent back
		long result = 0;
		
		// for each operation
		for(AbstractMap.SimpleEntry<String, Integer> operation : operations){
			
			System.out.println(operation.getKey() + " " + Integer.toString(operation.getValue()));
			
			// we apply the right function and add it to the sum
			switch(operation.getKey()){
				case "prime":{
					result += (Operations.prime(operation.getValue())%4000);
					break;
			}
				case "pell":{
					result += (Operations.pell(operation.getValue())%4000);
					break;
				}
			}
		}
		
		// a last modulo so the sum is within expected range
		return ((int) result%4000);
	}
	
	
	
	
}



















