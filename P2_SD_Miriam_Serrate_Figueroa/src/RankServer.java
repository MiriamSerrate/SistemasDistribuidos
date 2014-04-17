

public class RankServer {

	long rankA;
	long rankB;
	long server;
	
	public RankServer(long rankA, long rankB, long server){
		this.rankA = rankA;
		this.rankB = rankB;
		this.server = server;
	}

	public long getRankA() {
		return rankA;
	}

	public void setRankA(long rankA) {
		this.rankA = rankA;
	}

	public long getRankB() {
		return rankB;
	}

	public void setRankB(long rankB) {
		this.rankB = rankB;
	}

	public long getServer() {
		return server;
	}

	public void setServer(long server) {
		this.server = server;
	}
	
	
}
