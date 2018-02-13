package ca.polymtl.inf8480.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

public interface ServerInterface extends Remote {
	
	public int					CreateClientID() 				throws RemoteException;
	public boolean			create(String nom) 			throws RemoteException;
	public String[][]		list()									throws RemoteException;
	public String[][]		syncLocalDirectory() 		throws RemoteException;
	public String[] 			get(String nom, int checksum)						throws RemoteException;
	public String[]			lock(String nom, int clientid, int checksum) 	throws RemoteException;
	public boolean 		push(String nom, String contenu, int clientid) throws RemoteException;
	
}
