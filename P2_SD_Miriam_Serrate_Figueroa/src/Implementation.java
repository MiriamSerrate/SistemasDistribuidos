
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;

public class Implementation extends UnicastRemoteObject implements Replication, KVStore {
	private static final long serialVersionUID = 1L;
	private int id;
	@SuppressWarnings("unused")
	private int n;
	private String _SERVICENAME;
	private String serveraddr;
	private HashMap<String, Value> hashMap = new HashMap<String, Value>();
	private static Replication[] preferenceList;
	
	public Implementation(int id, int n, String serverAddr) throws RemoteException {
		super();
		this.id = id;
		this.n = n;
		serveraddr = serverAddr;
		this._SERVICENAME = "rmi://" + serverAddr + "/" + id;
	}
	
	public int getVersion(String key) throws RemoteException{ // Give the value and the version that this server have
		if(hashMap.containsKey(key)) return hashMap.get(key).version;
		return -1;
	}
	
	public String getValue(String key) throws RemoteException{ // Give the value and the version that this server have
		if(hashMap.containsKey(key)) return hashMap.get(key).value;
		return null;
	}
	
	public boolean alive() throws RemoteException{ //Answer true if the server is alive
		return true;
	}
	
	private int neighAlive() throws RemoteException { //Get the neighbors alive
		try{
			String[] names = Naming.list("rmi://" + serveraddr + ":1099");
		    int N = names.length;
		    preferenceList = new Replication[N];
		    int cont = 0;
		    for(int i=0; i<N; i++) { 
		    	 Replication replication = (Replication) Naming.lookup("rmi://" + serveraddr + "/" + (id+1+i) % N);
		    	 try{
			    	 if(replication.alive()){ //Comprove if the neighbor is alive
			    		 preferenceList[cont] = replication; 
			    		 cont++;
			    	 }
		    	 }catch(Exception e){}
		    }
		    return cont; //Return the number of alive servers
		} catch (MalformedURLException e) {
		} catch (NotBoundException e) {}
		return 0;
	}

	
	public String get(String key) throws RemoteException, IncorrectServersException {
		
		int numServers = neighAlive(); //Get the neighbors alive
		if (numServers < 2) throw new IncorrectServersException(); //We need 2 more servers to do the read
		
		int neighVersion, actualVersion = 0;
		String value = null;
		
		if(hashMap.containsKey(key)){
			actualVersion = hashMap.get(key).version;
			value = hashMap.get(key).value;
		}
		
		boolean neigh = false;
		Replication replication = null;
		
		for(int i = 0; i<2; i++){
			neighVersion = preferenceList[i].getVersion(key);
			if(neighVersion > actualVersion){
				actualVersion = neighVersion; //Store the highest version
				replication = preferenceList[i]; //Store the neigh with the highest version
				neigh = true;
			}
		}
		if(neigh) return replication.getValue(key);
		else return value;
	}
	
	
	public String put(String key, String value) throws RemoteException, IncorrectServersException {
		
		int version = 0, neighVersion, neighPos = -1;
		String v = null;
		
		int numServers = neighAlive(); //Get the neighbors alive
		
		System.out.println("Put: "+key+", "+value);
		if(numServers < 3) throw new IncorrectServersException(); //We need 3 more servers to do the write
		
		if(hashMap.containsKey(key)){ //If the key exist: keep the old value and remove this key
			version = hashMap.get(key).version;
		
			for(int i = 0; i<3; i++){
				neighVersion = preferenceList[i].getVersion(key);
				
				if(neighVersion != -1) { //The neigh doesn't have this key
					if (neighVersion > version){
						version = neighVersion;
						neighPos = i;
					}
				}
			}
			if(neighPos == -1) v = hashMap.get(key).value;
			else v = preferenceList[neighPos].getValue(key);
			version ++; //Update version
			hashMap.remove(key); //Remove the old key, value
		}
		
		Value val = new Value(value, version);
		hashMap.put(key, val);
		System.out.println("Put done");
		
		//Call a thread for each neighbor (only the first three in the table)
		for(int i=0; i<3; i++) new ThreadReplicate(preferenceList[i], key, value, version).start(); 
		
				
		return v;
	}
	
	public void replicate(String key, String value, int version) throws RemoteException{
		System.out.println("Replication: "+key+", "+value+", "+_SERVICENAME);
		
		if(hashMap.containsKey(key)) hashMap.remove(key); //Remove the old key, value
		Value val = new Value(value, version);
		hashMap.put(key, val);
	}
	
	
	// SPMD
	public static void main(String[] args) throws Exception {
			 System.setProperty("java.security.policy", "server.policy");
		     if ( System.getSecurityManager() == null ) {
		            System.setSecurityManager(new RMISecurityManager( ) );
			 }
		     
		     int id = Integer.parseInt(args[0]); 	// Identifies itself
		     String serveraddr = args[1];  // Identifies server (IP or host name)
		     int n = Integer.parseInt(args[2]);  // Number of manager in the ring
		     
		     Implementation manager = new Implementation (id, n, serveraddr);
			 
		     Naming.rebind("rmi://" + serveraddr + "/" + id,  manager);
		    
	}  // main


}
