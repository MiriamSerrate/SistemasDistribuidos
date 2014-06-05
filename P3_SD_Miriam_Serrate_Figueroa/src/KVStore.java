
import java.rmi.*;

public interface KVStore extends Remote {
	public String get(String key, boolean error) throws RemoteException, IncorrectServersException, ServerDownException;
	public String put(String key, String value, boolean error) throws RemoteException, IncorrectServersException, ServerDownException;
	public long getPosition() throws RemoteException;
	public void getDownServer() throws RemoteException;
	public void getUpServer() throws RemoteException;
}
