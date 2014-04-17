
import java.rmi.*;

public interface KVStore extends Remote {
	public String get(String key) throws RemoteException, IncorrectServersException;
	public String put(String key, String value) throws RemoteException, IncorrectServersException;
}
