
public class ThreadReplicate extends Thread{
	Replication neigh;
	String key;
	String value;
	int version;
	
	public ThreadReplicate(Replication neigh, String key, String value, int version){
		this.key = key;
		this.neigh = neigh;
		this.value = value;
		this.version = version;
	}
	
	 public void run(){
		 try{
			 neigh.replicate(key, value, version);
		 }catch(Exception e){}
		 
	 }

}
