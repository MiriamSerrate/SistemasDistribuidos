
import java.rmi.*;
import java.util.HashMap;

public interface Replication extends Remote {
	void replicate(String key, String value, int version) throws RemoteException;
	int getVersion(String key) throws RemoteException;
	String getValue(String key) throws RemoteException;
	long alive() throws RemoteException, ServerDownException;
	HashMap<String, Value> hello(boolean newServer) throws RemoteException;
}
