package tp2;
import java.rmi.Remote;
import java.rmi.RemoteException;



/*
//assuming the rmiregistry is on port 5000, this is the code you'll find in client
Registry registry = LocateRegistry.getRegistry(address, 5000);
stub = (ServerInterface) registry.lookup(name);
*/
class ServerInfo{
	String address;
	String name;
}


public interface LDAPinterface extends Remote {
	// for the client
	public void registerClient(String name, String password)  throws RemoteException;
	ServerInfo[] getServers(String name, String password)  throws RemoteException;
	
	// for the server
	public boolean confirmClient(String name, String password)  throws RemoteException;
	public String registerServer(String address)  throws RemoteException; // returns a generated name
	//InetAddress.getLocalHost().getHostAddress()
}