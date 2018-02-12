package ca.polymtl.inf8480.tp1.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;
import ca.polymtl.inf8480.tp1.shared.CloudUtil;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;


public class Server implements ServerInterface {

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
		
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		
		
		} catch (ConnectException e) {
			
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
		util = new CloudUtil("./serverFiles/");
		
		
		try{
			clientsTotal =  Integer.parseInt( util.properties.getProperty("clientsTotal"));
		}
		catch(NumberFormatException e){}
		catch(NullPointerException e){}
		
	}
	
	public Server() {
		super();
	}
	
	// useful for ID generation
	// during first session, begins at 1 so 0 is reserved
	private int clientsTotal = 1;
	// retains data between sessions, mainly the association <string filename, int as string checksum>
	private CloudUtil util;

	
	/*
		Génère un identifiant unique pour le client.
		Celui-ci est sauvegardé dans un fichier local et est	retransmis au serveur lors de l'appel à lock() ou  push().
		Le client doit demander un identifiant lors de sa première interaction avec le serveur
		- Retourne un int correspondant à l'ID. -
	*/
	@Override
	public int CreateClientID() throws RemoteException{
		
		int ID = clientsTotal;
		clientsTotal += 1;
		
		// so we can have a different id each time even if the server closes
		util.properties.setProperty("clientsTotal", Integer.toString(clientsTotal));
		util.syncProperties();
		
		return ID;
	}
	
	/*
		Crée un fichier vide sur le serveur avec le nom spécifié.
		Si un fichier portant ce nom existe déjà, l'opération échoue.
		- Retourne un booleen pour indiquer si reussi. -
	*/
	@Override
	public boolean create(String nom) throws RemoteException{
		
		// attempts creating the file
		try{
			util.writeFile(nom,"");
			util.setFileOwner(nom, 0);
			util.syncProperties();
			return true;
		}
		catch(Exception e){}
		
		return false;
	}
	
	/*
		Retourne la liste des fichiers présents sur le serveur.
		Pour chaque fichier, le nom et l'identifiant du client possédant le verrou (le cas échéant) est retourné.
		- Retourne un tableau d'entrees : String nomFichier, int client ayant le verrou -
	*/
	@Override
	public String[][] list() throws RemoteException{
		
		// we get a liting of files in storage
		File[] files = new File(util.storage).listFiles();
		String[][] result = new String[files.length][];
		
		// for each file we add its info to result
		for (int i=0; i< files.length; i++) {
			
			File file = files[i];
			String name = file.getName();
			String owner = Integer.toString(util.getFileOwner(name));
			
			// if there is an owner (clientID != 0) we specify it 
			if(Integer.parseInt(owner)!=0){
				// used format is {name [, owner]}
				result[i] = new String[2];
				result [i][1] = owner;
			}
			else{
				result[i] = new String[1];
			}
			result[i][0] = name;
		}
		
		
		return result;
	}
	
	
	/*
		Permet de récupérer les noms et les contenus de tous les fichiers du serveur.
		Le client appelle cette fonction pour synchroniser son répertoire local avec celui du serveur.
		Les fichiers existants seront écrasés et remplacés par les versions du le serveur.
		- Retourne une liste d'entrees : String nomFichier, String contenu -
	*/
	@Override
	public String[][] syncLocalDirectory() throws RemoteException{
		// we get a liting of files in storage
		File[] files = new File(util.storage).listFiles();
		String[][] result = new String[files.length][3];
		
		// for each file we add its info to result
		for (int i=0; i< files.length; i++) {
			
			File file = files[i];
			String name = file.getName();
			String content= util.readFile(name);
			
			result[i][0] = name;
			result [i][1] = content;
			result [i][2] =Integer.toString(util.getFileOwner(name));
		}		
		
		return result;
	}
	
	/*
		Demande au serveur d'envoyer la dernière version du fichier spécifié.
		Le client passe également la somme de contrôle du fichier qu'il possède.
		Si le client possède une somme de contrôle égale à celle du serveur, le fichier n’est pas retourné car il est déjà à jour dans le client.
		Si le client ne possède pas le fichier, il doit spécifier une somme de contrôle nulle pour forcer le serveur à lui envoyer le fichier.
		Le fichier est écrit dans le répertoire local courant.
		- Retourne un tableau contenant le fichier si besoin : String contenu -
	*/
	@Override
	public String[] get(String nom, String checksum) throws RemoteException{
		
		String path = util.storage + nom;
		File f = new File(path);
		
		
		if(!f.exists()) return new String[0];
		
		
		String md5 = util.getFileChecksum(nom);
		
		if(md5 == checksum && Integer.parseInt(checksum)!=0) return new String[0];
		
		System.out.println("has to be dowloaded");
		
		String[] content = new String[1];
		
		content[0] = util.readFile(nom);
		
		System.out.println(content[0]);
		
		return content;
	}
	
	/*
		Demande au serveur de verrouiller le fichier spécifié.
		La dernière version du fichier est écrite dans le répertoire local courant ( la somme de contrôle est aussi utilisée pour éviter un transfert inutile).
		L'opération échoue si le fichier n’existe pas ou il est déjà verrouillé par un autre client.
		Le client doit recevoir l’ID du client qui détient le verrou.
		- Retourne un tableau : int  as String l'ID du client (celui fourni si le lock reussi), String contenu si besoin -
	*/
	@Override
	public String[] lock(String nom, int clientID, String checksum) throws RemoteException{
		
		String[] content = get(nom, Integer.toString(clientID));
		int owner = util.getFileOwner(nom);
		
		if(owner == 0) owner = clientID;
		
		String[] result;
		if(content.length != 0){
			result = new String[2];
			result[1] = content[0];
		}
		else {
			result = new String[1];
		}
		result[0] = Integer.toString(owner);
		
		return result;
	}
	
	
	/*
		Envoie une nouvelle version du fichier spécifié au serveur.
		L'opération échoue si le fichier n'avait pas été verrouillé par le client préalablement.
		Si le push réussit, le contenu envoyé par le client remplace le contenu qui était sur le serveur auparavant et le fichier est déverrouillé.
		- Retourne un booleen pour indiquer si reussi. -
	*/
	@Override
	public boolean push(String nom, String contenu, int clientID) throws RemoteException{
		
		
		System.out.println("push request from " + Integer.toString(clientID) + " on " + nom);
		
		File f = new File(util.storage + nom);
		
		
		if( !f.exists() ) return false;
		
		int owner = util.getFileOwner(nom);
		if( owner != clientID ) return false;
		
		util.writeFile(nom, contenu);
		
		util.setFileOwner(nom, 0);
		
		util.syncProperties();
		

		return false;
	}
	
	
	
}
