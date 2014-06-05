

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class Client {

	private static ArrayList<RankServer> ranks;
	private static int numServers;
	private static long[] idList;
	private static String serveraddr;
	private static String[] names;
	
	public static int correctServer(String key){
		byte bytes[]= key.getBytes();
		Checksum checksum = new Adler32();
		checksum.update(bytes, 0, bytes.length);
		long id = checksum.getValue();
		
		int correctServer = -2;
		
		//Looking for the correct server
		if((id > ranks.get(0).rankA) || (id <= ranks.get(0).rankB)) correctServer = 0; //Look if its the first
		
		for(int i = 1; i<ranks.size(); i++){ 
			if((id > ranks.get(i).rankA) && (id <= ranks.get(i).rankB)){
				correctServer = i;
				break;
			}
		}
		return correctServer;
	}
	 private static void updateList(){
		 	
		try {
		names = Naming.list("rmi://" + serveraddr + ":1099");
			
		idList = new long[names.length];
		long[] idListOrdered = new long[names.length];
		numServers = names.length; //Total nodes
			
 		for (int i = 0; i < numServers; i++){
 			KVStore a = (KVStore) Naming.lookup("rmi:" + names[i]);
 			long position = a.getPosition(); //If is one of the new servers ask the position 
 			
 			if(position == 0){ // If position = 0 is one of the initial servers
	    			String n = names[i];
	    			byte bytes[]= n.getBytes();
	    			Checksum checksum = new Adler32();
	    			checksum.update(bytes, 0, bytes.length);
	    			long id = checksum.getValue();
	    			idListOrdered[i] = id;
	    			idList[i] = id;
 			}
 			else{ //If position != 0 is one of the new servers and have the exactly position we want in the ring
 				idListOrdered[i] = position;
 				idList[i] = position;
 			}
 		}
 		
 		Arrays.sort(idListOrdered);
 		
 		ranks = new ArrayList<RankServer>(numServers); //To keep the ranks of the servers
 		
 		//Store the correct ranks of each server
 		if(idListOrdered.length > 0) ranks.add(new RankServer(idListOrdered[numServers-1], idListOrdered[0], idListOrdered[0])); //Add the first server
 		
 		for(int i = 0; i<idListOrdered.length-1; i++){ //Add all except the first
 			ranks.add(new RankServer(idListOrdered[i], idListOrdered[i+1],  idListOrdered[i+1]));
 		}
		} catch (RemoteException e) {}
		  catch (MalformedURLException e){}
		  catch (NotBoundException e){}
	 }
	
	public static void main(String[] args) throws IOException {
		BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
		try { 
			serveraddr = args[0];
			updateList();
			
    		int option = 0;
    		
    		while(option < 6){
    			System.out.println("_________________");
				System.out.println("Chose an action:");
				System.out.println("1: Put");
				System.out.println("2: Get");
				System.out.println("3: Get down a Sever");
				System.out.println("4: Get up a Server");
				System.out.println("5: Exit");
				
				try{
					 option = Integer.parseInt(keyboard.readLine());
				}
				catch(NumberFormatException e){
						option = 0;
				}
				while(option<1 || option>6){
					System.out.println("Incorrect Input: Put 1 (Put), 2 (Get), 3 (Get down a Server), 4 (Get up a Server) or 5 (Exit):");
					try{
					 option = Integer.parseInt(keyboard.readLine());
					}catch(NumberFormatException e){}
				}
				
				switch(option){
				
					case 1:{ //Put
						String[] naming = Naming.list("rmi://" + serveraddr + ":1099");
						int num = naming.length;
						if(num > numServers) updateList(); // If we have new servers we need to update the list.
						
						System.out.println("Put the key:");
						String key = keyboard.readLine();
						System.out.println("Put the value:");
						String value = keyboard.readLine();
						
						int correctServer = correctServer(key);
		    			int nextServer = correctServer + 1;
		    			int err = 0;
		    			//Looking for the server id
		    			for(int j = 0; j<idList.length && err < 2; j++){
		    				boolean error = false;
    						if(ranks.get(correctServer).server == idList[j]) {
    							KVStore a = (KVStore) Naming.lookup( "rmi:" + names[j] );
    							System.out.println("Pun in: "+names[j]);
    							try{
	    							String oldValue = a.put(key, value, error);
	    							if(oldValue != null) System.out.println("The old value was: "+oldValue);
	    							else System.out.println("Don't have an old value.");
    							}
    							catch(IncorrectServersException e){
    								System.out.println("We don't have the necessary servers for do the put.");
    							}
    							catch(ServerDownException e){
    								error = true;
    								j=0;
    								correctServer = nextServer; //We need the next of the list
    								err++;
    							}
    							catch(RemoteException e){
    								error = true;
    								j=0;
    								correctServer = nextServer; //We need the next of the list
    								err++;
    							}
    						}
    					}
		    			break;
					}
					
					
					case 2: { //Get
						String[] naming = Naming.list("rmi://" + serveraddr + ":1099");
						int num = naming.length;
						if(num > numServers) updateList(); // If we have new servers we need to update the list.
						
						System.out.println("Put the key:");
						String key = keyboard.readLine();
						
						int correctServer = correctServer(key);
						int nextServer = correctServer + 1;
		    			
		    			int err=0;
		    			//Looking for the server id
		    			for(int j = 0; j<idList.length && err<2; j++){
		    				boolean error = false;
    						if(ranks.get(correctServer).server == idList[j]) {
    							KVStore a = (KVStore) Naming.lookup( "rmi:" + names[j] );
    							System.out.println("Get of: "+names[j]);
    							
    							try{
	    							String v = a.get(key, error);
	    							if(v == null) System.out.println("Incorrect key: Don't have "+key+"."); //If the key doesn't exist return null 
	    							else System.out.println("The value is: "+v);
    							} catch(IncorrectServersException e){
    								System.out.println("We don't have the necessary servers for do the get.");
    							}
    							catch(ServerDownException e){
    								error = true;
    								j=0;
    								correctServer = nextServer; //We need the next of the list
    								err++;
    							}
    							catch(RemoteException e){
    								error = true;
    								j=0;
    								correctServer = nextServer; //We need the next of the list
    								err++;
    							}
    						}
    					}
		    			break;
					}
					case 3:{ //Get down a server
						int op = -1;
						while(op < 0 || op > names.length-1){
							System.out.println("Which server do you want to put down?");
							for(int i=0;i<names.length;i++) System.out.println("Option "+ i +" : "+ names[i]);
							System.out.print("Put the option that you want: "); 
							op = Integer.parseInt(keyboard.readLine());
						}
						KVStore a = (KVStore) Naming.lookup( "rmi:" + names[op]);
						a.getDownServer();
						break;
					}
					case 4:{ // Get up a server
						int op = -1;
						while(op < 0 || op > names.length-1){
							System.out.println("Which server do you want to get up?");
							for(int i=0;i<names.length;i++) System.out.println("Option "+ i +" : "+ names[i]);
							System.out.print("Put the option that you want: "); 
							op = Integer.parseInt(keyboard.readLine());
						}
						KVStore a = (KVStore) Naming.lookup( "rmi:" + names[op]);
						a.getUpServer();
						break;
					}
					
					case 5://Exit program
						 System.out.println("Goodbye =) See you soon!");
						 break;
						
					
				}}
    		
		} catch (Exception e) {	}
	}
	
}
