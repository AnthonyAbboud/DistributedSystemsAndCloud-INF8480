package tp2;

import java.rmi.ConnectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


import java.util.*;
import java.io.*;

import tp2.LDAPinterface;



public class LDAP implements LDAPinterface {
	
	
	public static void main(String[] args) {
		LDAP server = new LDAP();
		server.run();
	}
	
	public LDAP()  {
		super();
	}
	
	
	// launches the server service
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			
			// chooses the port the object will be exported on
			LDAPinterface stub = (LDAPinterface) UnicastRemoteObject.exportObject(this, 5001);
			
			// finds the registry based on port (!)
			Registry registry = LocateRegistry.getRegistry(5000);
			// binds, in the registry, its instance to its name
			registry.rebind("LDAP", stub);
			System.out.println("LDAP ready.");
		
		
		} catch (ConnectException e) {
			
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
		
	}
	
	// total number of ervers referenced
	int nServers = 0;
	// clients referenced (name, password)
	Map<String, String> clients = new HashMap<String, String>();
	// servers referenced (address, name)
	List<ServerInfo> servers = new ArrayList<ServerInfo>();
	
	@Override
	public boolean confirmClient(String name, String password)  throws RemoteException{
		return clients.containsKey(name) && clients.get(name).equals(password) ;
	}
	
	@Override
	public void registerClient(String name, String password)  throws RemoteException{
		clients.put(name, password);
	}
	
	@Override
	public ServerInfo[] getServers(String name, String password)  throws RemoteException{
		if (confirmClient(name, password)){
			return (ServerInfo[]) servers.toArray();
		}
		return new ServerInfo[0];
	}
	
	@Override
	public String registerServer(String address)  throws RemoteException { // returns a generated name
		ServerInfo server = new ServerInfo();
		String name = "server" + Integer.toString(nServers);
		server.address = address;
		server.name = name;
		nServers++;
		return name;
	}
}