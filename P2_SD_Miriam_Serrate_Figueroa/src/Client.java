

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class Client {

	
	public static void main(String[] args) throws IOException {
		BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
		try { 
			
			String serveraddr = args[0];
			String[] names = Naming.list("rmi://" + serveraddr + ":1099");
			long[] idListOrdered = new long[names.length];
			int N = names.length; //Total nodes
			
    		for (int i = 0; i < N; i++){
    			String n = names[i];
    			byte bytes[]= n.getBytes();
    			Checksum checksum = new Adler32();
    			checksum.update(bytes, 0, bytes.length);
    			long id = checksum.getValue();
    			idListOrdered[i] = id;
    		}
    		
    		long[] idList = idListOrdered;
    		Arrays.sort(idListOrdered);
    		
    		ArrayList<RankServer> ranks = new ArrayList<RankServer>(N); //To keep the ranks of the servers
    		
    		//Store the correct ranks of each server
    		if(idListOrdered.length > 0) ranks.add(new RankServer(idListOrdered[N-1], idListOrdered[0], idListOrdered[0])); //Add the first server
    		
    		for(int i = 0; i<idListOrdered.length-1; i++){ //Add all except the first
    			ranks.add(new RankServer(idListOrdered[i], idListOrdered[i+1],  idListOrdered[i+1]));
    		}
    		
    		
    		int option = 0;
    		
    		while(option != 3){
    			System.out.println("_________________");
				System.out.println("Chose an action:");
				System.out.println("1: Put");
				System.out.println("2: Get");
				System.out.println("3: Exit");
				
				try{
					 option = Integer.parseInt(keyboard.readLine());
				}
				catch(NumberFormatException e){
						option = 0;
				}
				while(option<1 || option>3){
					System.out.println("Incorrect Input: Put 1 (Put), 2 (Get) or 3 (Exit):");
					try{
					 option = Integer.parseInt(keyboard.readLine());
					}catch(NumberFormatException e){}
				}
				
				switch(option){
				
					case 1:{ //Put
						System.out.println("Put the key:");
						String key = keyboard.readLine();
						System.out.println("Put the value:");
						String value = keyboard.readLine();
						
		    			byte bytes[]= key.getBytes();
		    			Checksum checksum = new Adler32();
		    			checksum.update(bytes, 0, bytes.length);
		    			long id = checksum.getValue();
		    			
		    			int correctServer = -1;
		    			
		    			//Looking for the correct server
		    			if((id > ranks.get(0).rankA) || (id <= ranks.get(0).rankB)) correctServer = 0; //Look if its the first
		    					    			
		    			for(int i = 1; i<ranks.size(); i++){ 
		    				if((id > ranks.get(i).rankA) && (id <= ranks.get(i).rankB)){
		    					correctServer = i;
		    					break;
		    				}
		        		}
		    			
		    			//Looking for the server id
		    			for(int j = 0; j<idList.length; j++){
    						if(ranks.get(correctServer).server == idList[j]) {
    							KVStore a = (KVStore) Naming.lookup( "rmi:" + names[j] );
    							try{
	    							String oldValue = a.put(key, value);
	    							if(oldValue != null) System.out.println("The old value was: "+oldValue);
	    							else System.out.println("Don't have an old value.");
    							}
    							catch(IncorrectServersException e){
    								System.out.println("We don't have the necessary servers for do the put.");
    							}
    							break;
    						}
    					}
		    			break;
					}
					
					
					case 2: { //Get
						System.out.println("Put the key:");
						String key = keyboard.readLine();
						
						byte bytes[]= key.getBytes();
		    			Checksum checksum = new Adler32();
		    			checksum.update(bytes, 0, bytes.length);
		    			long id = checksum.getValue();
		    			
		    			int correctServer = -1;
		    			
		    			//Looking for the correct server
		    			if((id > ranks.get(0).rankA) || (id <= ranks.get(0).rankB))	correctServer = 0; //Look if its the first
		    			
		    			for(int i = 1; i<ranks.size(); i++){ 
		    				if((id > ranks.get(i).rankA) && (id <= ranks.get(i).rankB)){
		    					correctServer = i;
		    					break;
		    				}
		        		}
		    			
		    			
		    			//Looking for the server id
		    			for(int j = 0; j<idList.length; j++){
    						if(ranks.get(correctServer).server == idList[j]) {
    							KVStore a = (KVStore) Naming.lookup( "rmi:" + names[j] );
    							try{
	    							String v = a.get(key);
	    							if(v == null) System.out.println("Incorrect key: Don't have "+key+"."); //If the key doesn't exist return null 
	    							else System.out.println("The value is: "+v);
    							} catch(IncorrectServersException e){
    								System.out.println("We don't have the necessary servers for do the get.");
    							}
    							break;
    						}
    					}
		    			break;
					}
					
					
					case 3:
						break;
				}
			}
		} catch (Exception e) {	}
	}
	
}
