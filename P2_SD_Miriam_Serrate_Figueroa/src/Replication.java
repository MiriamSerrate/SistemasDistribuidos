
import java.rmi.*;

public interface Replication extends Remote {
	void replicate(String key, String value, int version) throws RemoteException;
	int getVersion(String key) throws RemoteException;
	String getValue(String key) throws RemoteException;
	boolean alive() throws RemoteException;
}
