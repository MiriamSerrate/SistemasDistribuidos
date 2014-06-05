import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class ThreadPeriodic extends Thread{
	public String serveraddr;
	public boolean[] aliveList;

	public ThreadPeriodic(String serveraddr, boolean[] aliveList) {
		this.aliveList = aliveList;
		this.serveraddr = serveraddr;
	}
	public void run() {
		try {
			String[] names = Naming.list("rmi://" + serveraddr + ":1099");
			
			for(int i = 0; i<names.length; i++){
				Replication replication = (Replication) Naming.lookup("rmi://" + serveraddr + "/" + i);
				try{
					replication.alive();
					if(!aliveList[i]) {
						System.out.println("The server: "+names[i]+" is now up.");
						aliveList[i] = true;
					}
				} catch(ServerDownException e){
					if(aliveList[i]){
						System.out.println("The server: "+names[i]+" is now down.");
						aliveList[i] = false;
					}
				}
			}
		} catch (RemoteException | MalformedURLException e) {}
		catch(NotBoundException e){}
		catch(ArrayIndexOutOfBoundsException e){}

	}
	
}
