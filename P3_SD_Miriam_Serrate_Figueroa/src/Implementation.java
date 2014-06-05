

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class Implementation extends UnicastRemoteObject implements Replication, KVStore {
	private static final long serialVersionUID = 1L;
	private static int id;
	@SuppressWarnings("unused")
	private int n;
	private static long pos;
	private static String _SERVICENAME;
	private static String serveraddr;
	private static HashMap<String, Value> hashMap = new HashMap<String, Value>();
	private static Replication[] preferenceList;
	private static boolean[] aliveList;
	private static String minOfRank;
	private boolean serverDown = false;
	
	
	public Implementation(int id, int n, String serverAddr, long pos) throws RemoteException {
		super();
		this.id = id;
		this.n = n;
		this.pos = pos;
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
	
	public long alive() throws RemoteException, ServerDownException{ //Answer true if the server is alive
		if(serverDown) throw new ServerDownException();
		return pos;
	}
	
	private static int neighAlive() throws RemoteException { //Get the neighbors alive
		try{
			String[] names = Naming.list("rmi://" + serveraddr + ":1099");
			long[] positions = new long[names.length];
			long[] positionsOrdered = new long[names.length];
		    int N = names.length;
		    aliveList = new boolean[N];
		    preferenceList = new Replication[N];
		    Replication[] auxList = new Replication[N];
		    int cont = 0;
		    
		    for(int i=0; i<N; i++) { 
		    	 Replication replication = (Replication) Naming.lookup("rmi://" + serveraddr + "/" + i);
		    	 try{
		    		 long position = replication.alive();
			    	 if(position == 0 && !(names[i].equals(_SERVICENAME))){ //Comprove if the neighbor is alive
			    		 String n = names[i];
			    		 byte bytes[]= n.getBytes();
			    		 Checksum checksum = new Adler32();
			    		 checksum.update(bytes, 0, bytes.length);
			    		 long id = checksum.getValue();
			    		 positions[cont] = id;
			    		 positionsOrdered[cont] = id;
			    		 aliveList[i] = true;
			    		 auxList[cont] = replication; 
			    		 cont++;
			    	 }
			    	 else{
			    		 positions[cont] = position;
			    		 positionsOrdered[cont] = id;
			    		 aliveList[i] = true;
			    		 auxList[cont] = replication;
			    		 cont++;
			    	 }
		    	 } catch(ServerDownException e){
		    		 aliveList[i] = false;
		    	 }
		    }
		    Arrays.sort(positionsOrdered);
		    long idServer;
		    if(pos == 0){
			    String n = "//" + serveraddr + "/" + id;
	   		 	byte bytes[]= n.getBytes();
	   		 	Checksum checksum = new Adler32();
	   		 	checksum.update(bytes, 0, bytes.length);
	   		 	idServer = checksum.getValue();
		    }
	   		else idServer = pos;
	   		String rank = ""; 
   		 	int aux = 0;
   		 	//We need to find the neighbors of the server for the preferentList
   		 	boolean firstPos = false;
   		 	for(int i=0; i<cont-1; i++){
   		 		if(i==0 && positionsOrdered[i] > id) firstPos = true; // If the server is the first of the ring
   		 		if((id > positionsOrdered[i] && id < positionsOrdered[i+1]) || firstPos){
   		 			if(!firstPos) i = (i+1) % N;

   		 			for(int j = 0; j<cont; j++){
   		 				if(positions[j] == positionsOrdered[i]){
   		 					preferenceList[aux] = auxList[j]; 
   		 					minOfRank = rank;
   		 					rank = names[j];
   		 					
   		 					j = 0;
   		 					i = (i+1) % N;
   		 					aux++;
   		 				}
   		 			}
   		 			break;
   		 		}
   		 	}
		    
		    return cont; //Return the number of alive servers		
		} catch (MalformedURLException e) {
		} catch (NotBoundException e) {
		}
		return 0;
	}

	
	public String get(String key, boolean coordinatorDown) throws RemoteException, IncorrectServersException, ServerDownException {
		if(serverDown) throw new ServerDownException();
		int numServers = neighAlive(); //Get the neighbors alive
		int rank;
		if(coordinatorDown) rank = 1;  //Because is the second server for the client, the coordinator was down
		else rank = 2;
		
		if (numServers < rank) throw new IncorrectServersException(); //We need 2 more servers to do the read
		
		int neighVersion = 0, actualVersion = 0;
		String value = null;
		
		if(hashMap.containsKey(key)){
			actualVersion = hashMap.get(key).version;
			value = hashMap.get(key).value;
		}
		
		boolean neigh = false;
		Replication replication = null;
		
		for(int i = 0; i<rank; i++){
			try{
			neighVersion = preferenceList[i].getVersion(key);
			}catch(Exception e){}
			if(neighVersion > actualVersion){
				actualVersion = neighVersion; 		//Store the highest version
				replication = preferenceList[i]; 	//Store the neigh with the highest version
				neigh = true;
			}
		}
		if(neigh) return replication.getValue(key);
		else return value;
	}
	
	
	public String put(String key, String value, boolean coordinatorDown) throws RemoteException, IncorrectServersException, ServerDownException {
		if(serverDown) throw new ServerDownException();
		int version = 0, neighVersion, neighPos = -1;
		String v = null;
		
		int numServers = neighAlive(); //Get the neighbors alive
		int rank;
		if(coordinatorDown) rank = 2; //Because is the second server for the client, the coordinator was down
		else rank = 3;
		
		if(numServers < rank) throw new IncorrectServersException(); //We need 3 more servers to do the write
		
		System.out.println("Put: "+key+", "+value);
		
		if(hashMap.containsKey(key)){ //If the key exist: keep the old value and remove this key
			version = hashMap.get(key).version;
		
			for(int i = 0; i<rank; i++){
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
		
		//Call a thread for each neighbor (only the first three in the table)
		for(int i=0; i<3; i++) new ThreadReplicate(preferenceList[i], key, value, version).start(); 
		
		return v;
	}
	
	public void replicate(String key, String value, int version) throws RemoteException{		
		if(hashMap.containsKey(key)) hashMap.remove(key); //Remove the old key, value
		Value val = new Value(value, version);
		hashMap.put(key, val);
	}
	
	public HashMap<String, Value> hello(boolean newServer) throws RemoteException { 
		//Ask the other servers for keys and values it needs and the server asked returns all its hashMap
		if(newServer) n++;
		return hashMap;
	}
	
	
	public static boolean update(boolean newServer) throws RemoteException { //Update with the corresponding keys and values
		int numServers = neighAlive();
		long max, min; //The keys are in this rank
		
				byte bytesMax[]= minOfRank.getBytes();
				Checksum checksum = new Adler32();
				checksum.update(bytesMax, 0, bytesMax.length);
				min = checksum.getValue();
				
				byte bytesMin[]= _SERVICENAME.getBytes();
				checksum = new Adler32();
				checksum.update(bytesMin, 0, bytesMin.length);
				max = checksum.getValue();
				
				boolean specialCase = false;
				if(min > max) specialCase = true;
				Set<String> keys;
				for(int i = 0; i<numServers; i++){
					Replication neigh = preferenceList[i];
					HashMap<String, Value> newHash = neigh.hello(newServer);
					keys = newHash.keySet();
					
					for(String key: keys){
		    			byte bytesKey[]= key.getBytes();
		    			checksum = new Adler32();
		    			checksum.update(bytesKey, 0, bytesKey.length);
		    			long num = checksum.getValue();
		    			
		    			if((num > min && num >= max) || (specialCase && (num < min || num > max))){
		    				boolean addKey = true;
		    				if(hashMap.containsKey(key)){ //If the key exist: keep the old value and remove this key
		    					int version = hashMap.get(key).version;
		    					if(version < newHash.get(key).version) hashMap.remove(key); //Remove the old key, value
		    					else addKey = false;
		    				}
		    				if(addKey) hashMap.put(key, newHash.get(key));
		    			}
					
					}
				}
		return true; 
	}
	
	public long getPosition() throws RemoteException {
		return pos;
	}
	
	public void getDownServer() throws RemoteException {
		serverDown = true;
		hashMap.clear(); //Clean the keys and values we have to simulate that was down.
	}
	
	public void getUpServer() throws RemoteException {
		serverDown = false;
		update(false); //Update the keys and values because we simulate it was down and we need to update		
	}
	
	// SPMD
	public static void main(String[] args) throws Exception {
			 System.setProperty("java.security.policy", "server.policy");
		     if ( System.getSecurityManager() == null ) {
		            System.setSecurityManager(new RMISecurityManager( ) );
			 }
		     boolean newServer;
		     int id = Integer.parseInt(args[0]); 	// Identifies itself
		     String serveraddr = args[1];  			// Identifies server (IP or host name)
		     int n = Integer.parseInt(args[2]);  	// Number of manager in the ring
		     
		     String a = args[3];
		     if(a.equals("true")) newServer = true; // If is the new server would be true
		     else newServer = false;
		    
		     long pos = 0; 
		     
		     if(newServer)  pos = Long.parseLong(args[4]); 	// Position in the ring for the new servers, the old servers have a 0

		    
		     Implementation manager = new Implementation (id, n, serveraddr, pos);
		     
		     neighAlive(); //Because we need the preference list
		    
		     if(newServer) update(newServer);
		     
		    
		     Naming.rebind("rmi://" + serveraddr + "/" + id,  manager);
		     
		     //Periodic tack that say when a server goes down and when it gets up
		     while(true){
		    	 ThreadPeriodic periodic = new ThreadPeriodic(serveraddr, aliveList);
		    	 periodic.start();
		    	 
		    	 try {
		 			ThreadPeriodic.sleep(3000);
		 		} catch (InterruptedException e) {}
		     }
		    
	}  // main






}
